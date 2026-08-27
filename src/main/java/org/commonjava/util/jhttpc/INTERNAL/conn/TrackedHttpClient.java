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
package org.commonjava.util.jhttpc.INTERNAL.conn;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.commonjava.util.jhttpc.INTERNAL.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdcasey on 10/28/15.
 */
public class TrackedHttpClient
        extends CloseableHttpClient
{
    private final CloseableHttpClient delegate;

    private final ConnectionManagerTracker managerWrapper;

    private Set<WeakReference<HttpRequest>> requests = new HashSet<WeakReference<HttpRequest>>();

    private Set<WeakReference<CloseableHttpResponse>> responses = new HashSet<WeakReference<CloseableHttpResponse>>();

    public TrackedHttpClient( CloseableHttpClient delegate, ConnectionManagerTracker managerWrapper )
    {
        this.delegate = delegate;
        this.managerWrapper = managerWrapper;
    }

    @Override
    protected CloseableHttpResponse doExecute( HttpHost target, HttpRequest request, HttpContext context )
            throws IOException, ClientProtocolException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Tracking request/response" );
        requests.add( new WeakReference<HttpRequest>( request ) );

        CloseableHttpResponse response = delegate.execute( target, request, context );
        responses.add( new WeakReference<CloseableHttpResponse>( response ) );

        return response;
    }

//    @Override
//    public CloseableHttpResponse execute( final HttpHost target, final HttpRequest request, final HttpContext context )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( target, request, context );
//    }
//
//    @Override
//    public CloseableHttpResponse execute( final HttpUriRequest request, final HttpContext context )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( request, context );
//    }
//
//    @Override
//    public CloseableHttpResponse execute( final HttpUriRequest request )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( request );
//    }
//
//    @Override
//    public CloseableHttpResponse execute( final HttpHost target, final HttpRequest request )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( target, request );
//    }
//
//    @Override
//    public <T> T execute( final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( request, responseHandler );
//    }
//
//    @Override
//    public <T> T execute( final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler,
//                          final HttpContext context )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( request, responseHandler, context );
//    }
//
//    @Override
//    public <T> T execute( final HttpHost target, final HttpRequest request,
//                          final ResponseHandler<? extends T> responseHandler )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( target, request, responseHandler );
//    }
//
//    @Override
//    public <T> T execute( final HttpHost target, final HttpRequest request,
//                          final ResponseHandler<? extends T> responseHandler, final HttpContext context )
//            throws IOException, ClientProtocolException
//    {
//        return delegate.execute( target, request, responseHandler, context );
//    }

    @Override
    public void close()
            throws IOException
    {
        HttpUtils.cleanupResources( delegate, requests, responses );
        if ( managerWrapper != null )
        {
            managerWrapper.release();
        }
        delegate.close();
    }

    @Override
    @Deprecated
    public HttpParams getParams()
    {
        return delegate.getParams();
    }

    @Override
    @Deprecated
    public ClientConnectionManager getConnectionManager()
    {
        return delegate.getConnectionManager();
    }
}
