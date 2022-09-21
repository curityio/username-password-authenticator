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
import se.curity.identityserver.sdk.authentication.AnonymousRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;

import java.util.HashMap;
import java.util.Map;

import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public class UsernamePasswordActivateAccountLandingRequestHandler implements AnonymousRequestHandler<ActivateAccountRequestModel>
{
    private final AccountManager _accountManager;
    private final AuthenticatorInformationProvider _authenticatorInformationProvider;
    private final ExceptionFactory _exceptionFactory;
    private final Logger _logger;

    public UsernamePasswordActivateAccountLandingRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _accountManager = configuration.getAccountManager();
        _authenticatorInformationProvider = configuration.getAuthenticatorInformationProvider();
        _exceptionFactory = configuration.getExceptionFactory();
        _logger = LoggerFactory.getLogger(UsernamePasswordActivateAccountLandingRequestHandler.class);
    }

    @Override
    public ActivateAccountRequestModel preProcess(Request request, Response response)
    {
        if (request.isGetRequest())
        {
            return new ActivateAccountRequestModel(request);
        }

        throw _exceptionFactory.methodNotAllowed();
    }

    @Override
    public Void get(ActivateAccountRequestModel requestModel, Response response)
    {
        ActivateAccountRequestModel.Get model = requestModel.getGetRequestModel();

        String destinationUrl = String.format("%s/%s",
                _authenticatorInformationProvider.getFullyQualifiedAnonymousUri(),
                _accountManager.isSetPasswordAfterActivation() ? "activate-and-set" : "activate");

        Map<String, Object> data = new HashMap<>(2);
        data.put(ViewModelReservedKeys.ACTION, destinationUrl);
        data.put("formParameters", model.asMap());

        response.setResponseModel(templateResponseModel(data,
                "account-activation/landing"), HttpStatus.OK);

        return null;
    }

    @Override
    public Void post(ActivateAccountRequestModel requestModel, Response response)
    {
        assert false;
        return null;
    }
}