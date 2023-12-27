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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import se.curity.identityserver.sdk.haapi.HaapiContract;
import se.curity.identityserver.sdk.haapi.Message;
import se.curity.identityserver.sdk.haapi.RepresentationFactory;
import se.curity.identityserver.sdk.haapi.RepresentationFunction;
import se.curity.identityserver.sdk.haapi.RepresentationModel;
import se.curity.identityserver.sdk.web.Representation;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys.RECIPIENT_OF_COMMUNICATION;

public class ForgotPasswordPostRepresentation implements RepresentationFunction
{
    private static final Message MSG_FURTHER_INSTRUCTIONS = Message.ofKey("view.success.further-instructions");
    private static final Message MSG_NO_EMAIL = Message.ofKey("view.success.no-email");
    private static final Message MSG_CHECK_SPAM_FOLDER = Message.ofKey("view.success.check-your-spam-folder");
    private static final Message MSG_CONTINUE = Message.ofKey("view.success.return-to-login");

    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        return factory.newAuthenticationStep(builder -> {
            builder.addMessage(MSG_FURTHER_INSTRUCTIONS, HaapiContract.MessageClasses.HEADING);
            Message recipientOfCommunication = Message.ofLiteral(
                    mask(model.getString(RECIPIENT_OF_COMMUNICATION)));
            builder.addMessage(recipientOfCommunication, HaapiContract.MessageClasses.RECIPIENT_OF_COMMUNICATION);
            builder.addMessage(MSG_NO_EMAIL);
            builder.addMessage(MSG_CHECK_SPAM_FOLDER);

            builder.addLink(
                    URI.create(model.getString("_authUrl")),
                    HaapiContract.Links.Relations.RESTART,
                    MSG_CONTINUE
            );
        });
    }

    private static String mask(String recipientOfCommunication)
    {
        if (StringUtils.isBlank(recipientOfCommunication))
        {
            // to prevent attackers phishing for valid email accounts, pretend an email was sent to some random address
            recipientOfCommunication = RandomStringUtils.random(12, true, true);
        }

        Set<Integer> groupsToReplaceSet = new HashSet<>(Arrays.asList(2, 5));

        Matcher matcher = Pattern.compile("(.*?)([^@]{1,4})(@)(.*?)([^.]{1,4})(\\.)?(.*)?").matcher(recipientOfCommunication);

        if (matcher.matches())
        {
            List<String> result = new ArrayList<>(matcher.groupCount());

            for (int i = 1; i <= matcher.groupCount(); i++)
            {
                @Nullable String group = matcher.group(i);
                if (group == null)
                {
                    continue;
                }
                result.add(groupsToReplaceSet.contains(i) ? StringUtils.repeat("x", group.length()) : group);
            }

            return String.join("", result);
        }
        else
        {
            return recipientOfCommunication;
        }
    }
}
