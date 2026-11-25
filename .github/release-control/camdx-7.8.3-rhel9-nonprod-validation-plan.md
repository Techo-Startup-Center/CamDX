# CamDX 7.8.3 RHEL 9 Non-Production Validation Plan

## Status

PLAN ONLY.

This plan begins only after the exact `7.8.3-1.el9` candidate from GitHub Actions run `43543209160` has completed:

1. Package Builder 19/19 independent verification.
2. Controlled-host signing of verified copies.
3. Isolated Nexus publication to `camdx-rpm-7.8.3-r1-canary`.
4. Nexus 19/19 round-trip checksum/signature verification.
5. Isolated RHEL 9 repository-consumption check.

No production promotion, member-facing repository-group change, or operational-server deployment is authorized by this plan.

---

## 1. Release identity and test topology

Frozen candidate:

- Source: `3887c949ec5d02c87fb5351e1c960b61607bbfd6`
- Full CI: `43408359021`
- RHEL release build: `43543209160`
- RPM version: `7.8.3-1.el9`
- 19 packages: 12 x86_64, 7 noarch

RHEL RPM packaging covers Security Server components. Central Server validation must use the approved 7.8.3 Central Server build/package/container track and validate interoperability with the RHEL 9 Security Server canary.

Recommended isolated topology:

- CS-A: CamDX 7.8.3 Central Server in non-production.
- SS-RHEL-FRESH: clean RHEL 9 Security Server installed from the 7.8.3 canary repository.
- SS-RHEL-UPGRADE: RHEL 9 Security Server initially running approved CamDX 7.8.2, then upgraded to 7.8.3.
- SS-OLD-742: representative CamDX 7.4.2 Security Server.
- SS-OLD-732 / SS-OLD-722: representative legacy versions where coexistence coverage requires them.
- Mock/DEV provider and requester services for real signed API exchange.
- Non-production PKI/OCSP/TSA and, where available, Futurex/HSM test token.

Do not use a production member identity or operational production server.

---

## 2. Gate N0: Pre-deployment evidence and rollback readiness

Owner: CamDX Operations + Infra/DevOps.

Before any 7.8.3 package transaction, capture:

```bash
date -Is
cat /etc/redhat-release
uname -a
getenforce
timedatectl status

rpm -qa | sort > rpm-before.txt
dnf repolist -v > dnf-repolist-before.txt
systemctl --no-pager --type=service --state=running > services-before.txt

sudo tar -C / -czf etc-xroad-before.tgz etc/xroad
sudo -u postgres pg_dumpall > postgres-before.sql

sudo find /etc/xroad -type f -print0   | sort -z   | xargs -0 sha256sum   > etc-xroad-before-SHA256SUMS.txt
```

Also record:

- current global configuration anchor;
- AUTH certificate inventory and registration status;
- SIGN certificate inventory and status;
- Security Server code and registered clients/subsystems;
- active token/key inventory without private-key export;
- current OCSP/TSA configuration;
- current service endpoints and access rights;
- current operational-monitoring state.

Stop if a usable rollback backup cannot be demonstrated.

---

## 3. Gate N1: Fresh RHEL 9 installation

Owner: Infra/DevOps + CamDX Operations.

On `SS-RHEL-FRESH`:

1. Start from clean RHEL 9 with SELinux Enforcing.
2. Configure only the isolated 7.8.3 canary repository plus approved dependency repositories.
3. Confirm dependency resolution before changing package state.
4. Install the same Security Server package set used in the approved RHEL deployment model.
5. Confirm PostgreSQL first-start initialization.
6. Confirm `serverconf` and `messagelog` database initialization.
7. Confirm expected runtime filesystem ownership and permissions.
8. Confirm all required X-Road/CamDX services start.
9. Reboot the OS and confirm full recovery.
10. Confirm Admin UI title displays `CamDX Security Server`.

Minimum evidence:

```bash
sudo dnf   --disablerepo='*'   --enablerepo='camdx-7.8.3-canary'   repoquery   --requires --resolve xroad-securityserver   > dependency-resolution.txt

sudo dnf install -y xroad-securityserver

rpm -qa 'xroad-*' | sort > rpm-after-fresh-install.txt
systemctl --no-pager --full status   xroad-signer   xroad-confclient   xroad-proxy   xroad-proxy-ui-api   xroad-monitor   xroad-opmonitor   > services-after-fresh-install.txt
```

