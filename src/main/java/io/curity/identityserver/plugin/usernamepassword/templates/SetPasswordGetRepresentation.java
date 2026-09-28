/*
 *  Copyright 2026 Curity AB
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

import static io.curity.identityserver.plugin.usernamepassword.setPassword.RequestModel.NONCE_IS_INVALID;
import static io.curity.identityserver.plugin.usernamepassword.setPassword.RequestModel.OTP_REQUIRED;
import static io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys.SET_PASSWORD_ENDPOINT;

/**
 * The set password page asks for the OTP sent by email, then for the new password.
 */
public final class SetPasswordGetRepresentation implements RepresentationFunction
{
    private static final Message MSG_OTP_TITLE = Message.ofKey("view.otp.head1");
    private static final Message MSG_OTP_INFO = Message.ofKey("view.otp.p1");
    private static final Message MSG_LABEL_OTP = Message.ofKey("view.otp");
    private static final Message MSG_OTP_ACTION = Message.ofKey("view.otp.button");
    private static final Message MSG_PASSWORD_TITLE = Message.ofKey("view.head1");
    private static final Message MSG_LABEL_PASSWORD = Message.ofKey("view.newPassword");
    private static final Message MSG_PASSWORD_ACTION = Message.ofKey("view.button");
    private static final Message MSG_CANCEL = Message.ofKey("view.cancel");
    private static final Message MSG_INVALID = Message.ofKey("validation.error.token.invalid");

    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        URI authUrl = URI.create(model.getString("_authUrl"));

        if (model.getBoolean(NONCE_IS_INVALID, false))
        {
            return factory.newAuthenticationStep(step -> {
                step.addMessage(MSG_INVALID, HaapiContract.MessageClasses.ERROR);
                step.addFormAction(HaapiContract.Actions.Kinds.CONTINUE, authUrl, HttpMethod.GET,
                        null, null, MSG_CANCEL);
            });
        }

        URI setPasswordUrl = URI.create(model.getOptionalString("_anonymousUrl")
                .map(anonymousUrl -> anonymousUrl + "/set-password")
                .orElseGet(() -> model.getString(SET_PASSWORD_ENDPOINT)));
        boolean otpRequired = model.getBoolean(OTP_REQUIRED, false);

        return factory.newAuthenticationStep(step -> {
            if (otpRequired)
            {
                step.addMessage(MSG_OTP_INFO);
            }
            step.addFormAction(
                    HaapiContract.Actions.Kinds.PASSWORD_RESET,
                    setPasswordUrl,
                    HttpMethod.POST,
                    MediaType.X_WWW_FORM_URLENCODED,
                    otpRequired ? MSG_OTP_TITLE : MSG_PASSWORD_TITLE,
                    otpRequired ? MSG_OTP_ACTION : MSG_PASSWORD_ACTION,
                    fields -> {
                        if (otpRequired)
                        {
                            fields.addTextField("otp", MSG_LABEL_OTP);
                        }
                        else
                        {
                            fields.addPasswordField("password", MSG_LABEL_PASSWORD);
                        }
                    });
            step.addFormAction(HaapiContract.Actions.Kinds.CANCEL, authUrl, HttpMethod.GET,
                    null, null, MSG_CANCEL);
        });
    }
}
