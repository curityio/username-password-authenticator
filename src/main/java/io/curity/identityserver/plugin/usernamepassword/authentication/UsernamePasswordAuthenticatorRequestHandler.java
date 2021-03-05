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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AttributeName;
import se.curity.identityserver.sdk.attribute.AttributeValue;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.CredentialManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.web.Produces;
import se.curity.identityserver.sdk.web.Produces.ContentType;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;


import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static se.curity.identityserver.sdk.web.Response.ResponseModelScope.NOT_FAILURE;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

/**
 * Username/Password Authenticator request handler.
 */
@Produces(ContentType.HTML)
public final class UsernamePasswordAuthenticatorRequestHandler implements AuthenticatorRequestHandler<RequestModel>
{

    private final CredentialManager _credentialManager;
    private final UserPreferenceManager _userPreferenceManager;
    private final List<String> _regions;
    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordAuthenticatorRequestHandler.class);

    /**
     * Tiny container for template variable keys.
     */
    private static class ViewDataKeys
    {
        static final String USERNAME = "_username";
    }

    /**
     * Create a new instance of UsernamePasswordAuthenticatorRequestHandler using the configuration for this plugin.
     * <p>
     * The server knows how to provide an instance of this configuration because it is declared in the
     * plugin descriptor at {@link UsernamePasswordAuthenticatorPluginDescriptor#getConfigurationType()}.
     *
     * @param configuration for the Username/Password authenticator plugin
     */

    public UsernamePasswordAuthenticatorRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _credentialManager = configuration.getCredentialManager();
        _userPreferenceManager = configuration.getUserPreferenceManager();
        _regions = configuration.getApprovedRegions();

    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        // set the template and model for responses on the NOT_FAILURE scope
        response.setResponseModel(templateResponseModel(
                singletonMap(ViewDataKeys.USERNAME, _userPreferenceManager.getUsername()),
                "authenticate/get"), NOT_FAILURE);

        // on request validation failure, we should use the same template as for NOT_FAILURE
        response.setResponseModel(templateResponseModel(emptyMap(),
                "authenticate/get"), HttpStatus.BAD_REQUEST);

        //Make the regions from the configuration available in the frontend template
        response.putViewData("REGIONS", _regions, Response.ResponseModelScope.ANY);

        return new RequestModel(request);
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errors)
    {
        if (request.isPostRequest())
        {
            Collection<String> usernames = request.getFormParameterValues(RequestModel.Post.USERNAME_PARAM);

            if (!usernames.isEmpty())
            {
                // re-fill the username field with whatever was entered last time
                response.putViewData(ViewDataKeys.USERNAME, usernames.iterator().next(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Override
    public Optional<AuthenticationResult> get(RequestModel requestModel, Response response)
    {
        // authentication is never performed using GET, so return an empty optional to let the server know
        // authentication has not been performed.
        return Optional.empty();
    }

    @Override
    public Optional<AuthenticationResult> post(RequestModel requestModel, Response response)
    {
        RequestModel.Post model = requestModel.getPostRequestModel();

        Optional<AuthenticationResult> result = Optional.empty();

        @Nullable
        AuthenticationAttributes attributes = _credentialManager.verifyPassword(
                model.getUserName(),
                model.getPassword(),
                CredentialManager.NO_CONTEXT);

        if (attributes != null)
        {
            //Check if the submitted region is not in the list of approved regions from configuration
            if(!_regions.stream().anyMatch(str -> str.trim().equals(model.getRegion())))
            {
                response.addErrorMessage(ErrorMessage.withMessage("validation.error.incorrect.region"));

                // report a bad request so that the server will come back to the login template
                response.setHttpStatus(HttpStatus.BAD_REQUEST);
            }
            else {
                Attribute region = Attribute.of(AttributeName.of("region"), AttributeValue.of(model.getRegion()));
                // authentication was successful, set the result so that the server can see
                // the user account of the logged in user!
                // and add the region as a context attribute
                result = Optional.of(new AuthenticationResult(attributes.withContextAttribute(region)));

                _userPreferenceManager.saveUsername(model.getUserName());
            }
        }
        else
        {
            response.addErrorMessage(ErrorMessage.withMessage("validation.error.incorrect.credentials"));

            // report a bad request so that the server will come back to the login template
            response.setHttpStatus(HttpStatus.BAD_REQUEST);
        }

        return result;
    }
}
