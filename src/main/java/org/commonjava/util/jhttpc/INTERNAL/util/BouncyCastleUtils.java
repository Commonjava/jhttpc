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

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.commonjava.util.jhttpc.JHttpCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.commonjava.util.jhttpc.INTERNAL.util.SSLUtils.extractAliases;

/**
 * Utilities that are firewalled to protect use cases that don't include BouncyCastle. NOTE: If you call this, you should
 * take some measure to ensure BC is available! (eg. Class.forname(..))
 */
public class BouncyCastleUtils
{
    private static final String KEY_TYPE_PATTERN = "BEGIN (.+) PRIVATE KEY";

    static
    {
        Security.addProvider( new BouncyCastleProvider() );
    }

    public static KeyStore readKeyAndCertFromPem( String pemContent, String keyPass )
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, JHttpCException
    {
        Logger logger = LoggerFactory.getLogger( BouncyCastleUtils.class );

        final KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
        ks.load( null );

        //        final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
        //                KeyManagerFactory.getDefaultAlgorithm());
        //
        //        kmfactory.init(ks, keyPass.toCharArray());

        //        final CertificateFactory certFactory = CertificateFactory.getInstance( "X.509" );

        //        Pattern keyTypePattern = Pattern.compile( KEY_TYPE_PATTERN );
        //        Matcher matcher = keyTypePattern.matcher( pemContent );

        //        String keyType = "RSA";
        //        if ( matcher.find() )
        //        {
        //            String type = matcher.group( 1 );
        //            if ( "ENCRYPTED".equals( type ) )
        //            {
        //                keyType = "PKCS8";
        //            }
        //            else
        //            {
        //                keyType = type;
        //            }
        //        }
        //
        //        logger.trace( "Using key factory for type: {}", keyType );
        //        final KeyFactory keyFactory = KeyFactory.getInstance( keyType );

        //        final List<String> lines = SSLUtils.readLines( pemContent );
        //
        //        String currentHeader = null;
        //        final StringBuilder current = new StringBuilder();

        int certIdx = 0;

        BouncyCastleProvider bcProvider = new BouncyCastleProvider();
        InputDecryptorProvider provider =
                new JcePKCSPBEInputDecryptorProviderBuilder().setProvider( bcProvider ).build( keyPass.toCharArray() );

        final List<Certificate> certs = new ArrayList<Certificate>();
        PrivateKey key = null;

        PEMParser pemParser = new PEMParser( new StringReader( pemContent ) );
        Object pemObj = null;
        while ( ( pemObj = pemParser.readObject() ) != null )
        {
            logger.trace( "Got PEM object: {}", pemObj );
            if ( pemObj instanceof X509CertificateHolder )
            {
                X509CertificateHolder holder = (X509CertificateHolder) pemObj;
                X509Certificate certificate =
                        new JcaX509CertificateConverter().setProvider( bcProvider ).getCertificate( holder );

                certs.add( certificate );

                Set<String> aliases = new HashSet<String>();
                aliases.add( "certificate" + certIdx );

                extractAliases( certificate, aliases );

                KeyStore.TrustedCertificateEntry ksEntry = new KeyStore.TrustedCertificateEntry( certificate );
                for ( String alias : aliases )
                {
                    ks.setEntry( alias, ksEntry, null );
                    logger.trace( "Storing trusted cert under alias: {}\n  with DN: {}", alias,
                                  certificate.getSubjectDN().getName() );
                }

                certIdx++;
            }
            else if ( pemObj instanceof PKCS8EncryptedPrivateKeyInfo )
            {
                PKCS8EncryptedPrivateKeyInfo keyInfo = (PKCS8EncryptedPrivateKeyInfo) pemObj;
                PrivateKeyInfo privateKeyInfo = null;
                try
                {
                    privateKeyInfo = keyInfo.decryptPrivateKeyInfo( provider );
                }
                catch ( PKCSException e )
                {
                    throw new JHttpCException( "Failed to decrypt key/certificate: %s", e, e.getMessage() );
                }
                key = new JcaPEMKeyConverter().getPrivateKey( privateKeyInfo );
            }
            else if ( pemObj instanceof PEMEncryptedKeyPair )
            {
                PEMEncryptedKeyPair keyPair = (PEMEncryptedKeyPair) pemObj;
                PEMKeyPair decryptedKeyPair =
                        keyPair.decryptKeyPair( new BcPEMDecryptorProvider( keyPass.toCharArray() ) );
                PrivateKeyInfo privateKeyInfo = decryptedKeyPair.getPrivateKeyInfo();
                key = new JcaPEMKeyConverter().getPrivateKey( privateKeyInfo );
            }
            else
            {
                logger.trace( "Got unrecognized PEM object: {} (class: {})", pemObj,
                              ( pemObj == null ? "NULL" : pemObj.getClass().getName() ) );
            }

            logger.trace( "Got private key:\n{}\n", key );
        }

        if ( key != null && !certs.isEmpty() )
        {
            logger.trace( "Setting key entry: {}", key );
            ks.setKeyEntry( MonolithicKeyStrategy.KEY, key, keyPass.toCharArray(),
                            certs.toArray( new Certificate[certs.size()] ) );
        }
        else
        {
            logger.warn( "No private key found in PEM!" );
        }

        return ks;
    }
}
