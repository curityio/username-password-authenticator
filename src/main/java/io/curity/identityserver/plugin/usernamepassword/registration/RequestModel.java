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

import com.google.common.html.HtmlEscapers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.RandomStringUtils;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.web.Request;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Request model for the {@link UsernamePasswordRegistrationRequestHandler}.
 * <p>
 * It creates a nested {@link RegistrationRequestModel} and validates it in case the
 * received request uses the POST method, otherwise nothing is validated.
 */
public final class RequestModel
{
    @Valid
    @Nullable
    private final RegistrationRequestModel _postRequestModel;

    RequestModel(Request request, boolean isUsernameAsEmail, boolean isSetPasswordAfterActivation)
    {
        _postRequestModel = request.isPostRequest() ?
                new RegistrationRequestModel(request, isUsernameAsEmail, isSetPasswordAfterActivation) :
                null;
    }

    /**
     * @return the POST request model. Only call this method if the request is a POST request.
     */
    RegistrationRequestModel getPostRequestModel()
    {
        return Optional.ofNullable(_postRequestModel).orElseThrow(() ->
                new RuntimeException("POST Request Model does not exist"));
    }

    static class RegistrationRequestModel
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

        RegistrationRequestModel(Request request, boolean isUsernameAsEmail, boolean isSetPasswordAfterActivation)
        {
            _userName = request.getFormParameterValueOrError(KEY_USERNAME);

            // Default the email to the username with this option
            if (isUsernameAsEmail) {
                _primaryEmail = _userName;
            }
            else {
                _primaryEmail = request.getFormParameterValueOrError(KEY_PRIMARY_EMAIL);
            }

            _primaryPhoneNumber = request.getFormParameterValueOrError(KEY_PRIMARY_PHONE_NUMBER);

            _firstName = request.getFormParameterValueOrError(KEY_GIVEN_NAME);
            _lastName = request.getFormParameterValueOrError(KEY_FAMILY_NAME);

            // Default the password to a value the user will not know
            if (isSetPasswordAfterActivation) {
                _password = createRandomPassword(16);
            } else {
                _password = request.getFormParameterValueOrError(KEY_PASSWORD);
            }
        }

        public String getUserName()
        {
            return _userName;
        }

        public String getPassword()
        {
            return _password;
        }

        public String getPrimaryEmail()
        {
            return _primaryEmail;
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

        public Map<String, Object> dataOnError()
        {
            Map<String, Object> data = new HashMap<>(5);
            data.put(KEY_USERNAME, _userName == null ? "" : HtmlEscapers.htmlEscaper().escape(_userName));
            data.put(KEY_GIVEN_NAME, _firstName == null ? "" : HtmlEscapers.htmlEscaper().escape(_firstName));
            data.put(KEY_FAMILY_NAME, _lastName == null ? "" : HtmlEscapers.htmlEscaper().escape(_lastName));
            data.put(KEY_PRIMARY_EMAIL, _primaryEmail == null ? "" : HtmlEscapers.htmlEscaper().escape(_primaryEmail));
            data.put(KEY_PRIMARY_PHONE_NUMBER, _primaryPhoneNumber == null ? "" : HtmlEscapers.htmlEscaper().escape(_primaryPhoneNumber));
            return data;
        }

        private String createRandomPassword(int length) {

            // Call RandomStringUtils.randomAscii(16) with a SecureRandom object
            return RandomStringUtils.random(length, 32, 127, false, false, null, new SecureRandom());
        }
    }
}
