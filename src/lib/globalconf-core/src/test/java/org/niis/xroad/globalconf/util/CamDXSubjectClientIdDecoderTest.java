/*
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.globalconf.util;

import ee.ria.xroad.common.CodedException;
import ee.ria.xroad.common.crypto.identifier.SignAlgorithm;
import ee.ria.xroad.common.identifier.ClientId;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link CamDXSubjectClientIdDecoder}, covering the operationally-confirmed
 * (2026-08-31 PKI evidence review) active serialNumber-based Cambodia certificate encoding.
 * The unused legacy O/OU/CN fallback path ({@code parseClientIdFromLegacyName}) is intentionally
 * not exercised here, since current CamDX certificates do not use it and it is not part of the
 * active entry point ({@code getSubjectClientId}).
 */
public class CamDXSubjectClientIdDecoderTest {

    private static KeyPair keyPair;

    @BeforeClass
    public static void init() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    /**
     * Decodes a Cambodia SIGN certificate subject in the active serialNumber-based encoding,
     * using synthetic, non-operational values:
     * C=KH, O=CamDX Test Member, CN=CAMDX-TEST-000001, serialNumber=CAMBODIA/ss.example.invalid/GOV.
     */
    @Test
    public void shouldDecodeClientIdFromObservedCertificate()
            throws GeneralSecurityException, IOException, OperatorCreationException {
        X509Certificate cert = generateSelfSignedCertificate(
                "C=KH, O=CamDX Test Member, CN=CAMDX-TEST-000001, serialNumber=CAMBODIA/ss.example.invalid/GOV", keyPair);

        ClientId clientId = CamDXSubjectClientIdDecoder.getSubjectClientId(cert);

        assertEquals(ClientId.Conf.create("CAMBODIA", "GOV", "CAMDX-TEST-000001"), clientId);
    }

    @Test(expected = CodedException.class)
    public void shouldFailIfCountryDoesNotMatch() throws GeneralSecurityException, IOException,
            OperatorCreationException {
        X509Certificate cert = generateSelfSignedCertificate(
                "C=XX, O=CamDX Test Member, CN=CAMDX-TEST-000001, serialNumber=CAMBODIA/ss.example.invalid/GOV", keyPair);
        CamDXSubjectClientIdDecoder.getSubjectClientId(cert);
    }

    @Test(expected = CodedException.class)
    public void shouldFailIfOrgMissing() throws GeneralSecurityException, IOException, OperatorCreationException {
        X509Certificate cert = generateSelfSignedCertificate(
                "C=KH, CN=CAMDX-TEST-000001, serialNumber=CAMBODIA/ss.example.invalid/GOV", keyPair);
        CamDXSubjectClientIdDecoder.getSubjectClientId(cert);
    }

    @Test(expected = CodedException.class)
    public void shouldFailIfCommonNameMissing() throws GeneralSecurityException, IOException,
            OperatorCreationException {
        X509Certificate cert = generateSelfSignedCertificate(
                "C=KH, O=CamDX Test Member, serialNumber=CAMBODIA/ss.example.invalid/GOV", keyPair);
        CamDXSubjectClientIdDecoder.getSubjectClientId(cert);
    }

    @Test(expected = CodedException.class)
    public void shouldFailIfSerialNumberMissing() throws GeneralSecurityException, IOException,
            OperatorCreationException {
        X509Certificate cert = generateSelfSignedCertificate("C=KH, O=CamDX Test Member, CN=CAMDX-TEST-000001", keyPair);
        CamDXSubjectClientIdDecoder.getSubjectClientId(cert);
    }

    @Test(expected = CodedException.class)
    public void shouldFailIfSerialNumberHasTooFewComponents() throws GeneralSecurityException, IOException,
            OperatorCreationException {
        X509Certificate cert = generateSelfSignedCertificate(
                "C=KH, O=CamDX Test Member, CN=CAMDX-TEST-000001, serialNumber=CAMBODIA/ss.example.invalid", keyPair);
        CamDXSubjectClientIdDecoder.getSubjectClientId(cert);
    }

    @Test(expected = CodedException.class)
    public void shouldFailIfSerialNumberHasTooManyComponents() throws GeneralSecurityException, IOException,
            OperatorCreationException {
        X509Certificate cert = generateSelfSignedCertificate(
                "C=KH, O=CamDX Test Member, CN=CAMDX-TEST-000001, serialNumber=CAMBODIA/ss.example.invalid/GOV/EXTRA", keyPair);
        CamDXSubjectClientIdDecoder.getSubjectClientId(cert);
    }

    private X509Certificate generateSelfSignedCertificate(String dn, KeyPair pair)
            throws OperatorCreationException, CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder(SignAlgorithm.SHA256_WITH_RSA.name()).build(pair.getPrivate());
        X500Name name = new X500Name(dn);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, new Date(),
                new Date(), name, pair.getPublic()
        );

        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
