/*
 *  Copyright 2022 Curity AB
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

package io.curity.identityserver.plugin.usernamepassword.forgotPassword;

import com.google.common.html.HtmlEscapers;
import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.descriptor.UsernamePasswordAuthenticatorPluginDescriptor;
import io.curity.identityserver.plugin.usernamepassword.setPassword.SetPasswordSessionData;
import io.curity.identityserver.plugin.usernamepassword.utils.NullEmailSender;
import io.curity.identityserver.plugin.usernamepassword.utils.OtpHelper;
import io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.attribute.Attributes;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.data.email.Email;
import se.curity.identityserver.sdk.data.tokens.TokenAttributes;
import se.curity.identityserver.sdk.data.tokens.TokenIssuerException;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.EmailSender;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.NonceTokenIssuer;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

/**
 * Username/Password Authenticator forgot password handler.
 */
public final class UsernamePasswordForgotPasswordRequestHandler implements AuthenticatorRequestHandler<RequestModel>
{
    /**
     * Create a new instance of UsernamePasswordForgotPasswordRequestHandler using the configuration for this plugin.
     * <p>
     * The server knows how to provide an instance of this configuration because it is declared in the
     * plugin descriptor at {@link UsernamePasswordAuthenticatorPluginDescriptor#getConfigurationType()}.
     *
     * @param configuration for the Username/Password authenticator plugin
     */

    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordForgotPasswordRequestHandler.class);

    private final UserPreferenceManager _userPreferenceManager;
    private final AccountManager _accountManager;
    private final EmailSender _emailSender;
    private final NonceTokenIssuer _nonceTokenIssuer;
    private final AuthenticatorInformationProvider _authenticatorInformationProvider;
    private final ExceptionFactory _exceptionFactory;
    private final SessionManager _sessionManager;
    private final OtpHelper _otpHelper;
    private final Duration _otpTimeToLive;

    public UsernamePasswordForgotPasswordRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _accountManager = configuration.getAccountManager();
        _userPreferenceManager = configuration.getUserPreferenceManager();
        _nonceTokenIssuer = configuration.getNonceTokenIssuer();
        _authenticatorInformationProvider = configuration.getAuthenticatorInformationProvider();
        _exceptionFactory = configuration.getExceptionFactory();
        _sessionManager = configuration.getSessionManager();
        _otpHelper = new OtpHelper(_sessionManager, _nonceTokenIssuer, configuration.getThrottler());
        _otpTimeToLive = Duration.ofSeconds(configuration.getOtpTimeToLive());

        if (configuration.getEmailSender().isPresent())
        {
            _emailSender = configuration.getEmailSender().get();
        }
        else
        {
            _logger.info("No email provider has been configured");
            _emailSender = new NullEmailSender();
        }
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        var data = new HashMap<String, Object>(1);
        data.put(ViewModelReservedKeys.SHOW_EMAIL_FIELD, !_accountManager.useUsernameAsEmail());

        if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(data, "forgot-password/post"),
                    Response.ResponseModelScope.NOT_FAILURE);

            var postModel = new RequestModel.PostRequestModel(request);
            data.put(ViewModelReservedKeys.USERNAME, postModel.getUsername());

            response.setResponseModel(templateResponseModel(data, "forgot-password/get"),
                    HttpStatus.BAD_REQUEST);
        }
        else if (request.isGetRequest())
        {
            var getModel = new RequestModel.GetRequestModel(_userPreferenceManager);
            data.put(ViewModelReservedKeys.USERNAME, getModel.getUsername());

            response.setResponseModel(templateResponseModel(data, "forgot-password/get"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }

        return new RequestModel(request, _userPreferenceManager);
    }

    @Override
    public Optional<AuthenticationResult> get(RequestModel requestModel, Response response)
    {
        return Optional.empty();
    }

    @Override
    public Optional<AuthenticationResult> post(RequestModel requestModel, Response response)
    {
        var postModel = requestModel.getPostRequestModel();

        @Nullable String username = postModel.getUsername();
        @Nullable String emailAddress = postModel.getPrimaryEmail();
        @Nullable AccountAttributes account = null;

        if (StringUtils.isNotBlank(emailAddress))
        {
            account = _accountManager.getByEmail(emailAddress);
        }

        if (account == null && StringUtils.isNotBlank(username))
        {
            account = _accountManager.getByUserName(username);
        }

        @Nullable String emailValue = AccountAttributes.emailFrom(account);
        boolean accountFound = account != null && emailValue != null;

        // throttle on the account when it exists, otherwise on the entered value, so both cases behave the same
        @Nullable String throttlingKey = accountFound ? account.getUserName()
                : StringUtils.isNotBlank(username) ? username : emailAddress;

        if (StringUtils.isNotBlank(throttlingKey) && _otpHelper.isSendingThrottled(throttlingKey))
        {
            // keep any OTP already sent valid, and do not send a new one
            response.addErrorMessage(ErrorMessage.withMessage("error.throttled"));
            return Optional.empty();
        }

        // a new request replaces any password reset already in progress in this session
        new SetPasswordSessionData(_sessionManager).remove();

        var setPasswordUrl = String.format("%s/set-password",
                _authenticatorInformationProvider.getFullyQualifiedAnonymousUri());
        response.putViewData(ViewModelReservedKeys.SET_PASSWORD_ENDPOINT, setPasswordUrl,
                Response.ResponseModelScope.NOT_FAILURE);

        if (accountFound)
        {
            onAccountFound(response, emailValue, account, setPasswordUrl);
        }
        else
        {
            onAccountNotFound(response, emailAddress, username, emailValue);
        }

        return Optional.empty();
    }

    private void onAccountFound(Response response, String emailValue, AccountAttributes account,
                                String setPasswordUrl)
    {
        String otp = OtpHelper.generateOtp();
        _otpHelper.store(issueNonce(account), otp, account.getUserName());

        var emailModel = new HashMap<String, Object>(2);
        emailModel.put("otp", otp);
        emailModel.put(ViewModelReservedKeys.SET_PASSWORD_ENDPOINT, setPasswordUrl);

        var emailToSend = new Email(emailModel);
        _emailSender.sendEmail(emailValue, emailToSend, "email/forgot-password/email");

        response.putViewData(ViewModelReservedKeys.RECIPIENT_OF_COMMUNICATION, emailValue, Response.ResponseModelScope.NOT_FAILURE);
    }

    private void onAccountNotFound(Response response,
                                   @Nullable String emailAddress,
                                   @Nullable String username,
                                   @Nullable String emailValue)
    {
        if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(emailAddress))
        {
            _logger.debug("The username or email address provided by the user is not associated with a valid account;" +
                            " will return a successful response anyway to protect against spear phishing attacks, " +
                            "username = {}, given email = {}, found email = {}",
                            username, emailAddress, emailValue);

            // pretend the email was sent out successfully to protect against spear phishing attacks
            var recipient = StringUtils.isNotBlank(username) ? username : emailAddress;
            _otpHelper.storeDecoy(recipient);
            response.putViewData(ViewModelReservedKeys.RECIPIENT_OF_COMMUNICATION, HtmlEscapers.htmlEscaper().escape(recipient), Response.ResponseModelScope.NOT_FAILURE);
        }
        else
        {
            // this will only happen if the request validator(s) allow everything to be empty!
            // If somehow we get here, show the expected error but log a warning as it is not expected to happen
            _logger.error("No username or email address were provided. The RequestModel was not validated correctly.");
        }
    }

    private String issueNonce(AccountAttributes account) {

        try
        {
            var tokenValue = new HashMap<String, Object>(1);
            tokenValue.put(OtpHelper.ACCOUNT_ID_ATTRIBUTE, account.getUserName());

            var now = Instant.now();
            var expires = now.plus(_otpTimeToLive);
            var tokenAttributes = new TokenAttributes(expires, now, Attributes.fromMap(tokenValue));
            return _nonceTokenIssuer.issue(tokenAttributes);
        }
        catch (TokenIssuerException ignored)
        {
            _logger.warn("Exception when trying to issue nonce token, are the datasources available?");
            throw _exceptionFactory.internalServerException(ErrorCode.TOKEN_ISSUANCE_ERROR,
                    "Failed to issue nonce token");
        }
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errorMessages)
    {
        if (request.isPostRequest())
        {
            var requestModel = new RequestModel.PostRequestModel(request);
            var data = new HashMap<String, Object>(1);
            data.put(ViewModelReservedKeys.FORM_POST_BACK, requestModel.dataOnError());

            // on POST validation failure, go back to the GET template
            response.setResponseModel(templateResponseModel(data,
                    "forgot-password/get"), HttpStatus.BAD_REQUEST);
        }
    }
}
