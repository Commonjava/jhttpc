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

import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.commonjava.util.jhttpc.model.SiteConfig;

/**
 * Created by jdcasey on 11/3/15.
 */
public class SiteConnectionConfig
{
    private final SiteConfig config;

    private PlainConnectionSocketFactory httpFactory = PlainConnectionSocketFactory.getSocketFactory();

    private SSLConnectionSocketFactory sslFactory = SSLConnectionSocketFactory.getSocketFactory();

    public SiteConnectionConfig( SiteConfig config )
    {

        this.config = config;
    }

    public SiteConnectionConfig withSSLConnectionSocketFactory( SSLConnectionSocketFactory factory )
    {
        if ( factory != null )
        {
            this.sslFactory = factory;
        }

        return this;
    }

    public Registry<ConnectionSocketFactory> getSocketFactoryRegistry()
    {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                              .register( "http", httpFactory )
                              .register( "https", sslFactory )
                              .build();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof SiteConnectionConfig ) )
        {
            return false;
        }

        SiteConnectionConfig that = (SiteConnectionConfig) o;

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
        return "SiteConnectionConfig{" +
                "config=" + config +
                ", sslFactory=" + sslFactory +
                '}';
    }

    public SiteConfig getConfig()
    {
        return config;
    }

    public int getMaxConnections()
    {
        return config.getMaxConnections();
    }

    public int getMaxPerRoute()
    {
        return config.getMaxPerRoute();
    }

    public int getConnectionPoolTimeoutSeconds()
    {
        return config.getConnectionPoolTimeoutSeconds();
    }

    public ConnectionConfig getConnectionConfig()
    {
        return config.getConnectionConfig();
    }

    public SocketConfig getSocketConfig()
    {
        return config.getSocketConfig();
    }

    public String getId()
    {
        return config.getId();
    }
}
