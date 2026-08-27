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
package org.commonjava.util.jhttpc;

import org.apache.http.Header;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.util.jhttpc.lifecycle.ShutdownEnabled;
import org.commonjava.util.jhttpc.model.SiteConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Interface extracted from {@link HttpFactory} in order to allow wrappers, such as those that add tracing.
 */
public interface HttpFactoryIfc
                extends Closeable, ShutdownEnabled
{
    CloseableHttpClient createClient() throws JHttpCException;

    CloseableHttpClient createClient( SiteConfig location ) throws JHttpCException;

    CloseableHttpClient createClient( SiteConfig location, List<Header> defaultHeaders ) throws JHttpCException;

    HttpClientContext createContext() throws JHttpCException;

    HttpClientContext createContext( SiteConfig location ) throws JHttpCException;

    void close() throws IOException;

    @Override
    boolean isShutdown();

    @Override
    boolean shutdownNow();

    @Override
    boolean shutdownGracefully( long timeoutMillis ) throws InterruptedException;
}
