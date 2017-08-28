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

import io.curity.identityserver.plugin.usernamepassword.authentication.UsernamePasswordAuthenticatorRequestHandler;
import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.registration.UsernamePasswordRegistrationRequestHandler;
import se.curity.identityserver.sdk.authentication.AnonymousRequestHandler;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.authentication.RegistrationRequestHandler;
import se.curity.identityserver.sdk.plugin.descriptor.AuthenticatorPluginDescriptor;

import java.util.Map;

import static java.util.Collections.emptyMap;
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
        return singletonMap("index", UsernamePasswordAuthenticatorRequestHandler.class);
    }

    @Override
    public Map<String, Class<? extends AnonymousRequestHandler<?>>> getAnonymousRequestHandlerTypes()
    {
        return emptyMap();
    }

    @Override
    public Map<String, Class<? extends RegistrationRequestHandler<?>>> getRegistrationRequestHandlerTypes()
    {
        return singletonMap("index", UsernamePasswordRegistrationRequestHandler.class);
    }
}
