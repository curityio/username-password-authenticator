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
import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.Attributes;
import se.curity.identityserver.sdk.data.tokens.TokenAttributes;
import se.curity.identityserver.sdk.data.tokens.TokenIssuerException;
import se.curity.identityserver.sdk.service.NonceTokenIssuer;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.Throttler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Binds a one-time password (OTP) to a nonce and to the current session.
 * <p>
 * The nonce carries the account ID, while the session holds a salted hash of the OTP and its expiration time.
 * Every verification attempt consumes the nonce before the OTP is checked, so that concurrent attempts cannot
 * check more than one OTP per nonce. After a wrong OTP, a new nonce with the same expiration time replaces it,
 * until the maximum number of attempts is reached.
 * <p>
 * Every verification attempt is also checked by a {@link Throttler}, keyed by the account the OTP was issued for,
 * so that OTPs cannot be brute-forced across sessions. Sending OTPs is throttled with a separate purpose.
 */
public final class OtpHelper
{
    private static final Logger _logger = LoggerFactory.getLogger(OtpHelper.class);
    private static final String ACCOUNT_ID_ATTRIBUTE = "accountId";
    private static final String SESSION_KEY = "otpData";
    private static final String THROTTLING_PURPOSE = "forgot-password-otp";
    private static final String SENDING_THROTTLING_PURPOSE = "forgot-password-email";
    private static final int OTP_LENGTH = 6;
    private static final int SALT_LENGTH = 16;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom _random = new SecureRandom();

    private final SessionManager _sessionManager;
    private final NonceTokenIssuer _nonceTokenIssuer;
    private final Throttler _throttler;
    private final Duration _timeToLive;
    private final String _authenticatorId;

    public OtpHelper(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _sessionManager = configuration.getSessionManager();
        _nonceTokenIssuer = configuration.getNonceTokenIssuer();
        _throttler = configuration.getThrottler();
        _timeToLive = Duration.ofSeconds(configuration.getOtpTimeToLive());
        _authenticatorId = configuration.id();
    }

    /**
     * Check whether sending a new OTP should be throttled.
     * <p>
     * Calling this method updates the throttling state, so it must be called once per OTP to be sent.
     *
     * @param identifier the account ID, or the username or email entered by the user if no account was found
     * @return true if no OTP should be sent
     */
    public boolean isSendingThrottled(String identifier)
    {
        if (_throttler.shouldThrottle(throttlingKey(identifier), SENDING_THROTTLING_PURPOSE)
                instanceof Throttler.ThrottlingResult.Throttled)
        {
            _logger.debug("Sending the OTP was throttled");
            return true;
        }
        return false;
    }

    /**
     * Issue a new OTP for the account and store it in the session, invalidating any previous one.
     *
     * @param accountId the account the OTP is issued for
     * @return the OTP to send to the user
     * @throws TokenIssuerException if the nonce could not be issued
     */
    public String issue(String accountId) throws TokenIssuerException
    {
        invalidate();

        String otp = generateOtp();
        String salt = generateSalt();
        Instant expiresAt = Instant.now().plus(_timeToLive);
        String nonce = issueNonce(accountId, expiresAt);

        write(new Data(nonce, salt, hash(salt, otp), throttlingKey(accountId), 0, expiresAt.getEpochSecond()));
        return otp;
    }

    /**
     * Store an OTP that can never be verified. Used when no account was found, so that the user sees the same
     * behavior as when an account exists.
     *
     * @param identifier the username or email entered by the user, so that attempts are throttled as usual
     */
    public void storeDecoy(String identifier)
    {
        invalidate();

        String salt = generateSalt();
        Instant expiresAt = Instant.now().plus(_timeToLive);
        write(new Data(null, salt, hash(salt, generateOtp()), throttlingKey(identifier), 0,
                expiresAt.getEpochSecond()));
    }

    public boolean hasPendingOtp()
    {
        @Nullable Data data = read();
        return data != null && !data.isExpired();
    }

