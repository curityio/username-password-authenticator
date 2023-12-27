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

import static se.curity.identityserver.sdk.service.AutoLoginManager.FORM_NAME_TOKEN;
import static se.curity.identityserver.sdk.service.AutoLoginManager.PATH_CONFIRM_CONTINUE;
import static se.curity.identityserver.sdk.service.AutoLoginManager.TEMPLATE_KEY_HAS_AUTO_LOGIN;
import static se.curity.identityserver.sdk.service.AutoLoginManager.TEMPLATE_KEY_TOKEN;
import static se.curity.identityserver.sdk.service.AutoLoginManager.TEMPLATE_KEY_USER_NAME;

public class CreateAccountPostRepresentationFunction implements RepresentationFunction
{
    private static final Message MSG_SUCCESS = Message.ofKey("view.success.success");
    private static final Message MSG_RETURN_TO_LOGIN = Message.ofKey("view.success.return-to-login");

    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        boolean hasAutoLogin = model.getBoolean(TEMPLATE_KEY_HAS_AUTO_LOGIN, false);
        return factory.newRegistrationStep(step -> {
            step.addMessage(MSG_SUCCESS);
            step.addMessage(Message.ofKey("authenticator.username-password-authenticator.create-account." + model.getString("_activationMessage")));
            model.getOptionalString("_activationMessageAdditionalInfo").ifPresent(messageKey ->
                    step.addMessage(Message.ofKey("authenticator.username-password-authenticator.create-account." + messageKey))
            );

            step.addFormAction(
                    HaapiContract.Actions.Kinds.CONTINUE,
                    URI.create(model.getString("_authUrl")),
                    HttpMethod.GET,
                    MediaType.X_WWW_FORM_URLENCODED,
                    null, MSG_RETURN_TO_LOGIN, fields -> {
                    }
            );

            if (hasAutoLogin)
            {
                URI autoLoginUri = URI.create(
                        model.getString("_anonymousUrl") + PATH_CONFIRM_CONTINUE);
                String alsk = model.getString(TEMPLATE_KEY_TOKEN);
                String username = model.getString(TEMPLATE_KEY_USER_NAME);
                Message proceedString = Message.ofKey("authenticator.continue-to-login-template", username);
                step.addFormAction(
                        HaapiContract.Actions.Kinds.CONTINUE_AUTO_LOGIN,
                        autoLoginUri,
                        HttpMethod.POST,
                        MediaType.X_WWW_FORM_URLENCODED,
                        proceedString, proceedString,
                        fields -> fields.addHiddenField(FORM_NAME_TOKEN, alsk));
            }
        });
    }
}
