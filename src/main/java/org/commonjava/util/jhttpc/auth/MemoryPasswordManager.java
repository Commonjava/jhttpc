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
package org.commonjava.util.jhttpc.auth;

import org.commonjava.util.jhttpc.model.SiteConfig;

import java.util.HashMap;
import java.util.Map;

public class MemoryPasswordManager
    implements PasswordManager
{

    private final Map<PasswordKey, String> passwords = new HashMap<PasswordKey, String>();

    @Override
    public void bind( final String password, final SiteConfig config, final PasswordType type )
    {
        passwords.put( new PasswordKey( config.getId(), type ), password );
    }

    @Override
    public void bind( final String password, final String siteId, final PasswordType type )
    {
        passwords.put( new PasswordKey( siteId, type ), password );
    }

    @Override
    public void bind( final String password, final PasswordKey id )
    {
        passwords.put( id, password );
    }

    @Override
    public void unbind( SiteConfig config, PasswordType type )
    {
        passwords.remove( new PasswordKey( config.getId(), type ) );
    }

    @Override
    public void unbind( String siteId, PasswordType type )
    {
        passwords.remove( new PasswordKey( siteId, type ) );
    }

    @Override
    public void unbind( PasswordKey id )
    {
        passwords.remove( id );
    }

    @Override
    public String lookup( final PasswordKey id )
    {
        return passwords.get( id );
    }

}
