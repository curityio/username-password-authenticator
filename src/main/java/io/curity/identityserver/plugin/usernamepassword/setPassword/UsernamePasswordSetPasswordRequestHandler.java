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

package io.curity.identityserver.plugin.usernamepassword.setPassword;

import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.utils.CredentialOperations;
import io.curity.identityserver.plugin.usernamepassword.utils.OtpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.attribute.SubjectAttributes;
import se.curity.identityserver.sdk.authentication.AnonymousRequestHandler;
import se.curity.identityserver.sdk.errors.ExternalServiceException;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.credential.CredentialUpdateResult;
import se.curity.identityserver.sdk.service.credential.UserCredentialManager;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;


import static java.util.Collections.emptyMap;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public final class UsernamePasswordSetPasswordRequestHandler implements AnonymousRequestHandler<RequestModel>
{
    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordSetPasswordRequestHandler.class);

    private final SessionManager _sessionManager;
    private final OtpHelper _otpHelper;
    private final AccountManager _accountManager;
    private final UserCredentialManager _userCredentialManager;

    public UsernamePasswordSetPasswordRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _sessionManager = configuration.getSessionManager();
        _otpHelper = new OtpHelper(_sessionManager, configuration.getNonceTokenIssuer(),
                configuration.getThrottler());
        _accountManager = configuration.getAccountManager();
        _userCredentialManager = configuration.getCredentialManager();
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        if (request.isGetRequest())
        {
            response.setResponseModel(templateResponseModel(emptyMap(),
                            "set-password/get"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }
        else if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(emptyMap(),
                            "set-password/post"),
                    Response.ResponseModelScope.NOT_FAILURE);

            response.setResponseModel(templateResponseModel(emptyMap(),
                            "set-password/get"),
                    HttpStatus.BAD_REQUEST);
        }

        putStepViewData(response);

        return new RequestModel(request);
    }

    @Override
    public Void get(RequestModel requestModel, Response response)
    {
        return null;
    }

    @Override
    public Void post(RequestModel requestModel, Response response)
    {
        var model = requestModel.getPostRequestModel();

        if (model.isOtpSubmission())
        {
            verifyOtp(model.getOtp(), response);
            return null;
        }

        UpdatePasswordResult result;
        try
        {
            result = updatePassword(model.getPassword());
        }
        catch (ExternalServiceException e)
        {
            response.addErrorMessage(ErrorMessage.withMessage("system.status.internal.error"));
            return null;
        }

        if (result instanceof UpdatePasswordResult.UpdateRejected rejected)
        {
            response.addErrorMessage(ErrorMessage.withMessage(CredentialUpdateResult.Rejected.CODE));
            CredentialOperations.onCredentialUpdateRejected(response, rejected.getRejected().getDetails());
        }
        else if (result instanceof UpdatePasswordResult.InvalidAccount)
        {
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.invalid.account"));
        }
        else if (result instanceof UpdatePasswordResult.InvalidToken)
        {
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.token.invalid"));
        }

        return null;
    }

    private void verifyOtp(String otp, Response response)
    {
        OtpHelper.VerificationResult result = _otpHelper.verify(otp);
        if (result instanceof OtpHelper.VerificationResult.Verified verified)
        {
            _logger.trace("OTP was accepted and the account ID saved to the session");
            new SetPasswordSessionData(_sessionManager).write(verified.accountId());

            // the OTP was accepted, so show the form to enter the new password
            response.setResponseModel(templateResponseModel(emptyMap(), "set-password/get"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }
        else if (result instanceof OtpHelper.VerificationResult.Throttled)
        {
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.otp.throttled"));
        }
        else
        {
            _logger.debug("OTP was not accepted");
            response.addErrorMessage(ErrorMessage.invalidParameter(RequestModel.Post.OTP_PARAM, "validation.error.otp.invalid"));
        }

        putStepViewData(response);
    }

    /**
     * Tell the view whether the OTP or the new password must be entered, or whether the flow cannot continue.
     */
    private void putStepViewData(Response response)
    {
        boolean otpVerified = new SetPasswordSessionData(_sessionManager).readAccountId() != null;
        boolean otpRequired = !otpVerified && _otpHelper.hasPendingOtp();

        response.putViewData(RequestModel.OTP_REQUIRED, otpRequired, Response.ResponseModelScope.ANY);
        response.putViewData(RequestModel.NONCE_IS_INVALID, !otpVerified && !otpRequired,
                Response.ResponseModelScope.ANY);
    }

    private UpdatePasswordResult updatePassword(String password)
    {
        var sessionData = new SetPasswordSessionData(_sessionManager);
        String accountId = sessionData.readAccountId();
        if (accountId == null)
        {
            _logger.trace("No verified OTP was found in the session");
            return new UpdatePasswordResult.InvalidToken();
        }

        @Nullable AccountAttributes account = _accountManager.getByUserName(accountId);
        if (account == null)
        {
            return new UpdatePasswordResult.InvalidAccount();
        }

        account = account.withPassword(password);
        CredentialUpdateResult result = _userCredentialManager.update(SubjectAttributes.of(account.getUserName()), password);
        if (result instanceof CredentialUpdateResult.Rejected rejected)
        {
            return new UpdatePasswordResult.UpdateRejected(rejected);
        }

        sessionData.remove();
        return new UpdatePasswordResult.Success();
    }
}
