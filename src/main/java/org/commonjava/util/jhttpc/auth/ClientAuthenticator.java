/*
 * Copyright (C) 2015-2024 Red Hat, Inc. (https://github.com/Commonjava/jhttpc)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.jhttpc.auth;

import org.apache.http.auth.AuthScope;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.util.jhttpc.JHttpCException;
import org.commonjava.util.jhttpc.model.SiteConfig;

import java.net.URL;

public abstract class ClientAuthenticator
{

    public HttpClientContext decoratePrototypeContext( final AuthScope scope, SiteConfig location, PasswordType type,
                                                       final HttpClientContext ctx )
            throws JHttpCException
    {
        return ctx;
    }

    public HttpClientBuilder decorateClientBuilder( final HttpClientBuilder builder )
            throws JHttpCException
    {
        return builder;
    }

}
