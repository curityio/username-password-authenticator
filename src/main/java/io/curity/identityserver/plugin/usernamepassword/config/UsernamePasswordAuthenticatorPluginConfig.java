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

package io.curity.identityserver.plugin.usernamepassword.config;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.CredentialManager;
import se.curity.identityserver.sdk.service.EmailSender;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.NonceTokenIssuer;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.service.credential.UserCredentialManager;

import java.util.Optional;

/**
 * Username/Password Authenticator Configuration.
 */
@Description("A custom implementation of username password flows")
public interface UsernamePasswordAuthenticatorPluginConfig extends Configuration
{
    @Description("The Credential Manager is used to verify and update credentials")
    CredentialManager getCredentialManager();

    @Description("The User Credential Manager is used to verify and update credentials")
    UserCredentialManager getUserCredentialManager();

    @Description("An object to manage storing and restoring the current user")
    UserPreferenceManager getUserPreferenceManager();

    @Description("The Account Manager is used to fetch the account")
    AccountManager getAccountManager();

    @Description("Email provider to use for 'forgot password' and 'forgot username' procedures")
    Optional<EmailSender> getEmailSender();

    @Description("A nonce token issuer used in the 'forgot password' and 'set password' procedures")
    NonceTokenIssuer getNonceTokenIssuer();

    @Description("An object to manage getting information about authentication paths")
    AuthenticatorInformationProvider getAuthenticatorInformationProvider();

    @Description("An object to manage storing session information")
    SessionManager getSessionManager();

    @Description("An object to manage creating exceptions when invalid conditions are encountered")
    ExceptionFactory getExceptionFactory();
}
