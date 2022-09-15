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

public class UpdatePasswordResult
{
    private UpdatePasswordResult()
    {
    }

    public static final class Success extends UpdatePasswordResult
    {
    }

    public static final class Weak extends UpdatePasswordResult
    {
        private final String _message;

        public Weak(String message)
        {
            _message = message;
        }

        public String getMessage()
        {
            return _message;
        }
    }

    public static final class InvalidAccount extends UpdatePasswordResult
    {
    }

    public static final class InvalidToken extends UpdatePasswordResult
    {
    }
}