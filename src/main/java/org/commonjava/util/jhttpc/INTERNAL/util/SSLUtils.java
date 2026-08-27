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

import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.commonjava.util.jhttpc.JHttpCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.x500.X500Principal;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.io.IOUtils.closeQuietly;

public final class SSLUtils
{

    private static final String[] BC_TEST_NAMES =
            { "org.bouncycastle.jce.provider.BouncyCastleProvider", "org.bouncycastle.openssl.PEMParser" };

    private static final Integer DNSNAME_TYPE = 2;

    private SSLUtils()
    {
    }

    public static KeyStore readKeyAndCert( final String pemContent, final String keyPass )
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
                   InvalidKeySpecException, JHttpCException
    {
        Logger logger = LoggerFactory.getLogger( SSLUtils.class );

        boolean bcEnabled = true;
        for ( String bctestName : BC_TEST_NAMES )
        {
            try
            {
                Class.forName( bctestName );
            }
            catch ( ClassNotFoundException e )
            {
                logger.warn(
                        "One or more BouncyCastle jars (bcprov-jdk15on, bcpkix-jdk15on) are missing from the classpath! PEM SSL client keys are not supported!" );
                bcEnabled = false;
                break;
            }
        }

        if ( !bcEnabled )
        {
            return null;
        }

        KeyStore ks = BouncyCastleUtils.readKeyAndCertFromPem( pemContent, keyPass );

        Enumeration<String> aliases = ks.aliases();
        while ( aliases.hasMoreElements() )
        {
            String alias = aliases.nextElement();
            logger.trace( "Got alias: {}. Is Cert? {} Is Private key? {}", alias, ks.isCertificateEntry( alias ),
                          ks.isKeyEntry( alias ) );
        }

        return ks;
    }

    public static KeyStore decodePEMTrustStore( final String pemContent, final String aliasPrefix )
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException
    {
        Logger logger = LoggerFactory.getLogger( SSLUtils.class );

        final KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
        ks.load( null );

        final CertificateFactory certFactory = CertificateFactory.getInstance( "X.509" );

        final List<String> lines = readLines( pemContent );

        final StringBuilder current = new StringBuilder();
        final List<String> entries = new ArrayList<String>();
        for ( String line : lines )
        {
            if ( line == null )
            {
                continue;
            }

            if ( line.startsWith( "-----BEGIN" ) )
            {
                current.setLength( 0 );
            }
            else if ( line.startsWith( "-----END" ) )
            {
                entries.add( current.toString() );
            }
            else
            {
                current.append( line );
            }
        }

        logger.trace( "Found {} entries to decode.", entries.size() );

        int i = 0;
        for ( final String entry : entries )
        {
            logger.trace( "Decoding certificate info from:\n\n{}\n\n", entry );
            final byte[] data = decodeBase64( entry );

            final Certificate c = certFactory.generateCertificate( new ByteArrayInputStream( data ) );
            X509Certificate cert = (X509Certificate) c;

            Set<String> aliases = new HashSet<String>();
            if ( i < 1 )
            {
                aliases.add( aliasPrefix );
            }
            else
            {
                aliases.add( aliasPrefix + i );
            }

            extractAliases( cert, aliases );

            KeyStore.TrustedCertificateEntry ksEntry = new KeyStore.TrustedCertificateEntry( cert );
            for ( String alias : aliases )
            {
                ks.setEntry( alias, ksEntry, null );
                logger.trace( "Storing trusted cert under alias: {}\n  with DN: {}", alias,
                              cert.getSubjectDN().getName() );
            }

            logger.trace( "Certificate added." );

            i++;
        }

        return ks;
    }

    public static void extractAliases( Certificate certificate, Set<String> aliases )
            throws CertificateParsingException
    {
        Logger logger = LoggerFactory.getLogger( SSLUtils.class );

        X509Certificate cert = (X509Certificate) certificate;
//        logger.debug( "Extracting aliases from:\n\n{}\n\n", cert );

        X500Principal x500Principal = cert.getSubjectX500Principal();
        X500Name x500Name = new X500Name( x500Principal.getName( X500Principal.RFC1779 ) );
        logger.trace( "Certificate X.500 name: '{}'", x500Name.toString() );

        RDN[] matchingRDNs = x500Name.getRDNs( BCStyle.CN );
        if ( matchingRDNs != null && matchingRDNs.length > 0 )
        {
            RDN cn = matchingRDNs[0];
            AttributeTypeAndValue typeAndValue = cn.getFirst();
            if ( typeAndValue != null )
            {
                String alias = IETFUtils.valueToString( typeAndValue.getValue() );
                logger.trace( "Found certificate alias: '{}'", alias );
                aliases.add( alias );
            }
        }

        Collection<List<?>> subjectAlternativeNames = cert.getSubjectAlternativeNames();
        if ( subjectAlternativeNames != null )
        {
            for ( List<?> names : subjectAlternativeNames )
            {
                if ( names.size() > 1 && ( DNSNAME_TYPE.equals( names.get( 0 ) ) ) )
                {
                    String alias = (String) names.get( 1 );
                    logger.trace( "Found subjectAlternativeName: '{}'", alias );
                    aliases.add( alias );
                }
            }
        }
        else
        {
            logger.debug( "NO SubjectAlternativeNames available!" );
        }
    }

    public static List<String> readLines( final String content )
            throws IOException
    {
        final List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new InputStreamReader(
                    new ByteArrayInputStream( content.getBytes( Charset.forName( "UTF-8" ) ) ) ) );
            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                lines.add( line.trim() );
            }
        }
        finally
        {
            closeQuietly( reader );
        }

        return lines;
    }
}
