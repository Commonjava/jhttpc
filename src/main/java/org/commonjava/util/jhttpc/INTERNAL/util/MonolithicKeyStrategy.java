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
package org.commonjava.util.jhttpc.INTERNAL.util;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.Map;

/**
 * Created by jdcasey on 11/6/15.
 */
public class MonolithicKeyStrategy
        implements PrivateKeyStrategy
{
    public static final String KEY = "key";

    @Override
    public String chooseAlias( Map<String, PrivateKeyDetails> aliases, Socket socket )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace(
                "Returning hard-coded alias 'key' to coordinate with key/cert read from SiteConfig for socket: {}",
                socket.getInetAddress() );
        logger.trace("Map of available aliases: {}", aliases );

        return KEY;
    }
}
