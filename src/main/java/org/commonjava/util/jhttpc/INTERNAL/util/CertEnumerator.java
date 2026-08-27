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
package org.commonjava.util.jhttpc.INTERNAL.util;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Created by jdcasey on 10/28/15.
 */
public final class CertEnumerator
{
    private final KeyStore ks;

    private String kcPass;

    public CertEnumerator( final KeyStore ks, String kcPass )
    {
        this.ks = ks;
        this.kcPass = kcPass;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();

        try
        {
            for ( final Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements(); )
            {
                final String alias = aliases.nextElement();
                final X509Certificate cert = (X509Certificate) ks.getCertificate( alias );

                sb.append( "\nAlias: " ).append( alias );
                if ( cert != null )
                {
                    sb.append( "\n\t" ).append( cert.getSubjectDN() );
                }

                if ( kcPass != null )
                {
                    Key key = ks.getKey( alias, kcPass.toCharArray() );
                    if ( key != null )
                    {
                        sb.append("\n\t").append( key.getAlgorithm() ).append( " private key." );
                    }
                }
            }
        }
        catch ( final KeyStoreException e )
        {
            sb.append( "ERROR READING KEYSTORE" );
        }
        catch ( UnrecoverableKeyException e )
        {
            sb.append( "ERROR READING KEY" );
        }
        catch ( NoSuchAlgorithmException e )
        {
            sb.append( "ERROR READING KEY" );
        }

        return sb.toString();
    }
}
