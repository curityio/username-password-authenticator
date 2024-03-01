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
import io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.authentication.ActivationResult;
import se.curity.identityserver.sdk.authentication.AnonymousRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.HashMap;

import static java.util.Collections.emptyMap;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public class UsernamePasswordActivateAccountRequestHandler
        implements AnonymousRequestHandler<ActivateAccountRequestModel>
{
    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordActivateAccountRequestHandler.class);

    private final AccountManager _accountManager;
    private final AuthenticatorInformationProvider _authenticatorInformationProvider;

    public UsernamePasswordActivateAccountRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _accountManager = configuration.getAccountManager();
        _authenticatorInformationProvider = configuration.getAuthenticatorInformationProvider();
    }

    @Override
    public ActivateAccountRequestModel preProcess(Request request, Response response)
    {

        if (request.isGetRequest())
        {
            response.setResponseModel(templateResponseModel(emptyMap(),
                    "account-activation/success"), Response.ResponseModelScope.NOT_FAILURE);

            response.setResponseModel(templateResponseModel(emptyMap(),
                   "account-activation/request-new-activation"), HttpStatus.BAD_REQUEST);
        }
        else if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(emptyMap(),
                    "account-activation/resent"), Response.ResponseModelScope.NOT_FAILURE);

            response.setResponseModel(templateResponseModel(emptyMap(),
                    "account-activation/request-new-activation"), HttpStatus.BAD_REQUEST);
        }

        var activateAccountUrl = String.format("%s/activate",
                _authenticatorInformationProvider.getFullyQualifiedAnonymousUri());
        response.putViewData(ViewModelReservedKeys.ACTIVATION_ENDPOINT, activateAccountUrl, Response.ResponseModelScope.ANY);

        return new ActivateAccountRequestModel(request);
    }

    @Override
    public Void get(ActivateAccountRequestModel requestModel, Response response)
    {
        var model = requestModel.getGetRequestModel();

        String tokenAsString = model.getToken();
        ActivationResult activationResult = _accountManager.activateAccount(tokenAsString);

        if (activationResult.isDone())
        {
            response.setResponseModel(activationResult.getModel(), HttpStatus.OK);
        }
        else
        {
            response.setHttpStatus(HttpStatus.BAD_REQUEST);
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.token.invalid"));
        }

        return null;
    }

    @Override
    @Nullable
    public Void post(ActivateAccountRequestModel requestModel, Response response)
    {
        var model = requestModel.getPostRequestModel();

        @Nullable AccountAttributes account = null;

        if (model.getEmail() != null)
        {
            account = _accountManager.getByEmail(model.getEmail());
        }

        if (account != null)
        {
            String activateAccountUrl = String.format("%s/activate-account",
                    _authenticatorInformationProvider.getFullyQualifiedAnonymousUri());
            var data = new HashMap<String, Object>(1);
            data.put(ViewModelReservedKeys.ACTIVATION_ENDPOINT, activateAccountUrl);

            ActivationResult activationResult = _accountManager.initializeActivation(account, data);
            if (activationResult.isDone() || activationResult.isPending())
            {
                response.setResponseModel(activationResult.getModel(), HttpStatus.OK);
            }
        }

        return null;
    }
}
