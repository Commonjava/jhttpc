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

import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CloseBlockingConnectionManager
        implements HttpClientConnectionManager, Closeable
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final SiteConnectionConfig config;

    private final HttpClientConnectionManager connectionManager;

    public CloseBlockingConnectionManager( final SiteConnectionConfig config, final HttpClientConnectionManager connectionManager )
    {
        this.config = config;
        this.connectionManager = connectionManager;
    }

    @Override
    public ConnectionRequest requestConnection( final HttpRoute route, final Object state )
    {
        logger.trace( "Requesting connection to: {} with state: {}",
                      route, state );
        ConnectionRequest request = connectionManager.requestConnection( route, state );
        logger.trace( "Connection request is: {}",
                      request );

        return request;
    }

    @Override
    public void releaseConnection( final HttpClientConnection conn, final Object newState, final long validDuration,
                                   final TimeUnit timeUnit )
    {
        logger.trace( "Releasing connection: {} with new state: {}", conn, newState );
        connectionManager.releaseConnection( conn, newState, validDuration, timeUnit );
    }

    @Override
    public void connect( final HttpClientConnection conn, final HttpRoute route, final int connectTimeout,
                         final HttpContext context )
            throws IOException
    {
        logger.trace( "Connecting: {} via route: {}", conn, route );
        connectionManager.connect( conn, route, connectTimeout, context );
    }

    @Override
    public void upgrade( final HttpClientConnection conn, final HttpRoute route, final HttpContext context )
            throws IOException
    {
        logger.trace( "Upgrading: {} via route: {}", conn, route );
        connectionManager.upgrade( conn, route, context );
    }

    @Override
    public void routeComplete( final HttpClientConnection conn, final HttpRoute route, final HttpContext context )
            throws IOException
    {
        logger.trace( "Route complete: {} with route: {}", conn, route );
        connectionManager.routeComplete( conn, route, context );
    }

    @Override
    public void closeIdleConnections( final long idletime, final TimeUnit tunit )
    {
        logger.trace( "Closing idle connections" );
        connectionManager.closeIdleConnections( idletime, tunit );
    }

    @Override
    public void closeExpiredConnections()
    {
        logger.trace( "Closing expired connections" );
        connectionManager.closeExpiredConnections();
    }

    @Override
    public void shutdown()
    {
        logger.trace( "BLOCKED connection-manager shutdown; connection pool is reusable.\n\n{}\n\n", this );
    }

    public void reallyShutdown()
    {
        logger.trace( "REALLY shutting down connection manager" );
        connectionManager.shutdown();
    }

    @Override
    public void close()
            throws IOException
    {
        reallyShutdown();
    }

    @Override
    public String toString()
    {
        return "CloseBlockingConnectionManager{" +
                "\nconfig=" + config +
                "\nconnectionManager=" + connectionManager +
                "\ninstance=" + super.hashCode() +
                "\n}";
    }
}
