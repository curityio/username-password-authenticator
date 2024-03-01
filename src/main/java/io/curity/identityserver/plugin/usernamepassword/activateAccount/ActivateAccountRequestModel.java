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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.web.Request;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ActivateAccountRequestModel
{
    @Valid
    @Nullable
    private final Get _getRequestModel;

    @Valid
    @Nullable
    private final Post _postRequestModel;

    public ActivateAccountRequestModel(Request request)
    {
        _getRequestModel = request.isGetRequest() ? new ActivateAccountRequestModel.Get(request) : null;
        _postRequestModel = request.isPostRequest() ? new ActivateAccountRequestModel.Post(request) : null;
    }

    public ActivateAccountRequestModel.Get getGetRequestModel()
    {
        return Optional.ofNullable(_getRequestModel).orElseThrow(() ->
                new RuntimeException("GET RequestModel does not exist"));
    }

    public ActivateAccountRequestModel.Post getPostRequestModel()
    {
        return Optional.ofNullable(_postRequestModel).orElseThrow(() ->
                new RuntimeException("POST RequestModel does not exist"));
    }

    public static class Get
    {
        static final String TOKEN_PARAM = "token";

        @NotEmpty(message = "validation.error.token.required")
        private final String _token;

        // we must ensure there's only one token in the request
        @Size(min = 1, max = 1, message = "validation.error.token.required")
        private final Collection<String> _tokenValues;

        public Get(Request request)
        {
            _tokenValues = request.getQueryParameterValues(TOKEN_PARAM);
            _token = _tokenValues.iterator().hasNext() ? _tokenValues.iterator().next() : "";
        }

        public String getToken()
        {
            return _token;
        }

        public Map<String, Object> asMap()
        {
            var data = new HashMap<String, Object>(1);
            data.put(TOKEN_PARAM, getToken());
            return data;
        }
    }

    public static class Post
    {
        static final String PRIMARY_EMAIL_PARAM = "primaryEmail";

        @Email(message = "validation.error.email.invalid")
        private final String _email;

        public Post(Request request)
        {
            _email = request.getFormParameterValueOrError(PRIMARY_EMAIL_PARAM);
        }

        public String getEmail()
        {
            return _email;
        }
    }
}
