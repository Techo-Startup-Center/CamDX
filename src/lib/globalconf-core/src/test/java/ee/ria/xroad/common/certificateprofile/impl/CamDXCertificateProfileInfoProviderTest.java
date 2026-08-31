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
package ee.ria.xroad.common.certificateprofile.impl;

import ee.ria.xroad.common.certificateprofile.CertificateProfileInfoProvider;
import ee.ria.xroad.common.certificateprofile.DnFieldDescription;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.SecurityServerId;

import org.bouncycastle.util.Arrays;
import org.junit.Test;
import org.mockito.Mockito;

import javax.security.auth.x500.X500Principal;

import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the CamDX implementation of CertificateProfileInfoProvider against the operationally-confirmed
 * Cambodia certificate subject encoding (serialNumber = instanceId/serverCode/memberClass).
 */
public class CamDXCertificateProfileInfoProviderTest {

    private static final SecurityServerId SERVER_ID =
            SecurityServerId.Conf.create("CAMBODIA", "GOV", "MOI", "ss-pro.gddtm.gov.kh");
    private static final ClientId CLIENT_ID = ClientId.Conf.create("CAMBODIA", "GOV", "CAMDX-000121");

    @Test
    public void providerReturnsCorrectImplementations() {
        CertificateProfileInfoProvider provider = new CamDXCertificateProfileInfoProvider();

        assertTrue(
                "Must return instance of CamDXAuthCertificateProfileInfo",
                provider.getAuthCertProfile(
                        new AuthCertificateProfileInfoParameters(SERVER_ID, "MOI")
                ) instanceof CamDXAuthCertificateProfileInfo
        );

        assertTrue(
                "Must return instance of CamDXSignCertificateProfileInfo",
                provider.getSignCertProfile(
                        new SignCertificateProfileInfoParameters(SERVER_ID, CLIENT_ID, "MOI")
                ) instanceof CamDXSignCertificateProfileInfo
        );
    }

    /**
     * Sign-certificate DN fields must reflect: CN = memberCode, serialNumber = instanceId/serverCode/memberClass.
     */
    @Test
    public void signProfileSubjectFields() {
        DnFieldDescription[] expectedFields = {
                new EnumLocalizedFieldDescriptionImpl("C", DnFieldLabelLocalizationKey.COUNTRY_CODE, "KH")
                        .setReadOnly(true),
                new EnumLocalizedFieldDescriptionImpl("O", DnFieldLabelLocalizationKey.ORGANIZATION_NAME, "")
                        .setReadOnly(false),
                new EnumLocalizedFieldDescriptionImpl("serialNumber", DnFieldLabelLocalizationKey.SERIAL_NUMBER,
                        "CAMBODIA/ss-pro.gddtm.gov.kh/GOV")
                        .setReadOnly(true),
                new EnumLocalizedFieldDescriptionImpl("CN", DnFieldLabelLocalizationKey.MEMBER_CODE,
                        "CAMDX-000121")
                        .setReadOnly(true)
        };

        assertTrue(
                "Did not get expected sign-profile fields",
                Arrays.areEqual(expectedFields, getSignProfile().getSubjectFields())
        );
    }

    /**
     * Auth-certificate DN fields must reflect: CN = server DNS name (operator-entered),
     * serialNumber = instanceId/serverCode/memberClass.
     */
    @Test
    public void authProfileSubjectFields() {
        DnFieldDescription[] expectedFields = {
                new EnumLocalizedFieldDescriptionImpl("C", DnFieldLabelLocalizationKey.COUNTRY_CODE, "KH")
                        .setReadOnly(true),
                new EnumLocalizedFieldDescriptionImpl("O", DnFieldLabelLocalizationKey.ORGANIZATION_NAME, "")
                        .setReadOnly(false),
                new EnumLocalizedFieldDescriptionImpl("serialNumber", DnFieldLabelLocalizationKey.SERIAL_NUMBER,
                        "CAMBODIA/ss-pro.gddtm.gov.kh/GOV")
                        .setReadOnly(true),
                new EnumLocalizedFieldDescriptionImpl("CN", DnFieldLabelLocalizationKey.SERVER_DNS_NAME, "")
                        .setReadOnly(false)
        };

        assertTrue(
                "Did not get expected auth-profile fields",
                Arrays.areEqual(expectedFields, getAuthProfile().getSubjectFields())
        );
    }

    /**
     * Reproduces the operationally-confirmed current CamDX SIGN certificate subject
     * (C=KH,O=MOI,CN=CAMDX-000121,serialNumber=CAMBODIA/ss-pro.gddtm.gov.kh/GOV) end-to-end through
     * CamDXSignCertificateProfileInfo.getSubjectIdentifier(), which delegates to CamDXSubjectClientIdDecoder.
     */
    @Test
    public void signProfileGetSubjectIdentifierMatchesObservedCertificate() {
        X509Certificate mockCert = Mockito.mock(X509Certificate.class);
        Mockito.when(mockCert.getSubjectX500Principal()).thenReturn(
                new X500Principal("C=KH,O=MOI,CN=CAMDX-000121,serialNumber=CAMBODIA/ss-pro.gddtm.gov.kh/GOV")
        );

        assertEquals(
                ClientId.Conf.create("CAMBODIA", "GOV", "CAMDX-000121"),
                getSignProfile().getSubjectIdentifier(mockCert)
        );
    }

    private CamDXSignCertificateProfileInfo getSignProfile() {
        return new CamDXSignCertificateProfileInfo(
                new SignCertificateProfileInfoParameters(SERVER_ID, CLIENT_ID, "MOI")
        );
    }

    private CamDXAuthCertificateProfileInfo getAuthProfile() {
        return new CamDXAuthCertificateProfileInfo(
                new AuthCertificateProfileInfoParameters(SERVER_ID, "MOI")
        );
    }
}
