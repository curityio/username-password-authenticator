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

import se.curity.identityserver.sdk.haapi.FieldKind;
import se.curity.identityserver.sdk.haapi.HaapiContract;
import se.curity.identityserver.sdk.haapi.Message;
import se.curity.identityserver.sdk.haapi.RepresentationFactory;
import se.curity.identityserver.sdk.haapi.RepresentationFunction;
import se.curity.identityserver.sdk.haapi.RepresentationModel;
import se.curity.identityserver.sdk.http.HttpMethod;
import se.curity.identityserver.sdk.http.MediaType;
import se.curity.identityserver.sdk.web.Representation;

import java.net.URI;

import static io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys.SHOW_EMAIL_FIELD;

public class ForgotPasswordGetRepresentation implements RepresentationFunction
{
    private static final Message MSG_FORM_TITLE = Message.ofKey("view.head1");
    private static final Message MSG_INFO = Message.ofKey("view.p1");
    private static final Message MSG_LABEL_EMAIL = Message.ofKey("view.email");
    private static final Message MSG_LABEL_USERNAME = Message.ofKey("view.username");
    private static final Message MSG_ACTION = Message.ofKey("view.button");
    private static final Message MSG_CANCEL = Message.ofKey("view.cancel");

    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        String authUrl = model.getString("_authUrl");
        String forgotPasswordUrl = authUrl + "/forgot-password";

        String userNameValue = model.getOptionalString("userName").orElse("");

        return factory.newAuthenticationStep(step -> {
            step.addMessage(MSG_INFO);
            step.addFormAction(
                    HaapiContract.Actions.Kinds.PASSWORD_RESET,
                    URI.create(forgotPasswordUrl),
                    HttpMethod.POST,
                    MediaType.X_WWW_FORM_URLENCODED,
                    MSG_FORM_TITLE,
                    MSG_ACTION,
                    fields -> {
                        if (model.getBoolean(SHOW_EMAIL_FIELD, false))
                        {
                            fields.addTextField("primaryEmail", MSG_LABEL_EMAIL, FieldKind.EMAIL);
                        }
                        fields.addUsernameField("userName", MSG_LABEL_USERNAME, userNameValue);
                    }
            );
            step.addLink(
                    URI.create(authUrl),
                    HaapiContract.Links.Relations.RESTART,
                    MSG_CANCEL);
        });
    }
}