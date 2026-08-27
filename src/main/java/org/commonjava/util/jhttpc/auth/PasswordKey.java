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

public class PasswordKey
{

    private final String siteId;

    private final PasswordType passwordType;

    public PasswordKey( SiteConfig config, PasswordType type )
    {
        this( config.getId(), type );
    }

    public PasswordKey( final String siteId, final PasswordType passwordType )
    {
        if ( siteId == null || siteId.trim().length() < 1 )
        {
            throw new IllegalArgumentException( "Empty/missing siteId!" );
        }

        if ( passwordType == null )
        {
            throw new IllegalArgumentException( "Missing passwordType!" );
        }

        this.siteId = siteId;
        this.passwordType = passwordType;
    }

    public String getSiteId()
    {
        return siteId;
    }

    public PasswordType getPasswordType()
    {
        return passwordType;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( siteId.hashCode() );
        result = prime * result + ( ( passwordType == null ) ? 0 : passwordType.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final PasswordKey other = (PasswordKey) obj;
        if ( !siteId.equals( other.siteId ) )
        {
            return false;
        }
        if ( passwordType == null )
        {
            if ( other.passwordType != null )
            {
                return false;
            }
        }
        else if ( !passwordType.equals( other.passwordType ) )
        {
            return false;
        }
        return true;
    }

}
