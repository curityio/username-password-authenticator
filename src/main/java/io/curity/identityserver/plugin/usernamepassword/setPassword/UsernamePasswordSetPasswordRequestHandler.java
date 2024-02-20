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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.SubjectAttributes;
import se.curity.identityserver.sdk.authentication.AnonymousRequestHandler;
import se.curity.identityserver.sdk.data.tokens.TokenAttributes;
import se.curity.identityserver.sdk.errors.ExternalServiceException;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.NonceTokenIssuer;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.credential.CredentialUpdateResult;
import se.curity.identityserver.sdk.service.credential.UserCredentialManager;
import se.curity.identityserver.sdk.service.credential.results.SubjectCredentialsNotFound;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.Optional;

import static java.util.Collections.emptyMap;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public final class UsernamePasswordSetPasswordRequestHandler implements AnonymousRequestHandler<RequestModel>
{
    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordSetPasswordRequestHandler.class);

    private final NonceTokenIssuer _nonceTokenIssuer;
    private final SessionManager _sessionManager;
    private final AccountManager _accountManager;
    private final UserCredentialManager _userCredentialManager;

    public UsernamePasswordSetPasswordRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _nonceTokenIssuer = configuration.getNonceTokenIssuer();
        _sessionManager = configuration.getSessionManager();
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

        return new RequestModel(request, response);
    }

    @Override
    public Void get(RequestModel requestModel, Response response)
    {
        var model = requestModel.getGetRequestModel();

        String token = model.getToken();
        if (validateToken(token))
        {
            response.putViewData(RequestModel.NONCE_IS_INVALID, false, Response.ResponseModelScope.ANY);
        }

        return null;
    }

    @Override
    public Void post(RequestModel requestModel, Response response)
    {
        var model = requestModel.getPostRequestModel();

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
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.password.weak"));
            response.addErrorMessage(ErrorMessage.withMessage(CredentialUpdateResult.Rejected.CODE));

            var filteredDetails = rejected.getRejected().getDetails().stream()
                    .filter(detail -> !(detail instanceof SubjectCredentialsNotFound)).toList();
            response.putViewData("_rejection_details", filteredDetails, Response.ResponseModelScope.FAILURE);
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

    private boolean validateToken(String token) {

        // This prevents an error if the page is refreshed after the nonce has been introspected
        var sessionData = new SetPasswordSessionData(_sessionManager);
        if (sessionData.hasToken(token))
        {
            _logger.trace("Nonce was found in the session");
            return true;
        }

        Optional<TokenAttributes> introspectionResult = _nonceTokenIssuer.introspect(token);
        if (introspectionResult.isPresent())
        {
            _logger.trace("Nonce was successfully introspected");
            TokenAttributes attributes = introspectionResult.get();
            Attribute accountId = attributes.get("accountId");
            String accountIdValue = accountId.getValueOfType(String.class);

            if (StringUtils.isNotBlank(accountIdValue)) {

                // Save to the session so that there is no error if the page is refreshed
                sessionData.write(token, accountIdValue);
                _logger.trace("Nonce was accepted and saved to the session");
                return true;

            }
            else
            {
                _logger.info("Nonce exists but has no accountId claim. " +
                        "The nonce cannot be used to reset the user password.");
            }
        }
        else
        {
            _logger.debug("Nonce was not found");
        }

        return false;
    }

    private UpdatePasswordResult updatePassword(String password)
    {
        var sessionData = new SetPasswordSessionData(_sessionManager);
        String accountId = sessionData.readAccountId();
        if (accountId == null)
        {
            _logger.trace("No valid nonce was found in the session");
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
