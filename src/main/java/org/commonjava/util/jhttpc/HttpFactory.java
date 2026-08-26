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
package org.commonjava.util.jhttpc;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.commonjava.util.jhttpc.INTERNAL.conn.ConnectionManagerCache;
import org.commonjava.util.jhttpc.INTERNAL.conn.ConnectionManagerTracker;
import org.commonjava.util.jhttpc.INTERNAL.conn.SiteConnectionConfig;
import org.commonjava.util.jhttpc.INTERNAL.conn.TrackedHttpClient;
import org.commonjava.util.jhttpc.INTERNAL.util.CertEnumerator;
import org.commonjava.util.jhttpc.INTERNAL.util.MonolithicKeyStrategy;
import org.commonjava.util.jhttpc.INTERNAL.util.SSLUtils;
import org.commonjava.util.jhttpc.auth.BasicAuthenticator;
import org.commonjava.util.jhttpc.auth.ClientAuthenticator;
import org.commonjava.util.jhttpc.auth.PasswordKey;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class HttpFactory
        implements HttpFactoryIfc
{
    private static final String SSL_FACTORY_ATTRIB = "ssl-factory";

    private static final String COOKIE_STORE = "cookie-store";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final PasswordManager passwords;

    private final ClientAuthenticator authenticator;

    private final ConnectionManagerCache connectionCache;

    public HttpFactory( final PasswordManager passwords )
    {
        this.passwords = passwords;
        this.authenticator = new BasicAuthenticator( passwords );
        this.connectionCache = new ConnectionManagerCache();
    }

    public HttpFactory( final ClientAuthenticator authenticator )
    {
        this.authenticator = authenticator;
        this.passwords = null;
        this.connectionCache = new ConnectionManagerCache();
    }

    public PasswordManager getPasswordManager()
    {
        return passwords;
    }

    @Override
    public CloseableHttpClient createClient()
            throws JHttpCException
    {
        return createClient( null, null );
    }

    @Override
    public CloseableHttpClient createClient( final SiteConfig location )
                    throws JHttpCException
    {
        return createClient( location, null );
    }

    @Override
    public CloseableHttpClient createClient( final SiteConfig location, final List<Header> defaultHeaders )
            throws JHttpCException
    {
        CloseableHttpClient client;
        if ( location != null )
        {
            HttpClientBuilder builder = HttpClients.custom();

            if ( authenticator != null )
            {
                builder = authenticator.decorateClientBuilder( builder );
            }

            logger.debug( "Using site config: {} for advanced client options", location );
            SiteConnectionConfig connConfig = new SiteConnectionConfig( location );

            final SSLConnectionSocketFactory sslFac = createSSLSocketFactory( location );
            if ( sslFac != null )
            {
                //                HostnameVerifier verifier = new SSLHostnameVerifierImpl( );
                //                builder.setSSLHostnameVerifier( verifier );
                builder.setSSLSocketFactory( sslFac );
                connConfig.withSSLConnectionSocketFactory( sslFac );
            }

            ConnectionManagerTracker managerWrapper = connectionCache.getTrackerFor( connConfig );
            logger.debug( "Using the connection manager tracker: {}", managerWrapper );
            builder.setConnectionManager( managerWrapper.acquire() );

            if ( location.getProxyHost() != null )
            {
                logger.debug( "Plan to use proxy for connection: host: {}, port: {}", location.getProxyHost(),
                              location.getProxyPort() );
                final HttpRoutePlanner planner = new DefaultProxyRoutePlanner(
                        new HttpHost( location.getProxyHost(), getProxyPort( location ) ) );
                builder.setRoutePlanner( planner );
            }

            final int timeout = 1000 * location.getRequestTimeoutSeconds();
            logger.debug( "Request timeout value: {}", timeout );
            builder.setDefaultRequestConfig( RequestConfig.custom()
//                                                          .setConnectionRequestTimeout( timeout )
                                                          .setSocketTimeout( timeout )
                                                          .setConnectTimeout( timeout )
                                                          .build() );

            if ( defaultHeaders != null )
            {
                builder.setDefaultHeaders( defaultHeaders );
            }

            client = new TrackedHttpClient( builder.build(), managerWrapper );
        }
        else
        {
            client = HttpClients.createDefault();
        }

        return client;
    }

    private int getProxyPort( final SiteConfig location )
    {
        int port = location.getProxyPort();
        if ( port < 1 )
        {
            port = -1;
        }

        return port;
    }

    @Override
    public HttpClientContext createContext()
            throws JHttpCException
    {
        return createContext( null );
    }

    @Override
    public HttpClientContext createContext( final SiteConfig location )
            throws JHttpCException
    {
        HttpClientContext ctx = HttpClientContext.create();

        if ( location != null )
        {
            logger.debug( "Configuring HttpClientContext from SiteConfig" );
            HttpClientContext prototype = location.getClientContextPrototype();
            if ( prototype != null )
            {
                logger.debug( "Configuring HttpClientContext based on prototype instance" );
                copyContextPrototype( prototype, ctx );
            }

            RequestConfig reqConf = location.getRequestConfig();
            if ( reqConf == null )
            {
                logger.debug( "Configuring HttpClientContext RequestConfig with connection-pool timeout: {}",
                              location.getConnectionPoolTimeoutSeconds() );

                reqConf = RequestConfig.copy( RequestConfig.DEFAULT )
                             .setConnectionRequestTimeout(
                                     location.getConnectionPoolTimeoutSeconds() )
                             .build();
            }
            else
            {
                logger.debug( "Configuring HttpClientContext with SiteLocation's RequestConfig instance" );
                ctx.setRequestConfig( RequestConfig.copy( reqConf ).build() );
            }


            CookieStore cookieStore = (CookieStore) location.getAttribute( COOKIE_STORE );
            if ( cookieStore == null )
            {
                logger.debug( "Creating CookieStore" );
                cookieStore = new BasicCookieStore();
                location.setAttribute( COOKIE_STORE, cookieStore );
            }

            logger.debug( "Setting CookieStore" );
            ctx.setCookieStore( cookieStore );

            final AuthScope as;
            try
            {
                as = new AuthScope( location.getHost(), location.getPort() );
            }
            catch ( MalformedURLException e )
            {
                throw new JHttpCException( "Failed to parse site URL for host and port: %s (site id: %s). Reason: %s",
                                           e, location.getUri(), location.getId(), e.getMessage() );
            }

            if ( location.getUser() != null )
            {
                if ( authenticator != null )
                {
                    logger.debug( "Setting up authentication from SiteConfig's user + authenticator: {}",
                                  authenticator );

                    ctx = authenticator.decoratePrototypeContext( as, location, PasswordType.USER, ctx );
                }
            }

            if ( location.getProxyHost() != null && location.getProxyUser() != null )
            {
                if ( authenticator != null )
                {
                    logger.debug( "Setting up proxy authentication from SiteConfig's proxy user + authenticator: {}",
                                  authenticator );

                    ctx = authenticator.decoratePrototypeContext(
                            new AuthScope( location.getProxyHost(), getProxyPort( location ) ), location,
                            PasswordType.PROXY, ctx );
                }
            }
        }

        return ctx;
    }

    private void copyContextPrototype( final HttpClientContext prototype, final HttpClientContext target )
    {
        target.setRequestConfig( prototype.getRequestConfig() );
        target.setAuthCache( prototype.getAuthCache() );
        target.setAuthSchemeRegistry( prototype.getAuthSchemeRegistry() );
        target.setCookieSpecRegistry( prototype.getCookieSpecRegistry() );
        target.setCookieStore( prototype.getCookieStore() );
        target.setCredentialsProvider( prototype.getCredentialsProvider() );
        target.setUserToken( prototype.getUserToken() );
    }

    private SSLConnectionSocketFactory createSSLSocketFactory( final SiteConfig location )
            throws JHttpCException
    {
        SSLConnectionSocketFactory fac = (SSLConnectionSocketFactory) location.getAttribute( SSL_FACTORY_ATTRIB );
        if ( fac != null )
        {
            return fac;
        }

        KeyStore ks = null;
        KeyStore ts = null;

        final String kcPem = location.getKeyCertPem();

        final String kcPass = passwords == null ? null : passwords.lookup( new PasswordKey( location, PasswordType.KEY ) );
        if ( kcPem != null )
        {
            logger.debug( "Adding client key/certificate from: {}", location );
            if ( kcPass == null || kcPass.length() < 1 )
            {
                logger.error( "Invalid configuration. Location: {} cannot have an empty key password!",
                              location.getUri() );
                throw new JHttpCException(
                        "Location: " + location.getUri() + " is misconfigured! Key password cannot be empty." );
            }

            try
            {
                logger.trace( "Reading Client SSL key from:\n\n{}\n\n", kcPem );
                ks = SSLUtils.readKeyAndCert( kcPem, kcPass );

                logger.trace( "Keystore contains the following certificates: {}", new CertEnumerator( ks, kcPass ) );
            }
            catch ( final CertificateException e )
            {
                logger.error( String.format(
                        "Invalid configuration. Location: %s has an invalid client certificate! Error: %s",
                        location.getUri(), e.getMessage() ), e );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final KeyStoreException e )
            {
                logger.error( String.format(
                        "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                        location.getUri(), e.getMessage() ), e );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final NoSuchAlgorithmException e )
            {
                logger.error( String.format(
                        "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                        location.getUri(), e.getMessage() ), e );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final InvalidKeySpecException e )
            {
                logger.error( String.format( "Invalid configuration. Invalid client key for repository: %s. Error: %s",
                                             location.getUri(), e.getMessage() ), e );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( IOException e )
            {
                throw new JHttpCException( "Failed to read client SSL key/certificate from: %s. Reason: %s", e,
                                           location, e.getMessage() );
            }
            catch ( JHttpCException e )
            {
                throw new JHttpCException( "Failed to read client SSL key/certificate from: %s. Reason: %s", e,
                                           location, e.getMessage() );
            }
        }
        else
        {
            logger.debug( "No client key/certificate found" );
        }

        final String sPem = location.getServerCertPem();

        //        logger.debug( "Server certificate PEM:\n{}", sPem );
        if ( sPem != null )
        {
            logger.debug( "Loading TrustStore (server SSL) information from: {}", location );
            try
            {
                logger.trace( "Reading Server SSL cert from:\n\n{}\n\n", sPem );
                ts = SSLUtils.decodePEMTrustStore( sPem, location.getHost() );

                logger.trace( "Trust store contains the following certificates:\n{}", new CertEnumerator( ts, null ) );
            }
            catch ( final CertificateException e )
            {
                logger.error( String.format(
                        "Invalid configuration. Location: %s has an invalid server certificate! Error: %s",
                        location.getUri(), e.getMessage() ), e );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final KeyStoreException e )
            {
                logger.error( String.format(
                        "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                        location.getUri(), e.getMessage() ), e );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final NoSuchAlgorithmException e )
            {
                logger.error( String.format(
                        "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s",
                        location.getUri(), e.getMessage() ), e );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( IOException e )
            {
                throw new JHttpCException(
                        "Failed to read server SSL certificate(s) (or couldn't parse server hostname) from: %s. Reason: %s",
                        e, location, e.getMessage() );
            }
        }
        else
        {
            logger.debug( "No server certificates found" );
        }

        // if user set either ks, ts, or want to ignore hostname verification, we know this is a ssl factory and set it accordingly
        if ( ks != null || ts != null || location.isIgnoreHostnameVerification() )
        {
            logger.debug( "Setting up SSL context." );
            try
            {
                SSLContextBuilder sslBuilder = SSLContexts.custom().setProtocol( SSLConnectionSocketFactory.TLS );

                if ( ks != null )
                {
                    logger.trace( "Loading key material for SSL context..." );
                    PrivateKeyStrategy pkStrategy = new MonolithicKeyStrategy();
                    sslBuilder.loadKeyMaterial( ks, kcPass.toCharArray(), pkStrategy );
                }

                if ( ts != null )
                {
                    logger.trace( "Loading trust material for SSL context..." );

                    SiteTrustType trustType = location.getTrustType();
                    if ( trustType == null )
                    {
                        trustType = SiteTrustType.DEFAULT;
                    }

                    sslBuilder.loadTrustMaterial( ts, trustType.getTrustStrategy() );
                }

                SSLContext ctx = sslBuilder.build();
                if ( location.isIgnoreHostnameVerification() )
                {
                    fac = new SSLConnectionSocketFactory( ctx, new NoopHostnameVerifier() );
                }
                else
                {
                    fac = new SSLConnectionSocketFactory( ctx, new DefaultHostnameVerifier() );
                }

                location.setAttribute( SSL_FACTORY_ATTRIB, fac );
                return fac;
            }
            catch ( final KeyManagementException e )
            {
                logger.error(
                        "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e,
                        location.getUri(), e.getMessage() );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final UnrecoverableKeyException e )
            {
                logger.error(
                        "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e,
                        location.getUri(), e.getMessage() );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final NoSuchAlgorithmException e )
            {
                logger.error(
                        "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e,
                        location.getUri(), e.getMessage() );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
            catch ( final KeyStoreException e )
            {
                logger.error(
                        "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e,
                        location.getUri(), e.getMessage() );
                throw new JHttpCException( "Failed to initialize SSL connection for repository: " + location.getUri() );
            }
        }
        else
        {
            logger.debug( "No SSL configuration present; no SSL context created." );
        }

        return null;
    }

    @Override
    public void close()
            throws IOException
    {
        //        connectionManager.reallyShutdown();
    }

    @Override
    public boolean isShutdown()
    {
        return connectionCache.isShutdown();
    }

    @Override
    public boolean shutdownNow()
    {
        return connectionCache.shutdownNow();
    }

    @Override
    public boolean shutdownGracefully( final long timeoutMillis )
            throws InterruptedException
    {
        return connectionCache.shutdownGracefully( timeoutMillis );
    }
}