Include addon services used by the test topology.

Gate passes only after fresh install, service restart and full OS reboot are successful.

---

## 4. Gate N2: In-place RHEL 9 upgrade from CamDX 7.8.2

Owner: Infra/DevOps + CamDX Operations.

On `SS-RHEL-UPGRADE`:

1. Establish an approved 7.8.2 RHEL baseline.
2. Complete Gate N0 backups and state capture.
3. Add the isolated 7.8.3 canary repository without altering the member-facing group.
4. Review the transaction before executing it.
5. Upgrade to `7.8.3-1.el9`.
6. Confirm configuration, databases, client/service definitions and certificates survive.
7. Confirm services restart and recover after OS reboot.

Transaction preview:

```bash
sudo dnf   --disablerepo='*'   --enablerepo='camdx-7.8.3-canary'   upgrade --assumeno 'xroad-*'   | tee dnf-upgrade-preview.txt
```

Execute only after reviewing that no unexpected package removal/downgrade is proposed.

After upgrade:

```bash
rpm -qa 'xroad-*' | sort > rpm-after-upgrade.txt
sudo systemctl --failed > systemd-failed-after-upgrade.txt
```

The official 7.8.2 → 7.8.3 upstream delta contains no Liquibase/database migration file changes. This lowers schema-migration risk but does not eliminate runtime compatibility testing.

Verify database state explicitly before declaring rollback safe.

---

## 5. Gate N3: CamDX PKI and certificate-profile regression

Owner: CamDX PKI/Security Engineering + Core Engineering.

Validate the Cambodia-specific certificate extension end to end.

### Existing certificates

- Existing CamDX AUTH certificate accepted after fresh/upgrade configuration.
- Existing CamDX SIGN certificate correctly maps to CamDX ClientId.
- Existing certificate status/OCSP validation remains functional.
- Existing TSA timestamp verification remains functional.

### New certificate lifecycle

- Generate new AUTH CSR.
- Issue/import AUTH certificate.
- Register AUTH certificate with Central Server.
- Confirm registration approval/activation.
- Generate new SIGN CSR using representative Cambodia DN/profile.
- Issue/import SIGN certificate.
- Activate signing certificate.
- Perform message signing and verification.
- Renew/replace certificates using the approved non-production lifecycle.

Representative SIGN semantics must include:

```text
C=KH
CN=<memberCode>
serialNumber=CAMBODIA/<serverCode>/<memberClass>
```

Expected ClientId mapping:

```text
CAMBODIA/<memberClass>/<memberCode>
```

Also validate AUTH/SIGN behavior with software token and the non-production HSM track where applicable.

---

## 6. Gate N4: Global configuration trust and certificate-validation security regression

Owner: Core X-Road Engineering + PKI/Security Engineering.

This is a release-critical 7.8.3 gate.

Official 7.8.3 fixes include:

- XRDDEV-3264: cached certificate trust must be checked against the current configuration source.
- XRDDEV-3265: revoked/rotated configuration-signing certificates must not remain trusted.
- XRDDEV-3271 / XRDDEV-3272: Configuration Client/Proxy global-configuration hardening.
- XRDDEV-3278: Configuration Client security hardening / same-origin behavior.

Validate positive behavior:

- Current CamDX anchor accepted.
- Internal and external global configuration download succeeds.
- Security Server and Central Server diagnostics remain healthy.
- Normal configuration signing-certificate rotation works.

Validate negative/security cases in an isolated test source:

- certificate removed from current configuration but present in old cache;
- expired certificate;
- rotated/revoked configuration-signing certificate;
- malformed configuration version;
- invalid instance directory;
- path traversal attempt;
- reserved filename;
- duplicate output target;
- foreign-origin or redirected `Content-Location`;
- malformed/detached configuration metadata.

Expected result: reject unsafe configuration and retain the last known valid operational configuration where the product design specifies that behavior. Capture exact error/status and service logs.

Do not test malformed global configuration against the operational CamDX configuration source.

---

## 7. Gate N5: 7.8.3 parser/message security regression

Owner: Core/Security Engineering.

Official 7.8.3 fixes include:

