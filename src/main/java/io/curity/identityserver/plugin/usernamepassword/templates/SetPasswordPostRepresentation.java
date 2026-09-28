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
import se.curity.identityserver.sdk.web.Representation;

import java.net.URI;

/**
 * Shown once the new password has been set.
 */
public final class SetPasswordPostRepresentation implements RepresentationFunction
{
    private static final Message MSG_HEADING = Message.ofKey("view.success.head1");
    private static final Message MSG_BACK_TO_LOGIN = Message.ofKey("view.back.to.login");

    @Override
    public Representation apply(RepresentationModel model, RepresentationFactory factory)
    {
        return factory.newAuthenticationStep(step -> {
            step.addMessage(MSG_HEADING, HaapiContract.MessageClasses.HEADING);
            step.addFormAction(HaapiContract.Actions.Kinds.CONTINUE, URI.create(model.getString("_authUrl")),
                    HttpMethod.GET, null, null, MSG_BACK_TO_LOGIN);
        });
    }
}
