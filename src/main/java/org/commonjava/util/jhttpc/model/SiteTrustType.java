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

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;

/**
 * Created by jdcasey on 10/21/15.
 */
public enum SiteTrustType
{
    TRUST_SELF_SIGNED( "self-signed", "trust-self-signed" )
            {
                @Override
                public TrustStrategy getTrustStrategy()
                {
                    return new TrustSelfSignedStrategy();
                }
            },

    DEFAULT( "default" )
            {
                @Override
                public TrustStrategy getTrustStrategy()
                {
                    return null;
                }
            };

    private String[] aliases;

    SiteTrustType( String... aliases )
    {
        this.aliases = aliases;
    }

    public abstract TrustStrategy getTrustStrategy();

    public static SiteTrustType getType( String named )
    {
        for ( SiteTrustType type : values() )
        {
            if ( type.name().equalsIgnoreCase( named ) )
            {
                return type;
            }

            for ( String alias : type.aliases )
            {
                if ( alias.equalsIgnoreCase( named ) )
                {
                    return type;
                }
            }
        }

        return DEFAULT;
    }
}