- XRDDEV-3262: bound signature MIME data.
- XRDDEV-3263: harden hash-chain verification.
- XRDDEV-3268: reject DTDs before SAAJ parsing.
- XRDDEV-3269: disallow DOCTYPE in Signature XML.

Positive test:

- standard SOAP request/response;
- signed request/response;
- attachments;
- normal hash-chain verification.

Negative tests:

- signature MIME part above the 10 MB security limit;
- hash-chain MIME part above the 1 MB security limit;
- malicious/recursive/expensive hash-chain structure;
- SOAP containing DTD;
- XXE/external entity payload;
- Signature XML containing DOCTYPE/external entity constructs.

Expected result: reject safely without excessive memory/CPU growth, parser entity resolution, proxy crash or service instability.

Capture proxy memory/CPU and logs during malformed-input tests.

---

## 8. Gate N6: Registration authorization and RBAC

Owner: Core Engineering + CamDX Operations.

### Registration authorization

Official fix XRDDEV-3266 prevents paired registration requests from bypassing disabled auto-approval.

Test independently:

- auto-approve authentication registration = true;
- auto-approve authentication registration = false;
- auto-approve client registration = true;
- auto-approve client registration = false;
- paired requests and `SUBMITTED_FOR_APPROVAL` state.

Expected result: no approval occurs where policy is disabled.

### Central Server permissions

Official fix XRDDEV-3267 removes unnecessary permissions from `XROAD_MANAGEMENT_SERVICE`.

Validate representative actions for:

- XROAD_MANAGEMENT_SERVICE;
- System Administrator;
- Security Officer;
- Observer;
- Registration Officer / equivalent configured roles.

Expected result: least-privilege behavior, with no regression for legitimate operational flows.

### Security Server send-test-mail access

Official fix XRDDEV-3276:

- System Administrator allowed.
- unauthorized roles denied.
- recipient restricted to configured notification contacts.

---

## 9. Gate N7: ACME HTTP-01 security and certificate renewal

Owner: PKI/Security Engineering.

Official fix XRDDEV-3281 hardens ACME challenge token handling.

Test:

- valid HTTP-01 token;
- `../` traversal;
- URL-encoded traversal;
- path separators;
- invalid characters;
- excessively long/abnormal token;
- challenge remains under configured challenge directory.

Where ACME is enabled in the CamDX test environment, complete one normal renewal cycle.

If CamDX production PKI does not use ACME for a given certificate type, record the feature as tested for platform safety but not part of the production certificate workflow.

---

## 10. Gate N8: PKCS#11 / HSM recovery

Owner: PKI/HSM Engineering.

Official fix XRDDEV-3301 changes `PKCS11Exception` handling during hardware token worker refresh.

Using the non-production Futurex/HSM test arrangement where available:

1. Token online and usable.
2. Sign successfully.
3. Temporarily make token unavailable through an approved test mechanism.
4. Observe worker refresh and exception behavior.
5. Restore token.
6. Confirm signing recovers.
7. Confirm whether signer restart is or is not required.
8. Confirm no stale key state or duplicate token worker persists.

Do not induce a production-HSM outage for this test.

---

## 11. Gate N9: End-to-end CamDX functionality

Owner: CamDX Operations + Core Engineering.

Validate:

- member registration;
- Security Server registration;
- subsystem/client registration;
- service description/configuration;
- access rights;
- real signed service request;
- real service response;
- error response;
- attachment request if used;
- local operational monitoring;
- remote `getSecurityServerOperationalData`;
- diagnostics page;
- configuration backup and restore.

For each exchange record:

- requester ClientId;
- provider ClientId;
- service ID;
- Security Server versions;
- request/response timestamp;
- result;
- relevant transaction/message ID;
- no sensitive payload in evidence.

---

## 12. Gate N10: Coexistence and mixed-version compatibility

Owner: CamDX Operations + Core Engineering.

Required matrix:

| Central Server | Security Server | Required |
|---|---|---|
| 7.8.3 | 7.8.3 RHEL | Yes |
| 7.8.3 | existing 7.8.2 | Yes |
| 7.8.3 | 7.4.2 | Yes |
| 7.8.3 | representative 7.3.2 | If still in coexistence scope |
| 7.8.3 | representative 7.2.2 | If still in coexistence scope |

For each required pairing verify:

