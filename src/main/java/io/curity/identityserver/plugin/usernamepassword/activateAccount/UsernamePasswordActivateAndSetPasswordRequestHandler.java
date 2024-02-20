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

package io.curity.identityserver.plugin.usernamepassword.activateAccount;

import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.setPassword.UpdatePasswordResult;
import io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.SubjectAttributes;
import se.curity.identityserver.sdk.authentication.ActivationResult;
import se.curity.identityserver.sdk.authentication.AnonymousRequestHandler;
import se.curity.identityserver.sdk.errors.CredentialManagerException;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.service.credential.CredentialUpdateResult;
import se.curity.identityserver.sdk.service.credential.UserCredentialManager;
import se.curity.identityserver.sdk.service.credential.results.SubjectCredentialsNotFound;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.HashMap;

import static java.util.Collections.emptyMap;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public class UsernamePasswordActivateAndSetPasswordRequestHandler
        implements AnonymousRequestHandler<ActivateAndSetPasswordRequestModel>
{
    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordActivateAndSetPasswordRequestHandler.class);

    private static final String USER_TO_SET_PASSWORD_FOR = "USER_TO_SET_PASSWORD_FOR";
    private final AccountManager _accountManager;
    private final SessionManager _sessionManager;
    private final UserCredentialManager _userCredentialManager;
    private final AuthenticatorInformationProvider _authenticatorInformationProvider;

    public UsernamePasswordActivateAndSetPasswordRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _accountManager = configuration.getAccountManager();
        _sessionManager = configuration.getSessionManager();
        _userCredentialManager = configuration.getCredentialManager();
        _authenticatorInformationProvider = configuration.getAuthenticatorInformationProvider();
    }

    @Override
    public ActivateAndSetPasswordRequestModel preProcess(Request request, Response response)
    {

        if (request.isGetRequest())
        {
            response.setResponseModel(templateResponseModel(emptyMap(),
                    "account-activation/set-password"), Response.ResponseModelScope.NOT_FAILURE);

            response.setResponseModel(templateResponseModel(emptyMap(),
                   "account-activation/request-new-activation"), HttpStatus.BAD_REQUEST);
        }
        else if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(emptyMap(),
                    "account-activation/success"), Response.ResponseModelScope.NOT_FAILURE);

            response.setResponseModel(templateResponseModel(emptyMap(),
                    "account-activation/set-password"), HttpStatus.BAD_REQUEST);
        }

        return new ActivateAndSetPasswordRequestModel(request);
    }

    @Override
    public Void get(ActivateAndSetPasswordRequestModel requestModel, Response response)
    {
        var model = requestModel.getGetRequestModel();

        String tokenAsString = model.getToken();
        ActivationResult activationResult = _accountManager.activateAccount(tokenAsString);

        if (activationResult.isDone())
        {
            response.setResponseModel(activationResult.getModel(), HttpStatus.OK);
            _sessionManager.put(Attribute.of(USER_TO_SET_PASSWORD_FOR, activationResult.getUsername()));
        }
        else
        {
            response.setHttpStatus(HttpStatus.BAD_REQUEST);
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.token.invalid"));

            var activateAccountUrl = String.format("%s/activate",
                    _authenticatorInformationProvider.getFullyQualifiedAnonymousUri());
            response.putViewData(ViewModelReservedKeys.ACTIVATION_ENDPOINT, activateAccountUrl, HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    @Override
    @Nullable
    public Void post(ActivateAndSetPasswordRequestModel requestModel, Response response)
    {
        var model = requestModel.getPostRequestModel();
        String password = model.getPassword();

        AccountAttributes account = getUserAccountFromSession();
        if (account != null)
        {
            CredentialUpdateResult result = _userCredentialManager.update(SubjectAttributes.of(account.getUserName()), password);
            if (result instanceof CredentialUpdateResult.Rejected rejected)
            {
                response.addErrorMessage(ErrorMessage.withMessage("validation.error.password.weak"));
                response.addErrorMessage(ErrorMessage.withMessage(CredentialUpdateResult.Rejected.CODE));

                var filteredDetails = rejected.getDetails().stream()
                        .filter(detail -> !(detail instanceof SubjectCredentialsNotFound)).toList();
                response.putViewData("_rejection_details", filteredDetails, Response.ResponseModelScope.FAILURE);
                return null;
            }

            _sessionManager.remove(USER_TO_SET_PASSWORD_FOR);
            return null;
        }

        setNoSessionResponse(model, response);
        return null;
    }

    private AccountAttributes getUserAccountFromSession() {

        AccountAttributes user = null;

        Attribute userAttributes = _sessionManager.get(USER_TO_SET_PASSWORD_FOR);
        if (userAttributes != null)
        {
            Object value = userAttributes.getValue();
            if (value != null)
            {
                user = _accountManager.getByUserName(value.toString());
            }
        }

        return user;
    }

    private void setNoSessionResponse(ActivateAndSetPasswordRequestModel.Post requestModel, Response response)
    {
        var activateAccountUrl = String.format("%s/activate",
                _authenticatorInformationProvider.getFullyQualifiedAnonymousUri());

        var data = new HashMap<String, Object>(1);
        data.put(ViewModelReservedKeys.ACTIVATION_ENDPOINT, activateAccountUrl);

        response.setResponseModel(templateResponseModel(data,
                "account-activation/request-new-activation"), HttpStatus.BAD_REQUEST);

        response.setHttpStatus(HttpStatus.BAD_REQUEST);
        response.addErrorMessage(ErrorMessage.withMessage("validation.error.token.invalid"));
    }
}
