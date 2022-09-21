/*
 *  Copyright 2017 Curity AB
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

package io.curity.identityserver.plugin.usernamepassword.authentication;

import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.descriptor.UsernamePasswordAuthenticatorPluginDescriptor;
import io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.CredentialManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static se.curity.identityserver.sdk.web.Response.ResponseModelScope.NOT_FAILURE;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

/**
 * Username/Password Authenticator request handler.
 */
public final class UsernamePasswordAuthenticationRequestHandler implements AuthenticatorRequestHandler<RequestModel>
{
    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordAuthenticationRequestHandler.class);

    private final AccountManager _accountManager;
    private final CredentialManager _credentialManager;
    private final UserPreferenceManager _userPreferenceManager;
    private final AuthenticatorInformationProvider _authenticatorInformationProvider;

    /**
     * Create a new instance of UsernamePasswordAuthenticatorRequestHandler using the configuration for this plugin.
     * <p>
     * The server knows how to provide an instance of this configuration because it is declared in the
     * plugin descriptor at {@link UsernamePasswordAuthenticatorPluginDescriptor#getConfigurationType()}.
     *
     * @param configuration for the Username/Password authenticator plugin
     */

    public UsernamePasswordAuthenticationRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _accountManager = configuration.getAccountManager();
        _credentialManager = configuration.getCredentialManager();
        _userPreferenceManager = configuration.getUserPreferenceManager();
        _authenticatorInformationProvider = configuration.getAuthenticatorInformationProvider();
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        var data = new HashMap<String, Object>(2);

        boolean isRegistrationDisabled = (_accountManager == null || !_accountManager.supportsRegistration());
        data.put(ViewModelReservedKeys.USERNAME, _userPreferenceManager.getUsername());
        if (isRegistrationDisabled)
        {
            data.put(ViewModelReservedKeys.REGISTER_ENDPOINT, null);
        }
        else
        {
            data.put(ViewModelReservedKeys.REGISTER_ENDPOINT, _authenticatorInformationProvider.getFullyQualifiedRegistrationUri());
        }

        // set the template and model for responses on the NOT_FAILURE scope
        response.setResponseModel(templateResponseModel(
                data,
                "authenticate/get"), NOT_FAILURE);

        // on request validation failure, we should use the same template as for NOT_FAILURE
        response.setResponseModel(templateResponseModel(data,
                "authenticate/get"), HttpStatus.BAD_REQUEST);

        return new RequestModel(request);
    }

    @Override
    public Optional<AuthenticationResult> get(RequestModel requestModel, Response response)
    {
        return Optional.empty();
    }

    @Override
    public Optional<AuthenticationResult> post(RequestModel requestModel, Response response)
    {
        var model = requestModel.getPostRequestModel();

        Optional<AuthenticationResult> result = Optional.empty();

        @Nullable
        AuthenticationAttributes attributes = _credentialManager.verifyPassword(
                model.getUserName(),
                model.getPassword(),
                CredentialManager.NO_CONTEXT);

        if (attributes != null)
        {
            // authentication was successful, set the result so that the server can see
            // the user account of the logged in user!
            result = Optional.of(new AuthenticationResult(attributes));
            _userPreferenceManager.saveUsername(model.getUserName());
        }
        else
        {
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.incorrect.credentials"));
            response.putViewData(ViewModelReservedKeys.FORM_POST_BACK, model.dataOnError(),
                    Response.ResponseModelScope.FAILURE);
        }

        return result;
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errorMessages)
    {
        if (request.isPostRequest())
        {
            var model = new RequestModel.Post(request);
            response.putViewData(ViewModelReservedKeys.FORM_POST_BACK, model.dataOnError(),
                    Response.ResponseModelScope.FAILURE);
        }
    }
}
