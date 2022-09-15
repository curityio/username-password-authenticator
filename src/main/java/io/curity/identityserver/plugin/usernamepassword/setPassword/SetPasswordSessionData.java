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

package io.curity.identityserver.plugin.usernamepassword.setPassword;

import com.google.gson.Gson;
import io.curity.identityserver.plugin.usernamepassword.utils.StringUtils;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.service.SessionManager;

public final class SetPasswordSessionData
{
    private final SessionManager _sessionManager;
    private Data _data;

    public SetPasswordSessionData(SessionManager sessionManager)
    {
        _sessionManager = sessionManager;
        _data = null;
    }

    public void write(String token, String accountId)
    {
        Data data = new Data(token, accountId);
        String jsonData = new Gson().toJson(data);
        _sessionManager.put(Attribute.of("nonceData", jsonData));
    }

    public boolean hasToken(String token)
    {
        Attribute nonceData = _sessionManager.get("nonceData");
        if (nonceData != null) {

            _data = new Gson().fromJson(nonceData.getValue().toString(), Data.class);
            if (_data != null && StringUtils.isNotBlank(_data.token) && _data.token.equals(token)) {
                return true;
            }
        }

        return false;
    }

    public String readAccountId()
    {
        Attribute nonceData = _sessionManager.get("nonceData");
        if (nonceData != null) {

            _data = new Gson().fromJson(nonceData.getValue().toString(), Data.class);
            if (_data != null && StringUtils.isNotBlank(_data.accountId)) {
                return _data.accountId;
            }
        }

        return null;
    }

    public void remove()
    {
        _sessionManager.remove("nonceData");
    }

    private static class Data
    {
        public Data(String tokenInput, String accountIdInput)
        {
            token = tokenInput;
            accountId = accountIdInput;
        }

        public String token;
        public String accountId;
    }
}
