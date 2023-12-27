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

import static io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys.SHOW_EMAIL_FIELD;
import static io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys.SHOW_PASSWORD_FIELDS;

public class CreateAccountGetRepresentationFunction implements RepresentationFunction
{
    private static final Message MSG_TITLE = Message.ofKey("meta.title");
    private static final Message MSG_DESCRIPTION = Message.ofKey("view.description");
    private static final Message MSG_ACTION_SUBMIT_TITLE = Message.ofKey("view.button");

    private static final Message MSG_FIELD_FIRST_NAME = Message.ofKey("view.firstName");
    private static final Message MSG_FIELD_LAST_NAME = Message.ofKey("view.lastName");
    private static final Message MSG_FIELD_EMAIL = Message.ofKey("view.email");
    private static final Message MSG_FIELD_PHONE = Message.ofKey("view.phone");
    private static final Message MSG_FIELD_USERNAME = Message.ofKey("view.username");
    private static final Message MSG_FIELD_PASSWORD = Message.ofKey("view.password");
    private static final Message MSG_FIELD_TERMS = Message.ofKey("view.terms");

    private static final String KEY_USERNAME = "userName";
    private static final String KEY_PRIMARY_EMAIL = "primaryEmail";
    private static final String KEY_PRIMARY_PHONE_NUMBER = "primaryPhoneNumber";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_GIVEN_NAME = "name.givenName";
    private static final String KEY_FAMILY_NAME = "name.familyName";
    
    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        Optional<String> maybeRegisterUrl = model.getOptionalString("_registerUrl");
        boolean showEmailField = model.getOptionalBoolean(SHOW_EMAIL_FIELD).orElse(false);
        boolean showPasswordField = model.getOptionalBoolean(SHOW_PASSWORD_FIELDS).orElse(false);

        return factory.newRegistrationStep(step -> {
            step.addMessage(MSG_DESCRIPTION);
            step.addFormAction(HaapiContract.Actions.Kinds.USER_REGISTER,
                    URI.create(maybeRegisterUrl.orElse("")),
                    HttpMethod.POST, MediaType.X_WWW_FORM_URLENCODED,
                    MSG_TITLE, MSG_ACTION_SUBMIT_TITLE, fields -> {
                        fields.addTextField(KEY_GIVEN_NAME, MSG_FIELD_FIRST_NAME);
                        fields.addTextField(KEY_FAMILY_NAME, MSG_FIELD_LAST_NAME);
                        if (showEmailField)
                        {
                            fields.addTextField(KEY_PRIMARY_EMAIL, MSG_FIELD_EMAIL);
                        }
                        fields.addTextField(KEY_PRIMARY_PHONE_NUMBER, MSG_FIELD_PHONE);
                        fields.addUsernameField(KEY_USERNAME, MSG_FIELD_USERNAME);
                        if (showPasswordField)
                        {
                            fields.addPasswordField(KEY_PASSWORD, MSG_FIELD_PASSWORD);
                        }
                        fields.addCheckboxField("agreeToTerms", MSG_FIELD_TERMS, "on", false, false);
                    });
        });
    }
}
