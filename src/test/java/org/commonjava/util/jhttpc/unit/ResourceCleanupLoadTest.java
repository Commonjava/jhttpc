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
package org.commonjava.util.jhttpc.unit;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.test.http.expect.ExpectationHandler;
import org.commonjava.test.http.junit4.expect.ExpectationServerWrapper;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jdcasey on 5/30/17.
 */
public class ResourceCleanupLoadTest
{

    private static final int THREAD_COUNT = 20;

    @Rule
    public ExpectationServerWrapper server = new ExpectationServerWrapper();

    @Rule
    public TestName name = new TestName();

    private ExecutorService executor;

    private HttpFactory factory;

    @Before
    public void setup()
            throws Exception
    {
        executor = Executors.newFixedThreadPool( THREAD_COUNT );
        factory = new HttpFactory( new MemoryPasswordManager() );
    }

    @Test
    public void get20Times_RangedResponses()
            throws Exception
    {
        final String path = name.getMethodName() + "/path/to/test";
        final String content = "This is a test.";
        final SiteConfig config = new SiteConfigBuilder( "test", server.formatUrl() ).withMaxConnections( 1 ).build();

        final AtomicInteger ai = new AtomicInteger( 0 );
        server.expect( "GET", server.formatUrl( path ), new ExpectationHandler()
        {
            @Override
            public void handle( final HttpServletRequest httpServletRequest,
                                final HttpServletResponse httpServletResponse )
                    throws ServletException, IOException
            {
                int id = ai.incrementAndGet();
                int code = ((id % 4) * 100 + 100 + (id % 5));
                httpServletResponse.setStatus( code );
                httpServletResponse.flushBuffer();
            }
        } );

        final CountDownLatch latch = new CountDownLatch( THREAD_COUNT );

        final AtomicBoolean timedOut = new AtomicBoolean( false );
        for( int i=0; i<THREAD_COUNT; i++)
        {
            final int idx = i;
            executor.execute( new Runnable(){
                public void run(){
                    String name = Thread.currentThread().getName();
                    Thread.currentThread().setName( "GET#" + idx );
                    CloseableHttpClient client = null;
                    try
                    {
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.info( "GET: {}", path );
                        client = factory.createClient( config );
                        logger.info( "Got client: {}", client );
                        CloseableHttpResponse response = client.execute( new HttpGet( server.formatUrl( path ) ) );

                        logger.info( "Response: {}", response.getStatusLine() );

//                        assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
//                        String result = IOUtils.toString( response.getEntity().getContent() );

//                        assertThat( result, equalTo( content ) );
                    }
                    catch ( ConnectionPoolTimeoutException e )
                    {
                        e.printStackTrace();
                        timedOut.set( true );
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        IOUtils.closeQuietly( client );
                        Thread.currentThread().setName( name );
                        latch.countDown();
                    }
                }
            } );
        }

        latch.await();
        assertThat( "Connection pool timed out!", timedOut.get(), equalTo( false ) );
    }

    @Test
    public void getReadAndClose20Times()
            throws Exception
    {
        final String path = name.getMethodName() + "/path/to/test";
        final String content = "This is a test.";
        final SiteConfig config = new SiteConfigBuilder( "test", server.formatUrl() ).withMaxConnections( 1 ).build();

        server.expect( "GET", server.formatUrl( path ), 200, content );

        final CountDownLatch latch = new CountDownLatch( THREAD_COUNT );

        final AtomicBoolean timedOut = new AtomicBoolean( false );
        for( int i=0; i<THREAD_COUNT; i++)
        {
            final int idx = i;
            executor.execute( new Runnable(){
                public void run(){
                    String name = Thread.currentThread().getName();
                    Thread.currentThread().setName( "GET#" + idx );
                    CloseableHttpClient client = null;
                    try
                    {
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.info( "GET: {}", path );
                        client = factory.createClient( config );
                        logger.info( "Got client: {}", client );
                        CloseableHttpResponse response = client.execute( new HttpGet( server.formatUrl( path ) ) );

                        logger.info( "Response: {}", response.getStatusLine() );

                        assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
                        String result = IOUtils.toString( response.getEntity().getContent(), Charset.defaultCharset() );

                        assertThat( result, equalTo( content ) );
                    }
                    catch ( ConnectionPoolTimeoutException e )
                    {
                        e.printStackTrace();
                        timedOut.set( true );
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        IOUtils.closeQuietly( client );
                        Thread.currentThread().setName( name );
                        latch.countDown();
                    }
                }
            } );
        }

        latch.await();
        assertThat( "Connection pool timed out!", timedOut.get(), equalTo( false ) );
    }

}
