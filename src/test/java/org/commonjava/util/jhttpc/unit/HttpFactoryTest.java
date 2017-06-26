/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.INTERNAL.util.CertEnumerator;
import org.commonjava.util.jhttpc.INTERNAL.util.SSLUtils;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

/**
 * Created by jdcasey on 10/28/15.
 */
public class HttpFactoryTest
{

    private ExpectationServer server;

    private HttpFactory factory;

    private PasswordManager passwordManager;

    @Before
    public void setup()
            throws Exception
    {
        server = new ExpectationServer();
        server.start();

        passwordManager = new MemoryPasswordManager();
        factory = new HttpFactory( passwordManager );
    }

    @Test
    public void simpleSingleGet_NoSiteConfig()
            throws Exception
    {
        String path = "/path/to/test";
        String content = "This is a test.";

        server.expect( server.formatUrl( path ), 200, content );

        HttpFactory factory = new HttpFactory( new MemoryPasswordManager() );
        CloseableHttpClient client = null;
        try
        {
            client = factory.createClient();
            String result = client.execute( new HttpGet( server.formatUrl( path ) ), new ResponseHandler<String>()
            {
                @Override
                public String handleResponse( HttpResponse response )
                        throws ClientProtocolException, IOException
                {
                    assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
                    return IOUtils.toString( response.getEntity().getContent() );
                }
            } );

            assertThat( result, equalTo( content ) );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    @Test
    @Ignore( "This is a diagnostic test for the environment, mainly for use with the docker-driven test below")
    public void checkKeyStrength()
            throws NoSuchAlgorithmException
    {
        int allowedKeyLength = 0;

        assertThat( "The allowed key length for AES is: " + allowedKeyLength + ", should be > 128!", Cipher.getMaxAllowedKeyLength( "AES" ) > 128, equalTo( true ) );

        System.out.println("The allowed key length for AES is: " + allowedKeyLength);    }

    @Test
    @Ignore( "This is for debugging problems that crop up in the functional test suite." )
    public void simpleSSLGet_Manual()
            throws Exception
    {
        String host = "172.17.1.69";
        String sitePath = "ssl-config/site.crt";
        String siteDownloadUrl = "http://" + host + "/" + sitePath;
        String clientPath = "ssl-config/client.pem";
        String clientDownloadUrl = "http://" + host + "/" + clientPath;
        CloseableHttpClient client = null;

        String serverCertPem = null;
        String clientCertPem = null;
        try
        {
            client = factory.createClient();
            HttpGet get = new HttpGet( siteDownloadUrl );
            CloseableHttpResponse response = client.execute( get );
            if ( response.getStatusLine().getStatusCode() == 200 )
            {
                serverCertPem = IOUtils.toString( response.getEntity().getContent() );
            }

            get = new HttpGet( clientDownloadUrl );
            response = client.execute( get );
            if ( response.getStatusLine().getStatusCode() == 200 )
            {
                clientCertPem = IOUtils.toString( response.getEntity().getContent() );
            }
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }

        assertThat( serverCertPem, notNullValue() );
        assertThat( clientCertPem, notNullValue() );

        //                "-----BEGIN CERTIFICATE-----\n" + "MIIEOTCCAyGgAwIBAgIJAPRXgKBSJE68MA0GCSqGSIb3DQEBCwUAMIGQMQswCQYD\n"
        //                        + "VQQGEwJVUzEPMA0GA1UECAwGS2Fuc2FzMRMwEQYDVQQHDApTbWFsbHZpbGxlMQ0w\n"
        //                        + "CwYDVQQKDARUZXN0MQwwCgYDVQQLDANXZWIxFjAUBgNVBAMMDXRlc3QubXljby5j\n"
        //                        + "b20xJjAkBgkqhkiG9w0BCQEWF3NpdGVhZG1pbkB0ZXN0Lm15Y28uY29tMB4XDTE1\n"
        //                        + "MTEwMjIxNTE1OFoXDTE2MTEwMTIxNTE1OFowgZAxCzAJBgNVBAYTAlVTMQ8wDQYD\n"
        //                        + "VQQIDAZLYW5zYXMxEzARBgNVBAcMClNtYWxsdmlsbGUxDTALBgNVBAoMBFRlc3Qx\n"
        //                        + "DDAKBgNVBAsMA1dlYjEWMBQGA1UEAwwNdGVzdC5teWNvLmNvbTEmMCQGCSqGSIb3\n"
        //                        + "DQEJARYXc2l0ZWFkbWluQHRlc3QubXljby5jb20wggEiMA0GCSqGSIb3DQEBAQUA\n"
        //                        + "A4IBDwAwggEKAoIBAQCpvPafp14tr0y724/y+4nlo8a18niCxe7X2kZ6lJfdMaJ0\n"
        //                        + "0xCyhhw1/jiiQtHon/1zS1PSc7152QXlWA90YqnVZpPAPINLekqXG2Wh7kcxRhJg\n"
        //                        + "vldd/Bek56l6dsPU2OrB8vFAvkM6vh9XRG/HizHk/iDOe0bwRFNIxYMi2FCFzXC7\n"
        //                        + "SRBDAixO98YXtMgIe2JEEysPgVBjCsOLZLzmlh1elVvtdAgQp6rjwEvxK9+usD66\n"
        //                        + "tbUghcAH6m5OD+m8cx6PUgBX5jHZuiupXnBNzhfbh9nPFwY76Pc3DHr9gsEyQStE\n"
        //                        + "a/As10V0/xSuWCilzVEdvFSlbs9lJkMrt4cVyLHtAgMBAAGjgZMwgZAwHQYDVR0O\n"
        //                        + "BBYEFEz6Wi94EBTh6jab9mcHu2iyPnE3MB8GA1UdIwQYMBaAFEz6Wi94EBTh6jab\n"
        //                        + "9mcHu2iyPnE3MAwGA1UdEwQFMAMBAf8wQAYDVR0RBDkwN4EXc2l0ZWFkbWluQHRl\n"
        //                        + "c3QubXljby5jb22HBKwRAEaCCWxvY2FsaG9zdIILMTcyLjE3LjAuNzAwDQYJKoZI\n"
        //                        + "hvcNAQELBQADggEBAELtpFGcd1OXNi3j8pTh24UiNAYiv9y/APOdyllr2YDxGK2h\n"
        //                        + "h0tEpsDXVIblVZppCcKwefkQLW81h39NjQOV6mCNgGlgSzDTdaYSkdBF2q2rtU6G\n"
        //                        + "LPy4ai4g2roccELCJ1QZ2s7EmJYenuUTwypieTMLOazopOsF3RdDYRqJZwWO5gGF\n"
        //                        + "zsVQ1O9RRzQJI5nTRTF/1R/cHSQBwyzPigACfbl7hWx7usyRS396fpvi/ulhQyy9\n"
        //                        + "Tzd0N6r3sr7d6cvXkqIHoImr0xk3Mww6BErm7BortT0OE+XEAPOQsfbrogEvCOV1\n"
        //                        + "yPKB1vtjssTVzCO10GiQO4D72NfPSuV3Q6QEMzk=\n" + "-----END CERTIFICATE-----";

        String baseUrl = "https://" + host + ":443/";
        String url = baseUrl + sitePath;
        SiteConfig config = new SiteConfigBuilder( "test", baseUrl )//.withTrustType( SiteTrustType.TRUST_SELF_SIGNED )
                                                                    .withServerCertPem( serverCertPem )
                                                                    .withKeyCertPem( clientCertPem )
                                                                    .build();

        passwordManager.bind( "test", config, PasswordType.KEY );

        client = null;
        try
        {
            client = factory.createClient( config );
            HttpGet get = new HttpGet( url );
            CloseableHttpResponse response = client.execute( get );
            System.out.println( response.getStatusLine() );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }

    }

}
