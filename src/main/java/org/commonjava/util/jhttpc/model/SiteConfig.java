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
package org.commonjava.util.jhttpc.model;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 10/28/15.
 */
public final class SiteConfig
{

    // TODO: too low?
    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 10;

    public static final int DEFAULT_PROXY_PORT = 8080;

    public static final int DEFAULT_MAX_CONNECTIONS = 4;

    public static final int DEFAULT_CONNECTION_POOL_TIMEOUT_SECONDS = 60;

    private final String id;

    private final String uri;

    private final String user;

    private final String proxyHost;

    private final Integer proxyPort;

    private final String proxyUser;

    private final String proxyAllowHttpJobTypes;

    private final String egressSites;

    private final SiteTrustType trustType;

    private final String keyCertPem;

    private final String serverCertPem;

    private final ConnectionConfig connectionConfig;

    private final SocketConfig socketConfig;

    private Map<String, Object> attributes;

    private final Integer requestTimeoutSeconds;

    private final Integer maxConnections;

    private final Integer connectionPoolTimeoutSeconds;

    private Integer maxPerRoute;

    private RequestConfig requestConfig;

    private HttpClientContext clientContextPrototype;

    private final boolean ignoreHostnameVerification;

    private final Boolean metricEnabled;

    private final String honeycombDataset;

    private final String honeycombWriteKey;

    private final Integer baseSampleRate;

    SiteConfig( String id, String uri, String user, String proxyHost, Integer proxyPort, String proxyUser,
                String proxyAllowHttpJobTypes, String egressSites, SiteTrustType trustType, String keyCertPem,
                String serverCertPem, Integer requestTimeoutSeconds, Integer connectionPoolTimeoutSeconds,
                Integer maxConnections, Integer maxPerRoute, final ConnectionConfig connectionConfig,
                final SocketConfig socketConfig, final RequestConfig requestConfig,
                HttpClientContext clientContextPrototype, boolean ignoreHostnameVerification,
                Map<String, Object> attributes, Boolean metricEnabled, String honeycombDataset,
                String honeycombWriteKey, Integer baseSampleRate )
    {
        this.id = id;
        this.uri = uri;
        this.user = user;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyAllowHttpJobTypes = proxyAllowHttpJobTypes;
        this.egressSites = egressSites;
        this.trustType = trustType;
        this.keyCertPem = keyCertPem;
        this.serverCertPem = serverCertPem;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.connectionPoolTimeoutSeconds = connectionPoolTimeoutSeconds;
        this.maxConnections = maxConnections;
        this.maxPerRoute = maxPerRoute;
        this.connectionConfig = connectionConfig;
        this.socketConfig = socketConfig;
        this.requestConfig = requestConfig;
        this.clientContextPrototype = clientContextPrototype;
        this.ignoreHostnameVerification = ignoreHostnameVerification;
        this.attributes = attributes == null ? new HashMap<String, Object>() : attributes;
        this.metricEnabled = metricEnabled;
        this.honeycombDataset = honeycombDataset;
        this.honeycombWriteKey = honeycombWriteKey;
        this.baseSampleRate = baseSampleRate;

    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public String getId()
    {
        return id;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort == null ? DEFAULT_PROXY_PORT : proxyPort;
    }

    public String getUser()
    {
        return user;
    }

    public String getHost()
            throws MalformedURLException
    {
        return new URL( getUri() ).getHost();
    }

    public int getPort()
            throws MalformedURLException
    {
        URL u = new URL( getUri() );
        int port = u.getPort();
        if ( port < 1 )
        {
            port = u.getDefaultPort();
        }

        return port;
    }

    public String getProxyUser()
    {
        return proxyUser;
    }

    public String getProxyAllowHttpJobTypes()
    {
        return proxyAllowHttpJobTypes;
    }

    public String getEgressSites()
    {
        return egressSites;
    }

    public String getKeyCertPem()
    {
        return keyCertPem;
    }

    public String getUri()
    {
        return uri;
    }

    public String getServerCertPem()
    {
        return serverCertPem;
    }

    public int getConnectionPoolTimeoutSeconds()
    {
        return connectionPoolTimeoutSeconds == null ? DEFAULT_CONNECTION_POOL_TIMEOUT_SECONDS : connectionPoolTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds()
    {
        return requestTimeoutSeconds == null ? DEFAULT_REQUEST_TIMEOUT_SECONDS : requestTimeoutSeconds;
    }

    public <T> T getAttribute( String key, Class<T> type )
    {
        Object value = getAttribute( key );
        return value == null ? null : type.cast( value );
    }

    public <T> T getAttribute( String key, Class<T> type, T defaultValue )
    {
        Object value = getAttribute( key );
        return value == null ? defaultValue : type.cast( value );
    }

    public synchronized Object setAttribute( String key, Object value )
    {
        if ( attributes == null )
        {
            attributes = new HashMap<String, Object>();
        }

        return attributes.put( key, value );
    }

    public Object getAttribute( String key )
    {
        return attributes == null ? null : attributes.get( key );
    }

    public int getMaxConnections()
    {
        return maxConnections == null ? DEFAULT_MAX_CONNECTIONS : maxConnections;
    }

    public int getMaxPerRoute()
    {
        return maxPerRoute == null ? getMaxConnections() : maxPerRoute;
    }

    public ConnectionConfig getConnectionConfig()
    {
        return connectionConfig;
    }

    public SocketConfig getSocketConfig()
    {
        return socketConfig;
    }

    public SiteTrustType getTrustType()
    {
        return trustType == null ? SiteTrustType.DEFAULT : trustType;
    }

    public Boolean isMetricEnabled() {
        return metricEnabled == null ? false : metricEnabled;
    }

    public String getHoneycombDataset() {
        return honeycombDataset;
    }

    public String getHoneycombWriteKey() {
        return honeycombWriteKey;
    }

    public Integer getBaseSampleRate() { return baseSampleRate; }

    public Object removeAttribute( String key )
    {
        if ( attributes != null )
        {
            return attributes.remove( key );
        }

        return null;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof SiteConfig ) )
        {
            return false;
        }

        SiteConfig that = (SiteConfig) o;

        return !( getId() != null ? !getId().equals( that.getId() ) : that.getId() != null );

    }

    @Override
    public int hashCode()
    {
        return 13 + (getId() != null ? getId().hashCode() : 0);
    }

    @Override
    public String toString()
    {
        return "SiteConfig{" +
                "id='" + id + '\'' +
                '}';
    }

    public RequestConfig getRequestConfig()
    {
        return requestConfig;
    }

    public HttpClientContext getClientContextPrototype()
    {
        return clientContextPrototype;
    }

    public boolean isIgnoreHostnameVerification()
    {
        return ignoreHostnameVerification;
    }

}
