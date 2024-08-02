/*
 *  Copyright 2023 Curity AB
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

package io.curity.identityserver.plugin.usernamepassword.templates;

import se.curity.identityserver.sdk.haapi.HaapiContract;
import se.curity.identityserver.sdk.haapi.Message;
import se.curity.identityserver.sdk.haapi.RepresentationFactory;
import se.curity.identityserver.sdk.haapi.RepresentationFunction;
import se.curity.identityserver.sdk.haapi.RepresentationModel;
import se.curity.identityserver.sdk.http.HttpMethod;
import se.curity.identityserver.sdk.http.MediaType;
import se.curity.identityserver.sdk.web.Representation;

import java.net.URI;
import java.util.Optional;

import static se.curity.identityserver.sdk.haapi.HaapiContract.Links.Relations.FORGOT_ACCOUNT_ID;

public class AuthenticateGetRepresentationFunction implements RepresentationFunction
{
    private static final Message MSG_TITLE = Message.ofKey("meta.title");
    private static final Message MSG_ACTION = Message.ofKey("view.authenticate");
    private static final Message MSG_HEADER = Message.ofKey("view.top-header");
    private static final Message MSG_USERNAME = Message.ofKey("view.username");
    private static final Message MSG_PASSWORD = Message.ofKey("view.password");
    private static final Message MSG_FORGOT_PASSWORD = Message.ofKey("view.forgot-password");
    private static final Message MSG_FORGOT_ACCOUNT_ID = Message.ofKey("view.forgot-account-id");
    private static final Message MSG_REGISTER = Message.ofKey("view.no-account");

    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        String authUrl = model.getString("_authUrl");
        Optional<String> registerUrl = model.getOptionalString("_registerUrl");
        Optional<String> username = model.getOptionalString("_username");

        return factory.newAuthenticationStep(step -> {
            step.addMessage(MSG_HEADER, HaapiContract.MessageClasses.HEADING);
            step.addFormAction(HaapiContract.Actions.Kinds.LOGIN, URI.create(authUrl),
                    HttpMethod.POST,
                    MediaType.X_WWW_FORM_URLENCODED,
                    MSG_TITLE,
                    MSG_ACTION,
                    fields -> {
                        fields.addUsernameField("userName", MSG_USERNAME, username.orElse(""));
                        fields.addPasswordField("password", MSG_PASSWORD);
                    });

            step.addLink(URI.create(authUrl + "/forgot-password"),
                    HaapiContract.Links.Relations.FORGOT_PASSWORD,
                    MSG_FORGOT_PASSWORD);

            step.addLink(URI.create(authUrl + "/forgot-account-id"),
                    FORGOT_ACCOUNT_ID,
                    MSG_FORGOT_ACCOUNT_ID);

            registerUrl.ifPresent(regUrl -> step.addLink(
                    URI.create(regUrl),
                    HaapiContract.Links.Relations.REGISTER_CREATE,
                    MSG_REGISTER));
        });
    }
}
