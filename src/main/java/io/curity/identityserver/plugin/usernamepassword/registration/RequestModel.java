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

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.web.Request;

import javax.validation.Valid;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Request model for the {@link UsernamePasswordRegistrationRequestHandler}.
 * <p>
 * It creates a nested {@link HtmlFormRegistrationRequestModel} and validates it in case the
 * received request uses the POST method, otherwise nothing is validated.
 */
public final class RequestModel
{

    @Valid
    @Nullable
    private final HtmlFormRegistrationRequestModel _postRequestModel;

    RequestModel(Request request)
    {
        _postRequestModel = request.isPostRequest() ?
                new HtmlFormRegistrationRequestModel(request) :
                null;
    }

    /**
     * @return the POST request model. Only call this method if the request is a POST request.
     */
    HtmlFormRegistrationRequestModel getPostRequestModel()
    {
        return Optional.ofNullable(_postRequestModel).orElseThrow(() ->
                new RuntimeException("POST Request Model does not exist"));
    }

    static class HtmlFormRegistrationRequestModel
    {

        private static final String KEY_USERNAME = "userName";
        private static final String KEY_PRIMARY_EMAIL = "primaryEmail";
        private static final String KEY_PRIMARY_PHONE_NUMBER = "primaryPhoneNumber";
        private static final String KEY_PASSWORD = "password";
        private static final String KEY_GIVEN_NAME = "name.givenName";
        private static final String KEY_FAMILY_NAME = "name.familyName";

        @NotBlank(message = "error.validation.accountId.required")
        private final String _userName;

        @NotBlank(message = "error.validation.email.required")
        @Email(message = "error.validation.email.invalid")
        private final String _primaryEmail;

        @NotBlank(message = "error.validation.password.required")
        private final String _password;

        @Nullable
        private final String _primaryPhoneNumber;

        @Nullable
        private final String _firstName;

        @Nullable
        private final String _lastName;

        HtmlFormRegistrationRequestModel(Request request)
        {
            _userName = request.getFormParameterValueOrError(KEY_USERNAME);
            _primaryEmail = request.getFormParameterValueOrError(KEY_PRIMARY_EMAIL);
            _primaryPhoneNumber = request.getFormParameterValueOrError(KEY_PRIMARY_PHONE_NUMBER);
            _password = request.getFormParameterValueOrError(KEY_PASSWORD);
            _firstName = request.getFormParameterValueOrError(KEY_GIVEN_NAME);
            _lastName = request.getFormParameterValueOrError(KEY_FAMILY_NAME);
        }

        public String getUserName()
        {
            return Optional.ofNullable(_userName).orElseThrow(missingValueOf(KEY_USERNAME));
        }

        public String getPassword()
        {
            return Optional.ofNullable(_password).orElseThrow(missingValueOf(KEY_PASSWORD));
        }

        public String getPrimaryEmail()
        {
            return Optional.ofNullable(_primaryEmail).orElseThrow(missingValueOf(KEY_PRIMARY_EMAIL));
        }

        @Nullable
        public String getPrimaryPhoneNumber()
        {
            return _primaryPhoneNumber;
        }

        @Nullable
        public String getFirstName()
        {
            return _firstName;
        }

        @Nullable
        public String getLastName()
        {
            return _lastName;
        }

        private static Supplier<RuntimeException> missingValueOf(String parameterName)
        {
            return () -> new RuntimeException(String.format("Value of '%s' is missing", parameterName));
        }

    }

}
