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
import jakarta.validation.constraints.AssertTrue;
import org.apache.commons.lang3.StringUtils;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.web.Request;

import java.util.Optional;

public class RequestModel
{
    // no valid OTP or verified OTP exists in the session
    public static final String NONCE_IS_INVALID = "_nonce_is_invalid";

    // the user must enter the OTP before setting a new password
    public static final String OTP_REQUIRED = "_otp_required";

    @Nullable
    @Valid
    private final Post _postRequestModel;

    public RequestModel(Request request)
    {
        _postRequestModel = request.isPostRequest() ? new Post(request) : null;
    }

    public Post getPostRequestModel()
    {
        return Optional.ofNullable(_postRequestModel).orElseThrow(() ->
                new RuntimeException("POST RequestModel does not exist"));
    }

    /**
     * Either the OTP form or the password form is posted.
     */
    public static class Post
    {
        static final String OTP_PARAM = "otp";
        static final String PASSWORD_PARAM = "password";

        @Nullable
        private final String _otp;

        @Nullable
        private final String _password;

        public Post(Request request)
        {
            _otp = request.getFormParameterValueOrError(OTP_PARAM);
            _password = request.getFormParameterValueOrError(PASSWORD_PARAM);
        }

        public boolean isOtpSubmission()
        {
            return _otp != null;
        }

        @Nullable
        public String getOtp()
        {
            return _otp;
        }

        @Nullable
        public String getPassword()
        {
            return _password;
        }

        @AssertTrue(message = "validation.error.otp.required")
        public boolean isOtpValid()
        {
            return !isOtpSubmission() || StringUtils.isNotBlank(_otp);
        }

        @AssertTrue(message = "validation.error.password.required")
        public boolean isPasswordValid()
        {
            return isOtpSubmission() || StringUtils.isNotBlank(_password);
        }
    }
}
