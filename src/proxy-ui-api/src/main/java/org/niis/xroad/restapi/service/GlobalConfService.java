/**
 * The MIT License
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
package org.niis.xroad.restapi.service;

import ee.ria.xroad.common.CodedException;
import ee.ria.xroad.common.conf.globalconf.ApprovedCAInfo;
import ee.ria.xroad.common.conf.globalconf.GlobalGroupInfo;
import ee.ria.xroad.common.conf.globalconf.MemberInfo;
import ee.ria.xroad.common.conf.serverconf.model.TspType;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.SecurityServerId;
import ee.ria.xroad.common.identifier.XRoadId;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.facade.GlobalConfFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ee.ria.xroad.common.ErrorCodes.X_OUTDATED_GLOBALCONF;

/**
 * Global configuration service.
 * Contains methods that add some extra logic to the methods provided by {@link GlobalConfFacade}.
 * To avoid method explosion, do not add pure delegate methods here, use GlobalConfFacade directly instead.
 */
@Slf4j
@Service
@PreAuthorize("isAuthenticated()")
public class GlobalConfService {

    private final GlobalConfFacade globalConfFacade;
    private final ServerConfService serverConfService;

    @Autowired
    public GlobalConfService(GlobalConfFacade globalConfFacade,
            ServerConfService serverConfService) {
        this.globalConfFacade = globalConfFacade;
        this.serverConfService = serverConfService;
    }

    /**
     * @param securityServerId
     * @return whether the security server exists in current instance's global configuration
     */
    public boolean securityServerExists(SecurityServerId securityServerId) {
        if (!globalConfFacade.getInstanceIdentifiers().contains(securityServerId.getXRoadInstance())) {
            // unless we check instance existence like this, we will receive
            // CodedException: InternalError: Invalid instance identifier: x -exception
            // which is hard to turn correctly into http 404 instead of 500
            return false;
        }
        return globalConfFacade.existsSecurityServer(securityServerId);
    }

    /**
     * @param identifiers
     * @return whether the global group identifiers exist in global configuration
     */
    public boolean globalGroupIdentifiersExist(Collection<XRoadId> identifiers) {
        List<XRoadId> existingIdentifiers = globalConfFacade.getGlobalGroups().stream()
                .map(GlobalGroupInfo::getId)
                .collect(Collectors.toList());
        return existingIdentifiers.containsAll(identifiers);
    }

    /**
     * @param identifiers
     * @return whether the members identifiers exist in global configuration
     */
    public boolean clientIdentifiersExist(Collection<XRoadId> identifiers) {
        List<XRoadId> existingIdentifiers = globalConfFacade.getMembers().stream()
                .map(MemberInfo::getId)
                .collect(Collectors.toList());
        return existingIdentifiers.containsAll(identifiers);
    }

    /**
     * @return member classes for current instance
     */
    public Set<String> getMemberClassesForThisInstance() {
        return globalConfFacade.getMemberClasses(globalConfFacade.getInstanceIdentifier());
    }

    /**
     * Check the validity of the GlobalConf
     * @throws GlobalConfOutdatedException if conf is outdated
     */
    public void verifyGlobalConfValidity() throws GlobalConfOutdatedException {
        try {
            globalConfFacade.verifyValidity();
        } catch (CodedException e) {
            if (isCausedByOutdatedGlobalconf(e)) {
                throw new GlobalConfOutdatedException(e);
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("global conf validity check failed", e);
        }
    }

    static boolean isCausedByOutdatedGlobalconf(CodedException e) {
        return X_OUTDATED_GLOBALCONF.equals(e.getFaultCode());
    }

    /**
     * @return approved CAs for current instance
     */
    public Collection<ApprovedCAInfo> getApprovedCAsForThisInstance() {
        return globalConfFacade.getApprovedCAs(globalConfFacade.getInstanceIdentifier());
    }

    /**
     * @return approved timestamping services for current instance.
     * {@link TspType#getId()} is null for all returned items
     */
    public List<TspType> getApprovedTspsForThisInstance() {
        List<String> urls = globalConfFacade.getApprovedTsps(globalConfFacade.getInstanceIdentifier());
        List<TspType> tsps = urls.stream()
                .map(this::createTspType)
                .collect(Collectors.toList());
        return tsps;
    }

    /**
     * init TspType DTO with name and url. id will be null
     */
    private TspType createTspType(String url) {
        TspType tsp = new TspType();
        tsp.setUrl(url);
        tsp.setName(globalConfFacade.getApprovedTspName(globalConfFacade.getInstanceIdentifier(), url));
        return tsp;
    }

    /**
     * @param url
     * @return name of the timestamping service with the given url
     */
    public String getApprovedTspName(String url) {
        return globalConfFacade.getApprovedTspName(globalConfFacade.getInstanceIdentifier(), url);
    }

    /**
     * Checks if given client is one of this security server's clients
     */
    public boolean isSecurityServerClientForThisInstance(ClientId client) {
        return globalConfFacade.isSecurityServerClient(client,
                serverConfService.getSecurityServerId());
    }

}