    /**
     * Verify the OTP entered by the user.
     * <p>
     * If the OTP is correct, the OTP is removed from the session.
     * If it is wrong, the user may retry until the maximum number of attempts is reached.
     *
     * @param otp the OTP entered by the user
     * @return the verification result, holding the account ID if the OTP is correct and has not expired
     */
    public VerificationResult verify(String otp)
    {
        @Nullable Data data = read();
        if (data == null)
        {
            _logger.debug("No OTP was found in the session");
            return new VerificationResult.Invalid();
        }

        if (data.isExpired())
        {
            _logger.debug("The OTP has expired");
            invalidate();
            return new VerificationResult.Invalid();
        }

        if (_throttler.shouldThrottle(data.throttlingKey, THROTTLING_PURPOSE)
                instanceof Throttler.ThrottlingResult.Throttled)
        {
            _logger.debug("OTP verification was throttled");
            return new VerificationResult.Throttled();
        }

        if (data.nonce == null)
        {
            // a decoy OTP never verifies
            recordFailedAttempt(data, null);
            return new VerificationResult.Invalid();
        }

        // consume the nonce before checking the OTP, so that only one concurrent attempt can check it
        Optional<String> accountId = _nonceTokenIssuer.introspect(data.nonce).map(this::accountIdFrom);
        if (accountId.isEmpty())
        {
            // another attempt consumed the nonce concurrently; leave the session as that attempt leaves it
            _logger.debug("The OTP nonce was already consumed");
            return new VerificationResult.Invalid();
        }

        if (!MessageDigest.isEqual(
                data.otpHash.getBytes(StandardCharsets.US_ASCII),
                hash(data.salt, otp.trim()).getBytes(StandardCharsets.US_ASCII)))
        {
            recordFailedAttempt(data, accountId.get());
            return new VerificationResult.Invalid();
        }

        _sessionManager.remove(SESSION_KEY);
        _throttler.clear(data.throttlingKey, THROTTLING_PURPOSE);

        return new VerificationResult.Verified(accountId.get());
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

    /**
     * Count a wrong OTP. The nonce was already consumed, so a new one is issued unless no attempts are left.
     */
    private void recordFailedAttempt(Data data, @Nullable String accountId)
    {
        int attempts = data.attempts + 1;
        if (attempts >= MAX_ATTEMPTS)
        {
            _logger.debug("Maximum number of OTP attempts reached");
            _sessionManager.remove(SESSION_KEY);
            return;
        }

        @Nullable String nonce = null;
        if (accountId != null)
        {
            try
            {
                nonce = issueNonce(accountId, Instant.ofEpochSecond(data.expiresAt));
            }
            catch (TokenIssuerException e)
            {
                _logger.warn("Could not issue a new nonce after a wrong OTP; the user must request a new OTP");
                _sessionManager.remove(SESSION_KEY);
                return;
            }
        }

        write(new Data(nonce, data.salt, data.otpHash, data.throttlingKey, attempts, data.expiresAt));
    }

    private String issueNonce(String accountId, Instant expiresAt) throws TokenIssuerException
    {
        var attributes = Attributes.fromMap(Map.of(ACCOUNT_ID_ATTRIBUTE, accountId));
        return _nonceTokenIssuer.issue(new TokenAttributes(expiresAt, Instant.now(), attributes));
    }

    @Nullable
    private String accountIdFrom(TokenAttributes attributes)
    {
        @Nullable Attribute accountId = attributes.get(ACCOUNT_ID_ATTRIBUTE);
        return accountId == null ? null : accountId.getValueOfType(String.class);
    }

    /**
     * Throttle per authenticator, and regardless of case or surrounding spaces, so that variants of the same
     * identifier share the same state.
     */
    private String throttlingKey(String identifier)
    {
        return _authenticatorId + ":" + identifier.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateOtp()
    {
        var otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++)
        {
            otp.append(_random.nextInt(10));
        }
        return otp.toString();
    }

    private static String generateSalt()
    {
        var salt = new byte[SALT_LENGTH];
        _random.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    private String hash(String salt, String otp)
    {
        var value = String.join(":", salt, otp, _sessionManager.getSessionId());
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

    private record Data(@Nullable String nonce, String salt, String otpHash, String throttlingKey, int attempts,
                        long expiresAt)
    {
        boolean isExpired()
        {
            return Instant.now().getEpochSecond() >= expiresAt;
        }
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
