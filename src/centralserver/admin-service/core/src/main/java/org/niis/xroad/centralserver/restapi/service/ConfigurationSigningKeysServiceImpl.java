/*
 * The MIT License
 * <p>
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.centralserver.restapi.service;

import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.signer.protocol.dto.KeyInfo;
import ee.ria.xroad.signer.protocol.dto.KeyUsageInfo;
import ee.ria.xroad.signer.protocol.dto.TokenInfo;

import lombok.RequiredArgsConstructor;
import org.niis.xroad.centralserver.restapi.service.exception.NotFoundException;
import org.niis.xroad.centralserver.restapi.service.exception.SignerProxyException;
import org.niis.xroad.centralserver.restapi.service.exception.SigningKeyException;
import org.niis.xroad.cs.admin.api.domain.ConfigurationSigningKey;
import org.niis.xroad.cs.admin.api.dto.KeyLabel;
import org.niis.xroad.cs.admin.api.facade.SignerProxyFacade;
import org.niis.xroad.cs.admin.api.service.ConfigurationSigningKeysService;
import org.niis.xroad.cs.admin.api.service.SystemParameterService;
import org.niis.xroad.cs.admin.api.service.TokensService;
import org.niis.xroad.cs.admin.core.entity.ConfigurationSigningKeyEntity;
import org.niis.xroad.cs.admin.core.entity.ConfigurationSourceEntity;
import org.niis.xroad.cs.admin.core.entity.mapper.ConfigurationSigningKeyMapper;
import org.niis.xroad.cs.admin.core.repository.ConfigurationSigningKeyRepository;
import org.niis.xroad.cs.admin.core.repository.ConfigurationSourceRepository;
import org.niis.xroad.restapi.config.audit.AuditDataHelper;
import org.niis.xroad.restapi.config.audit.AuditEventHelper;
import org.niis.xroad.restapi.config.audit.RestApiAuditProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.ACTIVE_SIGNING_KEY_CANNOT_BE_DELETED;
import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.CONFIGURATION_NOT_FOUND;
import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.ERROR_DELETING_SIGNING_KEY;
import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.KEY_GENERATION_FAILED;
import static org.niis.xroad.centralserver.restapi.service.exception.ErrorMessage.SIGNING_KEY_NOT_FOUND;
import static org.niis.xroad.cs.admin.api.domain.ConfigurationSourceType.EXTERNAL;
import static org.niis.xroad.cs.admin.api.domain.ConfigurationSourceType.INTERNAL;
import static org.niis.xroad.cs.admin.api.dto.PossibleAction.GENERATE_KEY;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.DELETE_EXTERNAL_CONFIGURATION_SIGNING_KEY;
import static org.niis.xroad.restapi.config.audit.RestApiAuditEvent.DELETE_INTERNAL_CONFIGURATION_SIGNING_KEY;


@Service
@Transactional
@RequiredArgsConstructor
public class ConfigurationSigningKeysServiceImpl implements ConfigurationSigningKeysService {
    private static final Date SIGNING_KEY_CERT_NOT_BEFORE = Date.from(Instant.EPOCH);
    private static final Date SIGNING_KEY_CERT_NOT_AFTER = Date.from(Instant.parse("2038-01-01T00:00:00Z"));
    private final ConfigurationSigningKeyRepository configurationSigningKeyRepository;
    private final ConfigurationSourceRepository configurationSourceRepository;
    private final ConfigurationSigningKeyMapper configurationSigningKeyMapper;
    private final ObjectProvider<TokensService> tokensService;
    private final SignerProxyFacade signerProxyFacade;
    private final TokenActionsResolver tokenActionsResolver;
    private final AuditEventHelper auditEventHelper;
    private final AuditDataHelper auditDataHelper;


    @Override
    public List<ConfigurationSigningKey> findByTokenIdentifier(String tokenIdentifier) {
        return configurationSigningKeyRepository.findByTokenIdentifier(tokenIdentifier).stream()
                .map(configurationSigningKeyMapper::toTarget).collect(Collectors.toList());
    }

    @Override
    public void deleteKey(String identifier) {
        ConfigurationSigningKey signingKey = configurationSigningKeyRepository.findByKeyIdentifier(identifier)
                .map(configurationSigningKeyMapper::toTarget)
                .orElseThrow(() -> new NotFoundException(SIGNING_KEY_NOT_FOUND));

        if (signingKey.isActiveSourceSigningKey()) {
            throw new SigningKeyException(ACTIVE_SIGNING_KEY_CANNOT_BE_DELETED);
        }

        if (signingKey.getSourceType() == INTERNAL) {
            auditEventHelper.changeRequestScopedEvent(DELETE_INTERNAL_CONFIGURATION_SIGNING_KEY);
        } else if (signingKey.getSourceType() == EXTERNAL) {
            auditEventHelper.changeRequestScopedEvent(DELETE_EXTERNAL_CONFIGURATION_SIGNING_KEY);
        }
        auditDataHelper.put(RestApiAuditProperty.TOKEN_ID, signingKey.getTokenIdentifier());
        auditDataHelper.put(RestApiAuditProperty.KEY_ID, signingKey.getKeyIdentifier());
        try {
            TokenInfo tokenInfo = signerProxyFacade.getToken(signingKey.getTokenIdentifier());
            auditDataHelper.put(RestApiAuditProperty.TOKEN_SERIAL_NUMBER, tokenInfo.getSerialNumber());
            auditDataHelper.put(RestApiAuditProperty.TOKEN_FRIENDLY_NAME, tokenInfo.getFriendlyName());

            configurationSigningKeyRepository.deleteByKeyIdentifier(identifier);
            signerProxyFacade.deleteKey(signingKey.getKeyIdentifier(), true);
        } catch (Exception e) {
            throw new SigningKeyException(ERROR_DELETING_SIGNING_KEY, e);
        }

    }

    public Optional<ConfigurationSigningKey> findActiveForSource(String sourceType) {
        // TODO pass haNodeName if HA enabled
        return configurationSigningKeyRepository.findActiveForSource(sourceType, null)
                .map(configurationSigningKeyMapper::toTarget);
    }

    @Override
    public ConfigurationSigningKey addKey(String sourceType, String tokenId, String keyLabel) {

        var response = new ConfigurationSigningKey();
        response.setActiveSourceSigningKey(Boolean.FALSE);

        ConfigurationSourceEntity configurationSourceEntity =
                findConfigurationSourceBySourceType(sourceType.toLowerCase());

        final TokenInfo tokenInfo = tokensService.getObject().getToken(tokenId);
        tokenActionsResolver.requireAction(GENERATE_KEY, tokenInfo);

        KeyInfo keyInfo;
        try {
            keyInfo = signerProxyFacade.generateKey(tokenId, keyLabel);
        } catch (Exception e) {
            throw new SignerProxyException(KEY_GENERATION_FAILED);
        }

        final Instant generatedAt = Instant.now();

        final ClientId.Conf clientId = ClientId.Conf.create(SystemParameterService.INSTANCE_IDENTIFIER,
                "selfsigned", UUID.randomUUID().toString());

        try {
            final byte[] selfSignedCert = signerProxyFacade.generateSelfSignedCert(keyInfo.getId(), clientId,
                    KeyUsageInfo.SIGNING,
                    "N/A",
                    SIGNING_KEY_CERT_NOT_BEFORE,
                    SIGNING_KEY_CERT_NOT_AFTER);

            ConfigurationSigningKeyEntity signingKey = new ConfigurationSigningKeyEntity(keyInfo.getId(),
                    selfSignedCert, generatedAt, tokenId);

            if (configurationSourceEntity.getConfigurationSigningKey() == null) {
                configurationSourceEntity.setConfigurationSigningKey(signingKey);
                response.setActiveSourceSigningKey(Boolean.TRUE);
            }

            configurationSourceEntity.getConfigurationSigningKeys().add(signingKey);

            configurationSourceRepository.save(configurationSourceEntity);
        } catch (Exception e) {
            deleteKey(keyInfo.getId());
        }

        return response.setKeyIdentifier(keyInfo.getId())
                .setLabel(new KeyLabel(keyInfo.getLabel()))
                .setAvailable(keyInfo.isAvailable())
                .setKeyGeneratedAt(generatedAt)
                .setPossibleActions(new ArrayList<>(tokenActionsResolver.resolveActions(tokenInfo)))
                .setTokenIdentifier(tokenId);
    }

    private ConfigurationSourceEntity findConfigurationSourceBySourceType(String sourceType) {
        return configurationSourceRepository.findBySourceType(sourceType)
                .orElseThrow(() -> new NotFoundException(CONFIGURATION_NOT_FOUND));
    }
}
