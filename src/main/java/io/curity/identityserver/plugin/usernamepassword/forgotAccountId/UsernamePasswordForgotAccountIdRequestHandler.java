/*
 *  Copyright 2022 Curity AB
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

package io.curity.identityserver.plugin.usernamepassword.forgotAccountId;

import com.google.common.html.HtmlEscapers;
import io.curity.identityserver.plugin.usernamepassword.config.UsernamePasswordAuthenticatorPluginConfig;
import io.curity.identityserver.plugin.usernamepassword.descriptor.UsernamePasswordAuthenticatorPluginDescriptor;
import io.curity.identityserver.plugin.usernamepassword.utils.ViewModelReservedKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.data.email.Email;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.EmailSender;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static se.curity.identityserver.sdk.web.Response.ResponseModelScope.NOT_FAILURE;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

/**
 * Username/Password Authenticator forgot account ID handler.
 */
public final class UsernamePasswordForgotAccountIdRequestHandler implements AuthenticatorRequestHandler<RequestModel>
{
    /**
     * Create a new instance of UsernamePasswordForgotAccountIdRequestHandler using the configuration for this plugin.
     * <p>
     * The server knows how to provide an instance of this configuration because it is declared in the
     * plugin descriptor at {@link UsernamePasswordAuthenticatorPluginDescriptor#getConfigurationType()}.
     *
     * @param configuration for the Username/Password authenticator plugin
     */

    private final AccountManager _accountManager;
    private final UserPreferenceManager _userPreferenceManager;
    private final EmailSender _emailSender;
    private final Logger _logger;

    public UsernamePasswordForgotAccountIdRequestHandler(UsernamePasswordAuthenticatorPluginConfig configuration)
    {
        _logger = LoggerFactory.getLogger(UsernamePasswordForgotAccountIdRequestHandler.class);
        _accountManager = configuration.getAccountManager();
        _userPreferenceManager = configuration.getUserPreferenceManager();
        _emailSender = configuration.getEmailSender();
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        if (request.isPostRequest())
        {
            response.setResponseModel(templateResponseModel(emptyMap(),
                    "forgot-account-id/get"), HttpStatus.BAD_REQUEST);

            response.setResponseModel(templateResponseModel(emptyMap(),
                    "forgot-account-id/post"), NOT_FAILURE);
        }
        else
        {
            Map<String, Object> data = new HashMap<>(1);
            data.put(ViewModelReservedKeys.USERNAME, _userPreferenceManager.getUsername());

            response.setResponseModel(templateResponseModel(data,
                    "forgot-account-id/get"), NOT_FAILURE);
        }

        return new RequestModel(request);
    }

    @Override
    public Optional<AuthenticationResult> get(RequestModel requestModel, Response response)
    {
        return Optional.empty();
    }

    @Override
    public Optional<AuthenticationResult> post(RequestModel requestModel, Response response)
    {
        RequestModel.Post postModel = requestModel.getPostRequestModel();

        @Nullable AccountAttributes account = _accountManager.getByEmail(postModel.getPrimaryEmail());

        @Nullable String emailValue = AccountAttributes.emailFrom(account);

        if (account != null && emailValue != null)
        {
            response.putViewData(ViewModelReservedKeys.RECIPIENT_OF_COMMUNICATION, emailValue, Response.ResponseModelScope.ANY);

            Map<String, Object> model = new HashMap<>(1);
            model.put("accountId", account.getUserName());
            Email emailToSend = new Email(model);

            _emailSender.sendEmail(emailValue, emailToSend, "email/forgot-account-id/email");
        }
        else
        {
            // pretend the email was correctly sent to the input email address for security reasons
            response.putViewData(ViewModelReservedKeys.RECIPIENT_OF_COMMUNICATION, HtmlEscapers.htmlEscaper().escape(postModel.getPrimaryEmail()),
                    Response.ResponseModelScope.ANY);
        }

        response.setHttpStatus(HttpStatus.OK);

        return Optional.empty();
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errorMessages)
    {
        if (request.isPostRequest())
        {
            RequestModel.Post requestModel = new RequestModel.Post(request);

            Map<String, Object> data = new HashMap<>(1);
            data.put(ViewModelReservedKeys.FORM_POST_BACK, requestModel.dataOnError());

            // on POST validation failure, go back to the GET template
            response.setResponseModel(templateResponseModel(data,
                    "forgot-account-id/get"), HttpStatus.BAD_REQUEST);
        }
    }
}
