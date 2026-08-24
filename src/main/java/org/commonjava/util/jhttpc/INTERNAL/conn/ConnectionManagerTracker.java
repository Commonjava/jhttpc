/**
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

import org.apache.http.HttpResponse;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.SessionInputBuffer;
import org.commonjava.util.jhttpc.lifecycle.ShutdownEnabled;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdcasey on 11/3/15.
 */
public class ConnectionManagerTracker
        implements ShutdownEnabled
{
    private SiteConnectionConfig config;

    private ConnectionManagerCache managerCache;

    private CloseBlockingConnectionManager manager;
    private int users = 0;
    private boolean detached;
    private long lastRetrieval;

    public ConnectionManagerTracker( SiteConnectionConfig config, ConnectionManagerCache managerCache )
    {
        this.config = config;
        this.managerCache = managerCache;
    }

    public synchronized CloseBlockingConnectionManager acquire()
    {
        if ( manager == null )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "Creating connection pool for: {} with {} connections.", config.getId(),
                         config.getMaxConnections() );

//            ManagedHttpClientConnectionFactory fac =
//                    new ManagedHttpClientConnectionFactory( new BestEffortResponseParserFactory() );

            ManagedHttpClientConnectionFactory fac =
                    new ManagedHttpClientConnectionFactory( new ResponseParserFactory() );

            PoolingHttpClientConnectionManager poolingMgr =
                    new PoolingHttpClientConnectionManager( config.getSocketFactoryRegistry(), fac );

//            PoolingHttpClientConnectionManager poolingMgr =
//                    new PoolingHttpClientConnectionManager( config.getSocketFactoryRegistry() );

            poolingMgr.setMaxTotal( config.getMaxConnections() );
            poolingMgr.setDefaultMaxPerRoute( config.getMaxPerRoute() );

            ConnectionConfig connectionConfig = config.getConnectionConfig();
            if ( connectionConfig != null )
            {
                poolingMgr.setDefaultConnectionConfig( connectionConfig );
            }

            SocketConfig socketConfig = config.getSocketConfig();
            if ( socketConfig != null )
            {
                poolingMgr.setDefaultSocketConfig( socketConfig );
            }

            manager = new CloseBlockingConnectionManager( config, poolingMgr );
        }

        detached = false;
        users++;
        return manager;
    }

    public synchronized void release()
    {
        users--;
        tryShutdown();
    }

    private boolean tryShutdown()
    {
        if ( detached && !isActive() )
        {
            closeManager();
            managerCache.remove( config );
            return true;
        }

        return false;
    }

    private void closeManager()
    {
        if ( manager != null )
        {
            manager.reallyShutdown();
            manager = null;
        }
    }

    public long getLastRetrieval()
    {
        return lastRetrieval;
    }

    public ConnectionManagerTracker retrieved()
    {
        lastRetrieval = System.currentTimeMillis();
        return this;
    }

    public boolean detach()
    {
        this.detached = true;
        return tryShutdown();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ConnectionManagerTracker that = (ConnectionManagerTracker) o;

        return config.equals( that.config );

    }

    @Override
    public int hashCode()
    {
        return config.hashCode();
    }

    @Override
    public String toString()
    {
        return "ConnectionManagerTracker{" +
                "config=" + config +
                ", manager=" + manager + ", instance=" + super.hashCode() + '}';
    }

    public SiteConfig getSiteConfig()
    {
        return config.getConfig();
    }

    public SiteConnectionConfig getConnectionConfig()
    {
        return config;
    }

    @Override
    public boolean isShutdown()
    {
        return users < 1;
    }

    @Override
    public synchronized boolean shutdownNow()
    {
        closeManager();
        return true;
    }

    @Override
    public synchronized boolean shutdownGracefully( final long timeoutMillis )
            throws InterruptedException
    {
        long expires = System.currentTimeMillis() + timeoutMillis;
        while ( isActive() && System.currentTimeMillis() < expires )
        {
            Thread.sleep( 100 );
        }

        if ( !isActive() )
        {
            closeManager();
            return true;
        }

        return false;
    }

    public boolean isActive()
    {
        return users > 0;
    }

    private class ResponseParserFactory
            implements HttpMessageParserFactory<HttpResponse>
    {
        @Override
        public HttpMessageParser<HttpResponse> create( final SessionInputBuffer sessionInputBuffer,
                                                       final MessageConstraints messageConstraints )
        {
            return new DefaultHttpResponseParser( sessionInputBuffer, messageConstraints );
        }
    }
}
