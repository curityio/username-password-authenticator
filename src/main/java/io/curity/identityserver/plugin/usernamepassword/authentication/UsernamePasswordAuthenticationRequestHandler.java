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
import se.curity.identityserver.sdk.attribute.SubjectAttributes;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.credential.CredentialVerificationResult;
import se.curity.identityserver.sdk.service.credential.UserCredentialManager;
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
    private final UserCredentialManager _userCredentialManager;
    private final UserPreferenceManager _userPreferenceManager;

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
        _userCredentialManager = configuration.getCredentialManager();
        _userPreferenceManager = configuration.getUserPreferenceManager();
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        var data = new HashMap<String, Object>(2);
        data.put(ViewModelReservedKeys.USERNAME, _userPreferenceManager.getUsername());

        boolean isRegistrationEnabled = (_accountManager != null && _accountManager.supportsRegistration());
        data.put(ViewModelReservedKeys.REGISTRATION_ENABLED, isRegistrationEnabled);

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
        var subjectAttributes = SubjectAttributes.of(model.getUserName());

        @Nullable
        var credentialVerificationResult = _userCredentialManager.verify(subjectAttributes, model.getPassword());
        if (credentialVerificationResult instanceof CredentialVerificationResult.Accepted)
        {
            var attributes = ((CredentialVerificationResult.Accepted) credentialVerificationResult).getAuthenticationAttributes();
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
