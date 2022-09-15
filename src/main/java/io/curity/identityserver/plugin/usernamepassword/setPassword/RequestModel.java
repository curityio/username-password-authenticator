/*
 * Copyright (C) 2022 Curity AB. All rights reserved.
 *
 * The contents of this file are the property of Curity AB.
 * You may not copy or use this file, in either source code
 * or executable form, except in compliance with terms
 * set by Curity AB.
 *
 * For further information, please contact Curity AB.
 */

package io.curity.identityserver.plugin.usernamepassword.setPassword;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;

import java.util.Collection;
import java.util.Optional;

public class RequestModel
{
    public static final String NONCE_IS_INVALID = "_nonce_is_invalid";

    @Nullable
    @Valid
    private final Get _getRequestModel;

    @Nullable
    @Valid
    private final Post _postRequestModel;

    public RequestModel(Request request, Response response)
    {
        _getRequestModel = request.isGetRequest() ? new Get(request, response) : null;
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

        public Get(Request request, Response response)
        {
            _tokenValues = request.getQueryParameterValues(TOKEN_PARAM);
            _token = _tokenValues.iterator().hasNext() ? _tokenValues.iterator().next() : "";

            // until the nonce is validated, we must consider it invalid
            response.putViewData(NONCE_IS_INVALID, true, Response.ResponseModelScope.ANY);
        }

        public String getToken()
        {
            return _token;
        }
    }

    public static class Post
    {
        static final String PASSWORD_PARAM = "password";

        @NotBlank(message = "validation.error.password.required")
        private final String _password;

        public Post(Request request) {
            _password = request.getFormParameterValueOrError(PASSWORD_PARAM);
        }

        public String getPassword()
        {
            return _password;
        }
    }
}
