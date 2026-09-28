/*
 *  Copyright 2026 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugin.usernamepassword.utils;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.data.tokens.TokenAttributes;
import se.curity.identityserver.sdk.service.NonceTokenIssuer;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.Throttler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Binds a one-time password (OTP) to a nonce and to the current session.
 * <p>
 * The nonce carries the account ID and the expiration time, while the session holds a hash of the OTP.
 * The nonce is only introspected (and so consumed) once the user has entered the correct OTP, or when too many
 * wrong attempts have been made.
 * <p>
 * Every verification attempt is also checked by a {@link Throttler}, keyed by the account the OTP was issued for,
 * so that OTPs cannot be brute-forced across sessions. Sending OTPs is throttled with a separate purpose.
 */
public final class OtpHelper
{
    public static final String ACCOUNT_ID_ATTRIBUTE = "accountId";

    private static final Logger _logger = LoggerFactory.getLogger(OtpHelper.class);
    private static final String SESSION_KEY = "otpData";
    private static final String THROTTLING_PURPOSE = "forgot-password-otp";
    private static final String SENDING_THROTTLING_PURPOSE = "forgot-password-email";
    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom _random = new SecureRandom();

    private final SessionManager _sessionManager;
    private final NonceTokenIssuer _nonceTokenIssuer;
    private final Throttler _throttler;

    public OtpHelper(SessionManager sessionManager, NonceTokenIssuer nonceTokenIssuer, Throttler throttler)
    {
        _sessionManager = sessionManager;
        _nonceTokenIssuer = nonceTokenIssuer;
        _throttler = throttler;
    }

    public static String generateOtp()
    {
        var otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++)
        {
            otp.append(_random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Check whether sending a new OTP should be throttled.
     * <p>
     * Calling this method updates the throttling state, so it must be called once per OTP to be sent.
     *
     * @param throttlingKey the account ID, or the username or email entered by the user if no account was found
     * @return true if no OTP should be sent
     */
    public boolean isSendingThrottled(String throttlingKey)
    {
        if (_throttler.shouldThrottle(throttlingKey, SENDING_THROTTLING_PURPOSE)
                instanceof Throttler.ThrottlingResult.Throttled)
        {
            _logger.debug("Sending the OTP was throttled");
            return true;
        }
        return false;
    }

    /**
     * Store the OTP in the session, invalidating any previous one.
     *
     * @param nonce     a nonce containing the {@link #ACCOUNT_ID_ATTRIBUTE}
     * @param otp       the OTP sent to the user
     * @param accountId the account the OTP was issued for, used as the throttling key
     */
    public void store(String nonce, String otp, String accountId)
    {
        invalidate();
        write(new Data(nonce, hash(nonce, otp), accountId, 0));
    }

    /**
     * Store an OTP that can never be verified. Used when no account was found, so that the user sees the same
     * behavior as when an account exists.
     *
     * @param throttlingKey the username or email entered by the user, so that attempts are throttled as usual
     */
    public void storeDecoy(String throttlingKey)
    {
        invalidate();
        write(new Data(null, hash(generateOtp(), generateOtp()), throttlingKey, 0));
    }

    public boolean hasPendingOtp()
    {
        return read() != null;
    }

    /**
     * Verify the OTP entered by the user.
     * <p>
     * If the OTP is correct, the nonce is consumed and the OTP is removed from the session.
     * If it is wrong, the user may retry until the maximum number of attempts is reached.
     *
     * @param otp the OTP entered by the user
     * @return the verification result, holding the account ID if the OTP is correct and the nonce has not expired
     */
    public VerificationResult verify(String otp)
    {
        @Nullable Data data = read();
        if (data == null)
        {
            _logger.debug("No OTP was found in the session");
            return new VerificationResult.Invalid();
        }

        if (_throttler.shouldThrottle(data.throttlingKey, THROTTLING_PURPOSE)
                instanceof Throttler.ThrottlingResult.Throttled)
        {
            _logger.debug("OTP verification was throttled");
            return new VerificationResult.Throttled();
        }

        if (data.nonce == null || !MessageDigest.isEqual(
                data.otpHash.getBytes(StandardCharsets.US_ASCII),
                hash(data.nonce, otp.trim()).getBytes(StandardCharsets.US_ASCII)))
        {
            int attempts = data.attempts + 1;
            if (attempts >= MAX_ATTEMPTS)
            {
                _logger.debug("Maximum number of OTP attempts reached");
                invalidate();
            }
            else
            {
                write(new Data(data.nonce, data.otpHash, data.throttlingKey, attempts));
            }
            return new VerificationResult.Invalid();
        }

        _sessionManager.remove(SESSION_KEY);
        _throttler.clear(data.throttlingKey, THROTTLING_PURPOSE);

        @Nullable String accountId = _nonceTokenIssuer.introspect(data.nonce)
                .map(this::accountIdFrom)
                .orElse(null);

        return accountId == null ? new VerificationResult.Invalid() : new VerificationResult.Verified(accountId);
    }

    /**
     * Remove the OTP from the session and consume its nonce so that it cannot be used anymore.
     */
    public void invalidate()
    {
        @Nullable Data data = read();
        _sessionManager.remove(SESSION_KEY);
        if (data != null && data.nonce != null)
        {
            _nonceTokenIssuer.introspect(data.nonce);
        }
    }

    @Nullable
    private String accountIdFrom(TokenAttributes attributes)
    {
        @Nullable Attribute accountId = attributes.get(ACCOUNT_ID_ATTRIBUTE);
        return accountId == null ? null : accountId.getValueOfType(String.class);
    }

    private String hash(String nonce, String otp)
    {
        var value = String.join(":", nonce, otp, _sessionManager.getSessionId());
        try
        {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Nullable
    private Data read()
    {
        @Nullable Attribute attribute = _sessionManager.get(SESSION_KEY);
        return attribute == null ? null : new Gson().fromJson(attribute.getValue().toString(), Data.class);
    }

    private void write(Data data)
    {
        _sessionManager.put(Attribute.of(SESSION_KEY, new Gson().toJson(data)));
    }

    private record Data(@Nullable String nonce, String otpHash, String throttlingKey, int attempts)
    {
    }

    public sealed interface VerificationResult
    {
        record Verified(String accountId) implements VerificationResult
        {
        }

        record Invalid() implements VerificationResult
        {
        }

        record Throttled() implements VerificationResult
        {
        }
    }
}
