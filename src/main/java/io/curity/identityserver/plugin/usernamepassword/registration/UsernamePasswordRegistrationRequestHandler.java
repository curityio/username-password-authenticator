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

package io.curity.identityserver.plugin.usernamepassword.registration;

import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.registration.RequestModel.HtmlFormRegistrationRequestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.attribute.scim.v2.Name;
import se.curity.identityserver.sdk.attribute.scim.v2.multivalued.PhoneNumber;
import se.curity.identityserver.sdk.authentication.RegistrationRequestHandler;
import se.curity.identityserver.sdk.authentication.RegistrationResult;
import se.curity.identityserver.sdk.errors.ExternalServiceException;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.CredentialManager;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.Optional;

import static java.util.Collections.emptyMap;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

/**
 * Username/Password Authenticator registration request handler.
 */
public final class UsernamePasswordRegistrationRequestHandler implements RegistrationRequestHandler<RequestModel>
{

    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordRegistrationRequestHandler.class);

    private final AccountManager _accountManager;
    private final CredentialManager _credentialManager;

    public UsernamePasswordRegistrationRequestHandler(UsernamePasswordAuthenticatorPluginConfig config)
    {
        _accountManager = config.getAccountManager();
        _credentialManager = config.getCredentialManager();
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        if (request.isPostRequest())
        {
            // POST request
            response.setResponseModel(templateResponseModel(emptyMap(), "create-account/get"),
                    HttpStatus.BAD_REQUEST);

            response.setResponseModel(templateResponseModel(emptyMap(), "create-account/post"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }
        else if (request.isGetRequest())
        {
            // GET request
            response.setResponseModel(templateResponseModel(emptyMap(), "create-account/get"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }

        return new RequestModel(request);
    }

    @Override
    public Optional<RegistrationResult> get(RequestModel request, Response response)
    {
        // nothing needs to be done on GET requests, only serve the appropriate template as already done in the
        // preProcess method.
        return Optional.empty();
    }

    @Override
    public Optional<RegistrationResult> post(RequestModel requestModel, Response response)
    {
        @Nullable ErrorMessage error;

        HtmlFormRegistrationRequestModel model = requestModel.getPostRequestModel();

        try
        {
            error = _accountManager.ensureNonDuplicateAccount(
                    model.getUserName(), model.getPrimaryEmail()).orElse(null);
        }
        catch (RuntimeException e)
        {
            _logger.error("An unexpected error occurred while check if the user account exists prior to creating it", e);

            error = ErrorMessage.withMessage("error.duplicateAccountCheckFailed");
        }

        if (error != null)
        {
            response.addErrorMessage(error);
            return Optional.empty();
        }
        else
        {
            return createAccount(model, response);
        }

    }

    private Optional<RegistrationResult> createAccount(HtmlFormRegistrationRequestModel requestModel, Response response)
    {
        String password = requestModel.getPassword();

        // never give a plain-text password to the account directly, use a CredentialManager to transform
        // (hash, salt etc.) the password
        String transformedPassword = _credentialManager.transform(requestModel.getUserName(), password, null);

        AccountAttributes account = AccountAttributes.of(
                requestModel.getUserName(), transformedPassword, requestModel.getPrimaryEmail())
                // this means we will not require confirmation of email/phoneNumber before activating the account
                .withActive(true);

        String firstName = trimmed(requestModel.getFirstName());
        String lastName = trimmed(requestModel.getLastName());

        // if at least one name is given, add it to the account
        if (!firstName.isEmpty() || !lastName.isEmpty())
        {
            account = account.withName(Name.of(firstName, lastName));
        }

        String phoneNumber = trimmed(requestModel.getPrimaryPhoneNumber());

        if (!phoneNumber.isEmpty())
        {
            account = account.withPhoneNumbers(PhoneNumber.of(phoneNumber, true));
        }

        try
        {
            _accountManager.createAccount(account);
        }
        catch (ExternalServiceException e)
        {
            _logger.warn("Error creating account", e);

            // Add a generic error to the response's errors array to avoid a backend error
            // being shown to users in the form.
            response.addErrorMessage(ErrorMessage.withMessage("An error has occurred while creating a new account. " +
                    "Please try again and contact us if you still have problems."));

            // by adding an error to the response above, we ensure an error response will be returned
            return Optional.empty();
        }

        response.setHttpStatus(HttpStatus.CREATED);

        return Optional.empty();
    }

    private String trimmed(@Nullable String value)
    {
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }

}
