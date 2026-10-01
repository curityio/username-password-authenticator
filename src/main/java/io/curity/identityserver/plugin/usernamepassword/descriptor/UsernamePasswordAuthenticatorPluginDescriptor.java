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

package io.curity.identityserver.plugin.usernamepassword.descriptor;

import com.google.common.collect.ImmutableMap;
import io.curity.identityserver.plugin.usernamepassword.activateAccount.UsernamePasswordActivateAccountLandingRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.activateAccount.UsernamePasswordActivateAccountRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.activateAccount.UsernamePasswordActivateAndSetPasswordRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.authentication.UsernamePasswordAuthenticationRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.forgotAccountId.UsernamePasswordForgotAccountIdRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.forgotPassword.UsernamePasswordForgotPasswordRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.registration.UsernamePasswordRegistrationRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.setPassword.UsernamePasswordSetPasswordRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.templates.AuthenticateGetRepresentationFunction;
import io.curity.identityserver.plugin.usernamepassword.templates.CreateAccountGetRepresentationFunction;
import io.curity.identityserver.plugin.usernamepassword.templates.CreateAccountPostRepresentationFunction;
import io.curity.identityserver.plugin.usernamepassword.templates.ForgotAccountIdGetRepresentation;
import io.curity.identityserver.plugin.usernamepassword.templates.ForgotAccountIdPostRepresentation;
import io.curity.identityserver.plugin.usernamepassword.templates.ForgotPasswordGetRepresentation;
import io.curity.identityserver.plugin.usernamepassword.templates.ForgotPasswordPostRepresentation;
import io.curity.identityserver.plugin.usernamepassword.templates.SetPasswordGetRepresentation;
import io.curity.identityserver.plugin.usernamepassword.templates.SetPasswordPostRepresentation;
import se.curity.identityserver.sdk.authentication.AnonymousRequestHandler;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.authentication.RegistrationRequestHandler;
import se.curity.identityserver.sdk.haapi.RepresentationFunction;
import se.curity.identityserver.sdk.plugin.descriptor.AuthenticatorPluginDescriptor;
import se.curity.identityserver.sdk.web.RequestHandlerSet;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * Username/Password Authenticator plugin descriptor.
 */
public final class UsernamePasswordAuthenticatorPluginDescriptor
        implements AuthenticatorPluginDescriptor<UsernamePasswordAuthenticatorPluginConfig>
{

    @Override
    public String getPluginImplementationType()
    {
        return "username-password-authenticator";
    }

    @Override
    public Class<? extends UsernamePasswordAuthenticatorPluginConfig> getConfigurationType()
    {
        return UsernamePasswordAuthenticatorPluginConfig.class;
    }

    @Override
    public Map<String, Class<? extends AuthenticatorRequestHandler<?>>> getAuthenticationRequestHandlerTypes()
    {
        return new HashMap<String, Class<? extends AuthenticatorRequestHandler<?>>>() {{
            put("index", UsernamePasswordAuthenticationRequestHandler.class);
            put("forgot-password", UsernamePasswordForgotPasswordRequestHandler.class);
            put("forgot-account-id", UsernamePasswordForgotAccountIdRequestHandler.class);
        }};
    }

    @Override
    public Map<String, Class<? extends AnonymousRequestHandler<?>>> getAnonymousRequestHandlerTypes()
    {
        return new HashMap<String, Class<? extends AnonymousRequestHandler<?>>>() {{
            put("set-password", UsernamePasswordSetPasswordRequestHandler.class);
            put("activate-account", UsernamePasswordActivateAccountLandingRequestHandler.class);
            put("activate", UsernamePasswordActivateAccountRequestHandler.class);
            put("activate-and-set", UsernamePasswordActivateAndSetPasswordRequestHandler.class);
        }};
    }

    @Override
    public Map<String, Class<? extends RegistrationRequestHandler<?>>> getRegistrationRequestHandlerTypes()
    {
        return singletonMap("index", UsernamePasswordRegistrationRequestHandler.class);
    }

    @Override
    public RequestHandlerSet allowedHandlersForCrossSiteNonSafeRequests()
    {
        // No handler accepts cross-site non-safe (e.g. POST) requests. The set-password handlers change the
        // password of the account held in the session, so they must not accept cross-site POSTs (CSRF).
        // The links sent by email are opened with a GET, which is not affected, and the forms post same-site.
        return RequestHandlerSet.none();
    }

    @Override
    public Map<String, Class<? extends RepresentationFunction>> getRepresentationFunctions()
    {
        return ImmutableMap.<String, Class<? extends RepresentationFunction>>builder()
                .put("authenticate/get", AuthenticateGetRepresentationFunction.class)
                .put("create-account/get", CreateAccountGetRepresentationFunction.class)
                .put("create-account/post", CreateAccountPostRepresentationFunction.class)
                .put("forgot-account-id/get", ForgotAccountIdGetRepresentation.class)
                .put("forgot-account-id/post", ForgotAccountIdPostRepresentation.class)
                .put("forgot-password/get", ForgotPasswordGetRepresentation.class)
                .put("forgot-password/post", ForgotPasswordPostRepresentation.class)
                .put("set-password/get", SetPasswordGetRepresentation.class)
                .put("set-password/post", SetPasswordPostRepresentation.class)
                .build();
    }
}
