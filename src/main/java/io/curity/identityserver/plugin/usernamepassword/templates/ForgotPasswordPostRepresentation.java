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
import se.curity.identityserver.sdk.web.Representation;

import java.net.URI;

import static io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys.RECIPIENT_OF_COMMUNICATION;
import static io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys.SET_PASSWORD_ENDPOINT;

public class ForgotPasswordPostRepresentation implements RepresentationFunction
{
    private static final Message MSG_FURTHER_INSTRUCTIONS = Message.ofKey("view.success.further-instructions");
    private static final Message MSG_NO_EMAIL = Message.ofKey("view.success.no-email");
    private static final Message MSG_CHECK_SPAM_FOLDER = Message.ofKey("view.success.check-your-spam-folder");
    private static final Message MSG_CONTINUE = Message.ofKey("view.success.return-to-login");
    private static final Message MSG_ENTER_CODE = Message.ofKey("view.success.enter-code");

    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        return factory.newAuthenticationStep(builder -> {
            builder.addMessage(MSG_FURTHER_INSTRUCTIONS, HaapiContract.MessageClasses.HEADING);
            Message recipientOfCommunication = Message.ofLiteral(mask(model.getString(RECIPIENT_OF_COMMUNICATION)));
            builder.addMessage(recipientOfCommunication, HaapiContract.MessageClasses.RECIPIENT_OF_COMMUNICATION);
            builder.addMessage(MSG_NO_EMAIL);
            builder.addMessage(MSG_CHECK_SPAM_FOLDER);

            String setPasswordUrl = model.getOptionalString("_anonymousUrl")
                    .map(anonymousUrl -> anonymousUrl + "/set-password")
                    .orElseGet(() -> model.getString(SET_PASSWORD_ENDPOINT));
            builder.addFormAction(HaapiContract.Actions.Kinds.CONTINUE, URI.create(setPasswordUrl),
                    HttpMethod.GET, null, null, MSG_ENTER_CODE);

            builder.addLink(URI.create(model.getString("_authUrl")), HaapiContract.Links.Relations.RESTART, MSG_CONTINUE);
        });
    }

    private static String mask(String recipientOfCommunication)
    {
        int atIndex = recipientOfCommunication.indexOf("@");
        if (atIndex < 0)
        {
            // not an email address, e.g. the username entered for an unknown account
            return maskPart(recipientOfCommunication);
        }
        String prefix = recipientOfCommunication.substring(0, atIndex);
        String domain = recipientOfCommunication.substring(atIndex + 1);

        return maskPart(prefix) + "@" + maskPart(domain);
    }

    /**
     * Keep only the first third of the value.
     */
    private static String maskPart(String value)
    {
        int oneThirdLength = Math.min(value.length(), Math.max(1, value.length() / 3));
        return value.substring(0, oneThirdLength) + "****";
    }
}
