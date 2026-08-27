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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributePasswordManager
        implements PasswordManager
{

    public static final String PASSWORD_PREFIX = "password_";

    private SiteConfigLookup lookupManager;

    public interface SiteConfigLookup
    {
        SiteConfig lookup( String siteId );
    }

    public AttributePasswordManager( SiteConfigLookup lookupManager )
    {
        this.lookupManager = lookupManager;
    }

    @Override
    public String lookup( final PasswordKey id )
    {
        SiteConfig config = lookupManager.lookup( id.getSiteId() );
        if ( config == null )
        {
            return null;
        }

        return config.getAttribute( PASSWORD_PREFIX + id.getPasswordType().name(), String.class );
    }

    public void bind( final String password, final SiteConfig loc, final PasswordType type )
    {
        if ( password == null )
        {
            return;
        }

        set( loc, type, password );
    }

    @Override
    public void bind( String password, String siteId, PasswordType type )
    {
        if ( password == null )
        {
            return;
        }

        SiteConfig config = getConfig( siteId );
        if ( config != null )
        {
            set( config, type, password );
        }
    }

    public void bind( final String password, final PasswordKey pwid )
    {
        if ( password == null )
        {
            return;
        }

        SiteConfig config = getConfig( pwid.getSiteId() );
        if ( config != null )
        {
            set( config, pwid.getPasswordType(), password );
        }
    }

    @Override
    public void unbind( SiteConfig config, PasswordType type )
    {
        clear( config, type );
    }

    @Override
    public void unbind( String siteId, PasswordType type )
    {
        SiteConfig config = getConfig( siteId );
        if ( config != null )
        {
            clear( config, type );
        }
    }

    @Override
    public void unbind( PasswordKey id )
    {
        SiteConfig config = getConfig( id.getSiteId() );
        if ( config != null )
        {
            clear( config, id.getPasswordType() );
        }
    }

    private SiteConfig getConfig( String siteId )
    {
        SiteConfig config = lookupManager.lookup( siteId );
        if ( config == null )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn( "Site config not found for: {}", siteId );
        }

        return config;
    }

    private void set( SiteConfig loc, PasswordType type, String password )
    {
        loc.setAttribute( PASSWORD_PREFIX + type.name(), password );
    }

    private void clear( SiteConfig loc, PasswordType type )
    {
        loc.removeAttribute( PASSWORD_PREFIX + type.name() );
    }

}
