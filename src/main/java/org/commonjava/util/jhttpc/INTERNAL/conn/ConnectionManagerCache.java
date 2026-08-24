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

import org.commonjava.util.jhttpc.JHttpCException;
import org.commonjava.util.jhttpc.lifecycle.ShutdownEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by jdcasey on 11/3/15.
 */
public class ConnectionManagerCache
        implements ShutdownEnabled
{
    private static final long EXPIRATION_SECONDS = 30;

    private static final long EXPIRATION_MILLIS = TimeUnit.MILLISECONDS.convert( EXPIRATION_SECONDS, TimeUnit.SECONDS );

    private final Map<SiteConnectionConfig, ConnectionManagerTracker> cache =
            new HashMap<SiteConnectionConfig, ConnectionManagerTracker>();

    private final Timer timer = new Timer( "jhttpc-connection-manager-cache", true );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public ConnectionManagerCache()
    {
        timer.scheduleAtFixedRate( new ExpirationSweeper( this ), EXPIRATION_MILLIS, EXPIRATION_MILLIS );
    }

    public synchronized void expireTrackersOlderThan( long duration, TimeUnit unit )
    {
        long expiration = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert( duration, unit );

        for ( SiteConnectionConfig config : new HashSet<SiteConnectionConfig>( cache.keySet() ) )
        {
            ConnectionManagerTracker tracker = cache.get( config );
            if ( tracker != null && tracker.getLastRetrieval() < expiration )
            {
                if ( tracker.detach() )
                {
                    logger.trace( "Detached connection tracker from manager cache: {}", tracker );
                }
                else
                {
                    logger.trace( "Detaching did not result in shutdown for: {}. Try shutdownNow() to forcibly shutdown.", tracker );
                }
            }
        }
    }

    public synchronized ConnectionManagerTracker getTrackerFor( SiteConnectionConfig config )
            throws JHttpCException
    {
        ConnectionManagerTracker tracker = cache.get( config );
        if ( tracker == null )
        {
            tracker = new ConnectionManagerTracker( config, this );
            cache.put( config, tracker );
        }

        return tracker.retrieved();
    }

    @Override
    public synchronized boolean isShutdown()
    {
        return cache.values().stream().noneMatch( ConnectionManagerTracker::isActive );
    }

    @Override
    public boolean shutdownNow()
    {
        try
        {
            return doShutdown( (tracker) -> tracker.shutdownNow() );
        }
        catch ( InterruptedException e )
        {
            logger.warn( "Interrupted while shutting down connection manager cache." );
        }

        return false;
    }

    private synchronized boolean doShutdown( Function<ConnectionManagerTracker, Boolean> shutdownAction )
            throws InterruptedException
    {
        AtomicInteger counter = new AtomicInteger( 0 );
        ExecutorService exec = Executors.newCachedThreadPool( ( runnable ) -> {
            Thread t = new Thread( runnable );
            t.setDaemon( true );
            t.setName( "jHTTPc shutdown-" + ( counter.getAndIncrement() ) + ":" + Thread.currentThread().getName() );
            t.setPriority( 9 );

            return t;
        } );

        ExecutorCompletionService<Boolean> svc = new ExecutorCompletionService<>( exec );
        cache.forEach( ( config, tracker ) -> svc.submit( () -> shutdownAction.apply( tracker ) ) );

        boolean result = true;
        while ( counter.getAndDecrement() > 0 )
        {
            try
            {
                result = result && svc.take().get();
            }
            catch ( ExecutionException e )
            {
                logger.warn( "Error executing shutdown of connection managers." );
                result = false;
            }
        }

        timer.cancel();

        return result;
    }

    @Override
    public boolean shutdownGracefully( final long timeoutMillis )
            throws InterruptedException
    {
        AtomicBoolean interrupted = new AtomicBoolean( false );
        boolean result = doShutdown( ( tracker ) -> {
            try
            {
                return tracker.shutdownGracefully( timeoutMillis );
            }
            catch ( InterruptedException e )
            {
                interrupted.set( true );
            }

            return false;
        } );

        if ( interrupted.get() )
        {
            throw new InterruptedException();
        }

        return result;
    }

    synchronized void remove( final SiteConnectionConfig config )
    {
        cache.remove( config );
    }

    static final class ExpirationSweeper
            extends TimerTask
    {

        private ConnectionManagerCache cache;

        public ExpirationSweeper( ConnectionManagerCache cache )
        {
            this.cache = cache;
        }

        @Override
        public void run()
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Sweeping for old connection trackers." );
            cache.expireTrackersOlderThan( EXPIRATION_SECONDS, TimeUnit.SECONDS );
        }
    }
}
