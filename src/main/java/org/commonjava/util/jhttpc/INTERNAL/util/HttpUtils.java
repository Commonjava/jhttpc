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
package org.commonjava.util.jhttpc.INTERNAL.util;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.AbstractExecutionAwareRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.lang.ref.WeakReference;
import java.util.Set;

import static org.apache.commons.io.IOUtils.closeQuietly;

public final class HttpUtils
{

    private HttpUtils()
    {
    }

    public static void cleanupResources( final CloseableHttpClient client, final HttpUriRequest request,
                                         final CloseableHttpResponse response )
    {
        if ( response != null && response.getEntity() != null )
        {
            EntityUtils.consumeQuietly( response.getEntity() );
            closeQuietly( response );
        }

        if ( request != null )
        {
            if ( request instanceof AbstractExecutionAwareRequest )
            {
                ( (AbstractExecutionAwareRequest) request ).reset();
            }
        }

        if ( client != null )
        {
            closeQuietly( client );
        }
    }

    public static void cleanupResources( CloseableHttpClient client, Set<WeakReference<HttpRequest>> requests,
                                         Set<WeakReference<CloseableHttpResponse>> responses )
    {
        if ( responses != null )
        {
            for ( WeakReference<CloseableHttpResponse> ref : responses )
            {
                if ( ref == null )
                {
                    continue;
                }

                CloseableHttpResponse response = ref.get();
                if ( response == null )
                {
                    continue;
                }

                if ( response.getEntity() != null )
                {
                    EntityUtils.consumeQuietly( response.getEntity() );
                    closeQuietly( response );
                }
            }
        }

        if ( requests != null )
        {
            for ( WeakReference<HttpRequest> ref : requests )
            {
                if ( ref == null )
                {
                    continue;
                }

                HttpRequest request = ref.get();
                if ( request == null )
                {
                    continue;
                }

                if ( request instanceof AbstractExecutionAwareRequest )
                {
                    ( (AbstractExecutionAwareRequest) request ).reset();
                }
            }
        }

        if ( client != null )
        {
            closeQuietly( client );
        }
    }
}
