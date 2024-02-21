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
import io.curity.identityserver.plugin.usernamepassword.registration.RequestModel.RegistrationRequestModel;
import io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.attribute.scim.v2.Name;
import se.curity.identityserver.sdk.attribute.scim.v2.multivalued.PhoneNumber;
import se.curity.identityserver.sdk.authentication.ActivationResult;
import se.curity.identityserver.sdk.authentication.RegistrationRequestHandler;
import se.curity.identityserver.sdk.authentication.RegistrationResult;
import se.curity.identityserver.sdk.errors.ExternalServiceException;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountCreationResult;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.service.credential.CredentialUpdateResult;
import se.curity.identityserver.sdk.service.credential.UserCredentialManager;
import se.curity.identityserver.sdk.service.credential.results.SubjectCredentialsNotFound;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

/**
 * Username/Password Authenticator registration request handler.
 */
public final class UsernamePasswordRegistrationRequestHandler implements RegistrationRequestHandler<RequestModel>
{
    private static final Logger _logger = LoggerFactory.getLogger(UsernamePasswordRegistrationRequestHandler.class);
    private final AccountManager _accountManager;
    private final UserCredentialManager _userCredentialManager;
    private final AuthenticatorInformationProvider _authenticatorInformationProvider;
    private final UserPreferenceManager _userPreferenceManager;

    public UsernamePasswordRegistrationRequestHandler(UsernamePasswordAuthenticatorPluginConfig config)
    {
        _accountManager = config.getAccountManager();
        _userCredentialManager = config.getCredentialManager();
        _authenticatorInformationProvider = config.getAuthenticatorInformationProvider();
        _userPreferenceManager = config.getUserPreferenceManager();
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        var data = new HashMap<String, Object>(2);
        data.put(ViewModelReservedKeys.SHOW_PASSWORD_FIELDS, !_accountManager.isSetPasswordAfterActivation());
        data.put(ViewModelReservedKeys.SHOW_EMAIL_FIELD, !_accountManager.useUsernameAsEmail());

        if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(data, "create-account/get"),
                    HttpStatus.BAD_REQUEST);

            response.setResponseModel(templateResponseModel(data, "create-account/post"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }
        else if (request.isGetRequest())
        {
            response.setResponseModel(templateResponseModel(data, "create-account/get"),
                    Response.ResponseModelScope.NOT_FAILURE);
        }

        response.putViewData("_hasCredentialPolicy", true, Response.ResponseModelScope.ANY);
        return new RequestModel(request, _accountManager.useUsernameAsEmail(), _accountManager.isSetPasswordAfterActivation());
    }

    @Override
    public Optional<RegistrationResult> get(RequestModel request, Response response)
    {
       return Optional.empty();
    }

    @Override
    public Optional<RegistrationResult> post(RequestModel requestModel, Response response)
    {
        @Nullable ErrorMessage error;

        var model = requestModel.getPostRequestModel();

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
            onPostRequestValidationError(response, model);
            return Optional.empty();
        }
        else
        {
            return createAccount(model, response);
        }
    }

    private Optional<RegistrationResult> createAccount(RegistrationRequestModel requestModel, Response response)
    {
        String password = requestModel.getPassword();
        AccountAttributes modelAccount = AccountAttributes.of(requestModel.getUserName(), password, requestModel.getPrimaryEmail())
                .withActive(false);

        String firstName = trimmed(requestModel.getFirstName());
        String lastName = trimmed(requestModel.getLastName());
        if (!firstName.isEmpty() || !lastName.isEmpty())
        {
            modelAccount = modelAccount.withName(Name.of(firstName, lastName));
        }

        String phoneNumber = trimmed(requestModel.getPrimaryPhoneNumber());
        if (!phoneNumber.isEmpty())
        {
            modelAccount = modelAccount.withPhoneNumbers(PhoneNumber.of(phoneNumber, true));
        }

        AccountAttributes account = modelAccount
                .withActive(false);

        try
        {
            ErrorMessage error = _accountManager.ensureNonDuplicateAccount(
                    requestModel.getUserName(),
                    requestModel.getPrimaryEmail(),
                    requestModel.getPrimaryPhoneNumber()).orElse(null);
            if (error != null)
            {
                response.addErrorMessage(error);
                return Optional.empty();
            }

            // Use the easiest to manage technique from the 9.0 documentation to save the user and password
            // https://curity.io/docs/idsvr/latest/system-admin-guide/upgrade/8_7_X_to_9_0_0.html#credential-data-access-provider-plugins
            var accountResult = _accountManager.withCredentialManager(_userCredentialManager).create(account);
            if (accountResult instanceof AccountCreationResult.CredentialRejected rejected) {

                response.addErrorMessage(ErrorMessage.withMessage(CredentialUpdateResult.Rejected.CODE));

                var filteredDetails = rejected.getCredentialResult().getDetails().stream()
                        .filter(detail -> !(detail instanceof SubjectCredentialsNotFound)).toList();
                response.putViewData("_rejection_details", filteredDetails, Response.ResponseModelScope.FAILURE);

                onPostRequestValidationError(response, requestModel);
                return Optional.empty();
            }
        }
        catch (ExternalServiceException e)
        {
            _logger.warn("Error creating account", e);

            // Add a generic error to the response's errors array to avoid a backend error
            // being shown to users in the form.
            response.addErrorMessage(ErrorMessage.withMessage(
                    "An error has occurred while creating a new account. " +
                    "Please try again and contact us if you still have problems."));

            // by adding an error to the response above, we ensure an error response will be returned
            return Optional.empty();
        }

        _userPreferenceManager.saveUsername(requestModel.getUserName());

        var activateAccountUrl = String.format("%s/activate-account",
                _authenticatorInformationProvider.getFullyQualifiedAnonymousUri());
        var model = new HashMap<String, Object>(1);
        model.put(ViewModelReservedKeys.ACTIVATION_ENDPOINT, activateAccountUrl);

        ActivationResult activationResult = _accountManager.initializeActivation(account, model);
        if (activationResult.isDone() || activationResult.isPending())
        {
            response.setResponseModel(activationResult.getModel(), HttpStatus.CREATED);
        }

        response.setHttpStatus(HttpStatus.CREATED);

        return Optional.empty();
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errorMessages)
    {
        if (request.isPostRequest())
        {
            var requestModel =
                    new RegistrationRequestModel(request, _accountManager.useUsernameAsEmail(), _accountManager.isSetPasswordAfterActivation());

            var data = new HashMap<String, Object>(1);
            data.put(ViewModelReservedKeys.FORM_POST_BACK, requestModel.dataOnError());

            // on POST validation failure, go back to the GET template
            response.setResponseModel(templateResponseModel(data,
                    "create-account/get"), HttpStatus.BAD_REQUEST);
        }
    }

    private void onPostRequestValidationError(Response response, RegistrationRequestModel model) {

        response.putViewData(ViewModelReservedKeys.FORM_POST_BACK, model.dataOnError(),
                Response.ResponseModelScope.FAILURE);
    }

    private String trimmed(@Nullable String value)
    {
        return Optional.ofNullable(value)
                .map(String::trim)
                .orElse("");
    }
}
