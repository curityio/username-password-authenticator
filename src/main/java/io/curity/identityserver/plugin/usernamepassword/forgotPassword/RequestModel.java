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

package io.curity.identityserver.plugin.usernamepassword.forgotPassword;

import com.google.common.html.HtmlEscapers;
import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import org.apache.commons.lang3.StringUtils;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.web.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RequestModel
{
    @Valid
    @Nullable
    private final PostRequestModel _postRequestModel;

    @Valid
    @Nullable
    private final GetRequestModel _getRequestModel;

    RequestModel(Request request, UserPreferenceManager userPreferenceManager)
    {
        _postRequestModel = request.isPostRequest() ? new PostRequestModel(request) : null;
        _getRequestModel = request.isGetRequest() ? new GetRequestModel(userPreferenceManager) : null;
    }

    PostRequestModel getPostRequestModel()
    {
        return Optional.ofNullable(_postRequestModel).orElseThrow(() ->
                new RuntimeException("POST RequestModel does not exist"));
    }

    static class GetRequestModel
    {
        @Nullable
        private final String _username;

        public GetRequestModel(UserPreferenceManager userPreferenceManager)
        {
            _username = userPreferenceManager.getUsername();
        }

        @Nullable
        public String getUsername()
        {
            return _username;
        }
    }

    static class PostRequestModel
    {
        private static final String USERNAME_PARAM = "userName";
        private static final String PRIMARY_EMAIL_PARAM = "primaryEmail";

        private final String _username;
        private final String _email;

        PostRequestModel(Request request)
        {
            _username = request.getFormParameterValueOrError(USERNAME_PARAM);
            _email = request.getFormParameterValueOrError(PRIMARY_EMAIL_PARAM);
        }

        Map<String, Object> dataOnError()
        {
            var data = new HashMap<String, Object>(2);
            data.put(USERNAME_PARAM, _username == null ? "" : HtmlEscapers.htmlEscaper().escape(_username));
            data.put(PRIMARY_EMAIL_PARAM, _email == null ? "" : HtmlEscapers.htmlEscaper().escape(_email));
            return data;
        }

        @Nullable
        @Email(message = "validation.error.email.invalid")
        public String getPrimaryEmail()
        {
            return _email;
        }

        @Nullable
        public String getUsername()
        {
            return _username;
        }

        @AssertTrue(message = "error.email.or.accountId.required")
        public boolean isUserName()
        {
            // at least one of the fields must have been provided
            return StringUtils.isNotBlank(getPrimaryEmail()) || StringUtils.isNotBlank(getUsername());
        }
    }
}
