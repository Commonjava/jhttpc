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
package org.commonjava.util.jhttpc.it;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.INTERNAL.util.SSLUtils;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.junit.Test;

import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractSSLTestsIT
        extends AbstractIT
{

    @Test
    public void clientSSLGet()
            throws Exception
    {
        String path = "/private/" + name.getMethodName() + "/path/to/test";
        String content = "This is a test.";

        putContent( path, content );

        SiteConfig config = getSiteConfigBuilder().withKeyCertPem( getClientKeyCertPem() ).build();
        passwordManager.bind( "test", config, PasswordType.KEY );

        CloseableHttpClient client = null;
        try
        {
            client = factory.createClient( config );
            CloseableHttpResponse response = client.execute( new HttpGet( formatSSLUrl( path ) ) );
            assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
            String result = IOUtils.toString( response.getEntity().getContent(), Charset.defaultCharset() );

            assertThat( result, equalTo( content ) );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    @Test
    public void simpleSSLGet()
            throws Exception
    {
        String path = name.getMethodName() + "/path/to/test";
        String content = "This is a test.";

        putContent( path, content );

        SiteConfig config = getSiteConfigBuilder().build();
        CloseableHttpClient client = null;
        try
        {
            client = factory.createClient( config );
            CloseableHttpResponse response = client.execute( new HttpGet( formatSSLUrl( path ) ) );
            assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
            String result = IOUtils.toString( response.getEntity().getContent(), Charset.defaultCharset() );

            assertThat( result, equalTo( content ) );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    @Test
    public void simpleSingleGet_NoSSL()
            throws Exception
    {
        String path = name.getMethodName() + "/path/to/test";
        String content = "This is a test.";

        putContent( path, content );

        CloseableHttpClient client = null;
        try
        {
            client = factory.createClient();
            CloseableHttpResponse response = client.execute( new HttpGet( formatUrl( path ) ) );
            assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
            String result = IOUtils.toString( response.getEntity().getContent(), Charset.defaultCharset() );

            assertThat( result, equalTo( content ) );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    @Test
    public void retrieveSiteCertificatePems()
            throws Exception
    {
        String[] paths = getCertificatePaths();

        CloseableHttpClient client = null;
        try
        {
            client = factory.createClient();
            for ( String path : paths )
            {
                CloseableHttpResponse response = client.execute( new HttpGet( formatUrl( path ) ) );
                assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
                String result = IOUtils.toString( response.getEntity().getContent(), Charset.defaultCharset() );

                System.out.println( result );
                assertThat( result, notNullValue() );
            }
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    @Test
    public void decodeSiteCertificatePems()
            throws Exception
    {
        String pem = getServerCertsPem();
        KeyStore store = SSLUtils.decodePEMTrustStore( pem, "somehost" );
        Enumeration<String> aliases = store.aliases();
        while ( aliases.hasMoreElements() )
        {
            System.out.println( aliases.nextElement() );
        }
    }

}
