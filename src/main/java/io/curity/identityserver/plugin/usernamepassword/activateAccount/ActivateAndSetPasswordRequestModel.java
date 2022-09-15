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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.web.Request;

import java.util.Collection;
import java.util.Optional;

public class ActivateAndSetPasswordRequestModel {

    @Valid
    @Nullable
    private final Get _getRequestModel;

    @Valid
    @Nullable
    private final Post _postRequestModel;

    public ActivateAndSetPasswordRequestModel(Request request)
    {
        _getRequestModel = request.isGetRequest() ? new Get(request) : null;
        _postRequestModel = request.isPostRequest() ? new Post(request) : null;
    }

    public Get getGetRequestModel()
    {
        return Optional.ofNullable(_getRequestModel).orElseThrow(() ->
                new RuntimeException("GET RequestModel does not exist"));
    }

    public Post getPostRequestModel()
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
    }

    public static class Post
    {
        static final String PASSWORD_PARAM = "password";

        @NotEmpty(message = "validation.error.password.required")
        private final String _password;

        public Post(Request request)
        {
            _password = request.getFormParameterValueOrError(PASSWORD_PARAM);
        }

        public String getPassword()
        {
            return _password;
        }
    }
}