- global configuration consumption;
- AUTH certificate registration/status;
- client/subsystem registration;
- signed request/response;
- OCSP/TSA;
- operational monitoring;
- expected diagnostics;
- no protocol incompatibility.

Preserve the approved parallel-replacement/blue-green migration model:

- OLD Security Server remains registered and operational.
- NEW Security Server uses a new Security Server code.
- NEW has independently generated keys/certificates.
- OLD remains rollback target until NEW is accepted.
- Do not clone private keys or reuse OLD server identity simply to simulate migration.

---

## 13. Gate N11: Backup, restore and database/configuration compatibility

Owner: Infra/DevOps + CamDX Operations.

Validate on fresh and upgraded 7.8.3 Security Servers:

- configuration backup creation;
- backup download;
- backup upload;
- restore;
- restored local configuration;
- restored database connectivity;
- certificate/key metadata consistency;
- service restart after restore;
- full OS reboot after restore.

Also test the documented supported direction for backup compatibility. Do not assume a 7.8.3-created backup can be restored to 7.8.2 unless explicitly proven.

The 7.8.2 → 7.8.3 changed-file set contains no Liquibase/database migration files, but capture schema/version state before and after upgrade and restore.

---

## 14. Gate N12: Rollback and service restoration

Owner: Infra/DevOps + CamDX Operations.

Rollback must be demonstrated, not only documented.

Before the 7.8.3 upgrade:

- preserve the 7.8.2 repository definition;
- database backup;
- `/etc/xroad` backup;
- complete package inventory;
- anchor copy;
- certificate/token inventory.

Test rollback on the isolated upgrade VM:

1. Stop traffic.
2. Restore approved 7.8.2 package repository.
3. Downgrade/reinstall the exact approved 7.8.2 package set.
4. Restore configuration/database only if required by the observed package behavior and validated procedure.
5. Start services.
6. Confirm global configuration.
7. Confirm certificates/token access.
8. Perform real request/response.
9. Reboot and confirm service restoration.

If RPM downgrade is blocked or any database/configuration incompatibility is found, classify rollback as HOLD and redesign the rollback procedure before release approval.

---

## 15. PR #18 and release-workflow governance

Owner: Core Engineering / repository maintainers.

PR #18 remains subject to normal review.

Do not merge solely to make the release workflow available.

Before merge:

- ordinary CI should be reviewed and green or any known intermittent failure explicitly re-run/resolved;
- release-control documentation reviewed;
- no product-source mutation after candidate source `3887c949...` may be silently treated as part of the frozen package candidate.

After approved merge to `master`, run the release workflow once in validation-only mode:

```text
source_sha=3887c949ec5d02c87fb5351e1c960b61607bbfd6
rpm_release=1
validation_only=true
```

Required evidence:

- workflow is available from default branch;
- workflow dispatch succeeds;
- controls pass;
- no Gradle build step runs;
- no RPM build runs;
- no artifacts are uploaded.

This validation does not replace the frozen candidate.

---

## 16. Evidence package and acceptance criteria

For every gate, retain:

- commands executed;
- host/environment identity;
- package versions;
- timestamps;
- pass/fail result;
- relevant logs;
- screenshots only where UI state is material;
- SHA-256 for evidence archives.

Do not include private keys, signing passphrases, database secrets, member production payloads or unrestricted credentials.

### Canary Ready acceptance

The milestone **CamDX 7.8.3 RHEL 9 Canary Ready** requires all of the following:

- Package Builder 19/19 verification PASS.
- Controlled signing 19/19 PASS.
- Signed manifest and unsigned-to-signed mapping complete.
- Nexus canary repository created with approved settings.
- Nexus 19/19 round-trip checksum PASS.
- Nexus 19/19 signature PASS.
- Isolated RHEL 9 canary can consume repository metadata/packages.
- PR #18 governance state is understood and no release-control blocker is open.

This milestone authorizes non-production validation only.

### Non-Production Release Candidate acceptance

Requires all Gates N0-N12 PASS, including:

- fresh install;
- in-place upgrade;
- CamDX PKI;
- global configuration security regressions;
- parser/message security regressions;
- registration/RBAC;
- ACME;
- HSM where applicable;
- real API exchange;
- mixed-version coexistence;
- backup/restore;
- demonstrated rollback.

Production promotion remains a separate Director-approved release gate.
