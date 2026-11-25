# CamDX 7.8.3-1.el9 Controlled Signing and Nexus Canary Operator Procedure

## Status and authorization boundary

This procedure applies only to the immutable RHEL 9 release candidate built by GitHub Actions run `43543209160`.

Approved identity:

- Product source: `3887c949ec5d02c87fb5351e1c960b61607bbfd6`
- Full CI validation: `43408359021`
- Release build: `43543209160`
- Release NEVR: `7.8.3-1.el9`
- Package count: 19 total, 12 x86_64 and 7 noarch
- GitHub release artifacts: unsigned
- Canary repository: `camdx-rpm-7.8.3-r1-canary`
- Canary asset path: `rhel/9/7.8.3/`

This procedure does not authorize:

- rebuilding or replacing the unsigned candidate;
- signing on Package Builder;
- adding the canary repository to `camdx-release-rpm`;
- modifying any 7.8.2 repository;
- production deployment.

Stop immediately on any failed checksum, unexpected package identity, failed RPM integrity check, unexpected signing identity, Nexus round-trip mismatch, or repository configuration mismatch.

---

## Gate A: Package Builder independent verification

### A1. Prepare workspace

```bash
set -euo pipefail

REPO="Techo-Startup-Center/CamDX"
RUN_ID="43543209160"

BASE="$HOME/engineering/camdx-7.8.3-r1-release"
RPMROOT="$BASE/release-artifacts"
META="$BASE/metadata"
EVIDENCE="$BASE/evidence"

mkdir -p "$RPMROOT/x86_64" "$RPMROOT/noarch" "$META" "$EVIDENCE"

gh auth status
```

### A2. Record GitHub run and artifact provenance

```bash
gh run view "$RUN_ID" -R "$REPO"   --json databaseId,workflowName,headSha,headBranch,event,status,conclusion,createdAt,updatedAt,url   > "$EVIDENCE/github-run.json"

gh api "repos/$REPO/actions/runs/$RUN_ID/artifacts"   > "$EVIDENCE/github-artifacts.json"

jq -e '
  .artifacts
  | map(select(
      .name == "camdx-rhel9-7.8.3-1-x86_64"
      or .name == "camdx-rhel9-7.8.3-1-noarch"
      or .name == "camdx-rhel9-7.8.3-1-metadata"
    ))
  | length == 3
' "$EVIDENCE/github-artifacts.json"

jq -e '
  any(.artifacts[];
    .name == "camdx-rhel9-7.8.3-1-x86_64"
    and .digest == "sha256:ac1c15c6e641ed8e80cbae50c2885d74324d4f3b06dfe198561dc2c047f37365")
  and
  any(.artifacts[];
    .name == "camdx-rhel9-7.8.3-1-noarch"
    and .digest == "sha256:fa9fcf7282b6724115f87f2a35d53dd9b55bb30ed0372d6af4565e6efae9d041")
  and
  any(.artifacts[];
    .name == "camdx-rhel9-7.8.3-1-metadata"
    and .digest == "sha256:cf80625331ad0172630d4584212629f7d14d789ff67d3941446cb26937988f09")
' "$EVIDENCE/github-artifacts.json"
```

Required result: both `jq -e` commands exit 0.

The run head SHA is expected to be the release-workflow execution commit `476f651e52f08c7f50f50758e915b831a185b08d`. The package source identity is verified separately from `BUILD-PROVENANCE.json` below.

### A3. Download exact artifacts

```bash
gh run download "$RUN_ID" -R "$REPO"   -n "camdx-rhel9-7.8.3-1-x86_64"   -D "$RPMROOT/x86_64"

gh run download "$RUN_ID" -R "$REPO"   -n "camdx-rhel9-7.8.3-1-noarch"   -D "$RPMROOT/noarch"

gh run download "$RUN_ID" -R "$REPO"   -n "camdx-rhel9-7.8.3-1-metadata"   -D "$META"
```

Do not obtain RPMs from a branch checkout or rebuild them locally.

### A4. Verify immutable metadata

```bash
(
  cd "$META"
  sha256sum -c METADATA-SHA256SUMS.txt
) | tee "$EVIDENCE/metadata-checksums.txt"

jq -e '
  .approved_source_sha == "3887c949ec5d02c87fb5351e1c960b61607bbfd6"
  and .built_source_sha == "3887c949ec5d02c87fb5351e1c960b61607bbfd6"
  and .workflow_run_id == "43543209160"
  and .expected_version == "7.8.3"
  and .expected_nevr_release == "1.el9"
  and .rpm_count == 19
  and .x86_64_count == 12
  and .noarch_count == 7
  and .github_artifacts_are_signed == false
' "$META/BUILD-PROVENANCE.json"

cp "$META/BUILD-PROVENANCE.json" "$EVIDENCE/BUILD-PROVENANCE-original.json"
```

The original provenance artifact contains an obsolete signing-stage description. Preserve it unchanged. The correction is recorded in `.github/release-control/camdx-rhel9-7.8.3-r1.md`.

### A5. Verify all 19 RPM SHA-256 values

```bash
(
  cd "$RPMROOT"
  sha256sum -c "$META/SHA256SUMS.txt"
) | tee "$EVIDENCE/unsigned-rpm-sha256-verification.txt"
```

Required result: exactly 19 `: OK` lines and zero failures.

### A6. Reconstruct and compare the complete package inventory

```bash
{
  printf 'filename\tname\tversion\trelease\tarch\tsize_bytes\tsha256\n'

  while IFS= read -r rpmfile; do
    filename="$(basename "$rpmfile")"
    name="$(rpm -qp --qf '%{NAME}' "$rpmfile")"
    version="$(rpm -qp --qf '%{VERSION}' "$rpmfile")"
    release="$(rpm -qp --qf '%{RELEASE}' "$rpmfile")"
    arch="$(rpm -qp --qf '%{ARCH}' "$rpmfile")"
    size="$(stat -c '%s' "$rpmfile")"
    sha="$(sha256sum "$rpmfile" | awk '{print $1}')"

    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n'       "$filename" "$name" "$version" "$release" "$arch" "$size" "$sha"
  done < <(find "$RPMROOT" -type f -name '*.rpm' | sort)
} > "$EVIDENCE/PACKAGE-BUILDER-RPM-INVENTORY.tsv"

diff -u   "$META/RPM-INVENTORY.tsv"   "$EVIDENCE/PACKAGE-BUILDER-RPM-INVENTORY.tsv"   | tee "$EVIDENCE/package-inventory-diff.txt"

test "$(find "$RPMROOT" -type f -name '*.rpm' | wc -l)" -eq 19
test "$(find "$RPMROOT/x86_64" -maxdepth 1 -type f -name '*.rpm' | wc -l)" -eq 12
test "$(find "$RPMROOT/noarch" -maxdepth 1 -type f -name '*.rpm' | wc -l)" -eq 7
```

Required result: `diff` exits 0 and the three count assertions exit 0.

### A7. Fail-closed RPM header/payload integrity check

```bash
VERIFY="$EVIDENCE/PACKAGE-BUILDER-RPM-VERIFY.txt"
: > "$VERIFY"

failed=0

while IFS= read -r rpmfile; do
  {
    echo "### $rpmfile"
    if ! rpm -Kv "$rpmfile"; then
      echo "FAILED: $rpmfile"
      failed=1
    fi
    echo
  } >> "$VERIFY" 2>&1
done < <(find "$RPMROOT" -type f -name '*.rpm' | sort)

cat "$VERIFY"
test "$failed" -eq 0

test "$(grep -c '^### ' "$VERIFY")" -eq 19
! grep -Eiq 'FAILED|BAD|NOT OK|NOKEY' "$VERIFY"
```

Gate A passes only when every command above succeeds.

### A8. Freeze Package Builder evidence and transfer set

```bash
(
  cd "$BASE"
  find metadata evidence release-artifacts -type f -print0     | sort -z     | xargs -0 sha256sum
) > "$EVIDENCE/PACKAGE-BUILDER-FULL-SHA256SUMS.txt"

tar -C "$BASE" -czf "$BASE/camdx-7.8.3-r1-verified-unsigned-and-evidence.tgz"   release-artifacts metadata evidence

sha256sum "$BASE/camdx-7.8.3-r1-verified-unsigned-and-evidence.tgz"   > "$BASE/camdx-7.8.3-r1-verified-unsigned-and-evidence.tgz.sha256"
```

Transfer this bundle to the existing controlled signing host using the approved internal transfer mechanism. Do not transfer the private signing key to Package Builder.

---

## Gate B: Controlled signing host

### B1. Verify transferred unsigned evidence before signing

After approved transfer to the signing host:

```bash
set -euo pipefail

BASE="$HOME/engineering/camdx-7.8.3-r1-signing"
BUNDLE="/path/to/camdx-7.8.3-r1-verified-unsigned-and-evidence.tgz"
BUNDLE_SUM="/path/to/camdx-7.8.3-r1-verified-unsigned-and-evidence.tgz.sha256"

mkdir -p "$BASE"

(
  cd "$(dirname "$BUNDLE")"
  sha256sum -c "$(basename "$BUNDLE_SUM")"
)

tar -C "$BASE" -xzf "$BUNDLE"

RPMROOT="$BASE/release-artifacts"
META="$BASE/metadata"
EVIDENCE="$BASE/evidence"

(
  cd "$RPMROOT"
  sha256sum -c "$META/SHA256SUMS.txt"
)

test "$(find "$RPMROOT" -type f -name '*.rpm' | wc -l)" -eq 19
```

### B2. Verify approved signing key identity

```bash
export GNUPGHOME="$HOME/.camdx-rpm-signing/gnupg"

PRIMARY_FPR="B88F7428D21393DFAFCFDC3DA2F01162FAA86C16"
SIGNING_SUBKEY_FPR="464A548B2267196C4F237376AC367CFD31FFE24E"

gpg --batch --with-colons --fingerprint "$PRIMARY_FPR"   > "$BASE/signing-key-fingerprints.txt"

gpg --batch --with-colons --list-secret-keys "$PRIMARY_FPR"   > "$BASE/signing-secret-key-record.txt"

grep -F "$PRIMARY_FPR" "$BASE/signing-key-fingerprints.txt"
grep -F "$SIGNING_SUBKEY_FPR" "$BASE/signing-key-fingerprints.txt"
```

Stop if either approved fingerprint is absent.

### B3. Preserve unsigned originals and sign copies only

```bash
SIGNED="$BASE/signed"

rm -rf "$SIGNED"
mkdir -p "$SIGNED/x86_64" "$SIGNED/noarch"

cp "$RPMROOT/x86_64/"*.rpm "$SIGNED/x86_64/"
cp "$RPMROOT/noarch/"*.rpm "$SIGNED/noarch/"

while IFS= read -r rpmfile; do
  rpmsign     --define "_signature gpg"     --define "_gpg_name $PRIMARY_FPR"     --define "_gpg_digest_algo sha256"     --define "__gpg /usr/bin/gpg"     --addsign "$rpmfile"
done < <(find "$SIGNED" -type f -name '*.rpm' | sort)
```

Interactive key/passphrase handling must follow the existing controlled signing-host procedure.

### B4. Verify all 19 signed RPMs and expected signing identity

```bash
SIGNED_VERIFY="$BASE/camdx-7.8.3-rhel9-r1-SIGNED-RPM-VERIFY.txt"
: > "$SIGNED_VERIFY"

failed=0

while IFS= read -r rpmfile; do
  {
    echo "### $rpmfile"

    if ! rpm -Kv "$rpmfile"; then
      echo "FAILED: $rpmfile"
      failed=1
    fi

    echo
  } >> "$SIGNED_VERIFY" 2>&1
done < <(find "$SIGNED" -type f -name '*.rpm' | sort)

cat "$SIGNED_VERIFY"

test "$failed" -eq 0
test "$(grep -c '^### ' "$SIGNED_VERIFY")" -eq 19
! grep -Eiq 'FAILED|BAD|NOT OK|NOKEY' "$SIGNED_VERIFY"

# RPM commonly reports the signing subkey by the final 8 hex digits.
test "$(grep -Eic 'key ID (31ffe24e|ac367cfd31ffe24e): OK' "$SIGNED_VERIFY")" -ge 19
```

If the local RPM version formats key IDs differently, do not weaken the control. Record the complete `rpm -Kv` output and verify the issuer against the approved signing subkey using the signing host's established RPM verification procedure.

### B5. Generate signed inventory and unsigned-to-signed traceability

```bash
SIGNED_MANIFEST="$BASE/camdx-7.8.3-rhel9-r1-SIGNED-SHA256SUMS.txt"
NEXUS_MANIFEST="$BASE/camdx-7.8.3-rhel9-r1-SIGNED-NEXUS-SHA256SUMS.txt"
MAP="$BASE/camdx-7.8.3-rhel9-r1-UNSIGNED-TO-SIGNED.tsv"

(
  cd "$SIGNED"
  find x86_64 noarch -type f -name '*.rpm' -print0     | sort -z     | xargs -0 sha256sum
) > "$SIGNED_MANIFEST"

while IFS= read -r rpmfile; do
  sha="$(sha256sum "$rpmfile" | awk '{print $1}')"
  printf '%s  %s\n' "$sha" "$(basename "$rpmfile")"
done < <(find "$SIGNED" -type f -name '*.rpm' | sort)   > "$NEXUS_MANIFEST"

printf 'filename\tname\tversion\trelease\tarch\tunsigned_sha256\tsigned_sha256\n' > "$MAP"

while IFS= read -r signed_file; do
  filename="$(basename "$signed_file")"
  unsigned_file="$(find "$RPMROOT" -type f -name "$filename" -print -quit)"

  test -n "$unsigned_file"

  name="$(rpm -qp --qf '%{NAME}' "$signed_file")"
  version="$(rpm -qp --qf '%{VERSION}' "$signed_file")"
  release="$(rpm -qp --qf '%{RELEASE}' "$signed_file")"
  arch="$(rpm -qp --qf '%{ARCH}' "$signed_file")"
  unsigned_sha="$(sha256sum "$unsigned_file" | awk '{print $1}')"
  signed_sha="$(sha256sum "$signed_file" | awk '{print $1}')"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n'     "$filename" "$name" "$version" "$release" "$arch" "$unsigned_sha" "$signed_sha"     >> "$MAP"
done < <(find "$SIGNED" -type f -name '*.rpm' | sort)

test "$(wc -l < "$MAP")" -eq 20

sha256sum   "$SIGNED_MANIFEST"   "$NEXUS_MANIFEST"   "$MAP"   "$SIGNED_VERIFY"   > "$BASE/camdx-7.8.3-rhel9-r1-SIGNING-EVIDENCE-SHA256SUMS.txt"
```

Gate B passes only after the 19/19 signature checks and signed evidence complete successfully.

---

## Gate C: Create isolated Nexus Yum canary

Run from the authorized Nexus administration host/session.

```bash
set -euo pipefail

CURLCFG="$HOME/.config/camdx/nexus-curl.conf"
NEXUS="https://repository.camdx.gov.kh"

SOURCE_REPO="camdx-rpm-7.8.2-r2-canary"
CANARY_REPO="camdx-rpm-7.8.3-r1-canary"

WORK="$HOME/engineering/camdx-7.8.3-r1-nexus"
mkdir -p "$WORK"
```

### C1. Confirm the target does not already exist

```bash
status="$(
  curl --config "$CURLCFG"     --silent --show-error     --output "$WORK/target-repo-existing.json"     --write-out '%{http_code}'     "$NEXUS/service/rest/v1/repositories/yum/hosted/$CANARY_REPO"
)"

case "$status" in
  404) ;;
  200)
    echo "STOP: $CANARY_REPO already exists. Review it instead of overwriting." >&2
    exit 1
    ;;
  *)
    echo "STOP: unexpected Nexus response: HTTP $status" >&2
    exit 1
    ;;
esac
```

### C2. Read the approved 7.8.2 canary configuration

```bash
curl --config "$CURLCFG"   --fail-with-body --silent --show-error   "$NEXUS/service/rest/v1/repositories/yum/hosted/$SOURCE_REPO"   > "$WORK/source-repo.json"

jq . "$WORK/source-repo.json"

jq -e '
  .yum.repodataDepth == 3
  and .storage.strictContentTypeValidation == true
  and .storage.writePolicy == "ALLOW_ONCE"
' "$WORK/source-repo.json"
```

If the established source repository does not satisfy these controls, stop and obtain repository-owner review. Do not silently create the new canary with different settings.

### C3. Build a new-repository request from the approved settings

```bash
jq --arg name "$CANARY_REPO" '{
  name: $name,
  online: .online,
  storage: {
    blobStoreName: .storage.blobStoreName,
    strictContentTypeValidation: .storage.strictContentTypeValidation,
    writePolicy: .storage.writePolicy
  },
  cleanup: {
    policyNames: (.cleanup.policyNames // [])
  },
  component: {
    proprietaryComponents: (.component.proprietaryComponents // false)
  },
  yum: {
    repodataDepth: .yum.repodataDepth
  }
}' "$WORK/source-repo.json"   > "$WORK/create-canary-repo.json"

jq . "$WORK/create-canary-repo.json"

jq -e '
  .name == "camdx-rpm-7.8.3-r1-canary"
  and .yum.repodataDepth == 3
  and .storage.strictContentTypeValidation == true
  and .storage.writePolicy == "ALLOW_ONCE"
' "$WORK/create-canary-repo.json"
```

### C4. Create the isolated repository

```bash
curl --config "$CURLCFG"   --fail-with-body --silent --show-error   -H 'Content-Type: application/json'   -X POST   --data-binary "@$WORK/create-canary-repo.json"   "$NEXUS/service/rest/v1/repositories/yum/hosted"

curl --config "$CURLCFG"   --fail-with-body --silent --show-error   "$NEXUS/service/rest/v1/repositories/yum/hosted/$CANARY_REPO"   > "$WORK/created-canary-repo.json"

jq . "$WORK/created-canary-repo.json"

jq -e '
  .name == "camdx-rpm-7.8.3-r1-canary"
  and .yum.repodataDepth == 3
  and .storage.strictContentTypeValidation == true
  and .storage.writePolicy == "ALLOW_ONCE"
' "$WORK/created-canary-repo.json"
```

Do not modify any group membership.

---

## Gate D: Publish only signed RPMs to the canary

Assume the signed directory and signed evidence have been transferred from the signing host using the approved internal mechanism and re-verified after transfer.

```bash
set -euo pipefail

CURLCFG="$HOME/.config/camdx/nexus-curl.conf"
NEXUS="https://repository.camdx.gov.kh"
CANARY_REPO="camdx-rpm-7.8.3-r1-canary"
DEST="$NEXUS/repository/$CANARY_REPO/rhel/9/7.8.3"

SIGNED="/path/to/verified/signed"
NEXUS_MANIFEST="/path/to/camdx-7.8.3-rhel9-r1-SIGNED-NEXUS-SHA256SUMS.txt"

test "$(find "$SIGNED" -type f -name '*.rpm' | wc -l)" -eq 19
```

Upload:

```bash
while IFS= read -r rpmfile; do
  name="$(basename "$rpmfile")"

  curl --config "$CURLCFG"     --fail-with-body --silent --show-error     --upload-file "$rpmfile"     "$DEST/$name"
done < <(find "$SIGNED" -type f -name '*.rpm' | sort)
```

With deploy-once behavior, any duplicate-path rejection is a stop condition. Do not delete/re-upload or overwrite packages to make the upload succeed.

### D1. Wait for Yum metadata

```bash
metadata_ok=0

for attempt in $(seq 1 30); do
  status="$(
    curl --silent --show-error       --output /dev/null       --write-out '%{http_code}'       "$DEST/repodata/repomd.xml" || true
  )"

  if [[ "$status" == "200" ]]; then
    metadata_ok=1
    break
  fi

  sleep 10
done

test "$metadata_ok" -eq 1

curl -fsSL "$DEST/repodata/repomd.xml"   -o "$WORK/repomd.xml"

sha256sum "$WORK/repomd.xml"   > "$WORK/repomd.xml.sha256"
```

---

## Gate E: Nexus round-trip verification

### E1. Download all 19 published RPMs into a clean directory

```bash
ROUNDTRIP="$WORK/roundtrip"

rm -rf "$ROUNDTRIP"
mkdir -p "$ROUNDTRIP"

while read -r expected_sha filename; do
  curl --fail-with-body --silent --show-error     "$DEST/$filename"     -o "$ROUNDTRIP/$filename"
done < "$NEXUS_MANIFEST"

test "$(find "$ROUNDTRIP" -maxdepth 1 -type f -name '*.rpm' | wc -l)" -eq 19
```

### E2. Verify returned bytes against signed manifest

```bash
(
  cd "$ROUNDTRIP"
  sha256sum -c "$NEXUS_MANIFEST"
) | tee "$WORK/NEXUS-ROUNDTRIP-SHA256-VERIFY.txt"
```

Required: 19/19 `: OK`.

### E3. Verify all returned RPM signatures

```bash
ROUNDTRIP_VERIFY="$WORK/NEXUS-ROUNDTRIP-RPM-VERIFY.txt"
: > "$ROUNDTRIP_VERIFY"

failed=0

while IFS= read -r rpmfile; do
  {
    echo "### $rpmfile"
    if ! rpm -Kv "$rpmfile"; then
      echo "FAILED: $rpmfile"
      failed=1
    fi
    echo
  } >> "$ROUNDTRIP_VERIFY" 2>&1
done < <(find "$ROUNDTRIP" -type f -name '*.rpm' | sort)

cat "$ROUNDTRIP_VERIFY"

test "$failed" -eq 0
test "$(grep -c '^### ' "$ROUNDTRIP_VERIFY")" -eq 19
! grep -Eiq 'FAILED|BAD|NOT OK|NOKEY' "$ROUNDTRIP_VERIFY"
test "$(grep -Eic 'key ID (31ffe24e|ac367cfd31ffe24e): OK' "$ROUNDTRIP_VERIFY")" -ge 19
```

Gate E passes only with exact signed-byte equality and signature verification for all 19 packages.

---

## Gate F: Isolated RHEL 9 repository usability check

This is a repository-consumption check, not the full non-production installation test.

Use an isolated RHEL 9 canary VM with the approved RHEL RPM public signing key already fingerprint-verified and installed at:

`/etc/pki/rpm-gpg/RPM-GPG-KEY-camdx-rhel9`

Create a canary-only repository file:

```bash
sudo tee /etc/yum.repos.d/camdx-7.8.3-canary.repo >/dev/null <<'EOF'
[camdx-7.8.3-canary]
name=CamDX 7.8.3 RHEL 9 Canary
baseurl=https://repository.camdx.gov.kh/repository/camdx-rpm-7.8.3-r1-canary/rhel/9/7.8.3
enabled=1
gpgcheck=1
repo_gpgcheck=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-camdx-rhel9
metadata_expire=0
EOF

sudo dnf clean metadata

sudo dnf   --disablerepo='*'   --enablerepo='camdx-7.8.3-canary'   makecache

sudo dnf   --disablerepo='*'   --enablerepo='camdx-7.8.3-canary'   repoquery   --qf '%{name}\t%{version}-%{release}\t%{arch}'   'xroad-*'   | sort   | tee "$HOME/camdx-7.8.3-canary-repoquery.txt"
```

Confirm the repository exposes the intended 19-package set and `7.8.3-1.el9` identities.

Do not install onto an operational CamDX server.

---

## Evidence required to declare "Canary Published and Verified"

Return the following evidence for independent review:

### Package Builder

- `github-run.json`
- `github-artifacts.json`
- `metadata-checksums.txt`
- `BUILD-PROVENANCE-original.json`
- `unsigned-rpm-sha256-verification.txt`
- `PACKAGE-BUILDER-RPM-INVENTORY.tsv`
- `package-inventory-diff.txt`
- `PACKAGE-BUILDER-RPM-VERIFY.txt`
- `PACKAGE-BUILDER-FULL-SHA256SUMS.txt`
- verified bundle SHA-256

### Signing host

- `signing-key-fingerprints.txt`
- `signing-secret-key-record.txt` with secret material excluded; metadata only
- `camdx-7.8.3-rhel9-r1-SIGNED-RPM-VERIFY.txt`
- `camdx-7.8.3-rhel9-r1-SIGNED-SHA256SUMS.txt`
- `camdx-7.8.3-rhel9-r1-SIGNED-NEXUS-SHA256SUMS.txt`
- `camdx-7.8.3-rhel9-r1-UNSIGNED-TO-SIGNED.tsv`
- `camdx-7.8.3-rhel9-r1-SIGNING-EVIDENCE-SHA256SUMS.txt`

Do not capture or share private key material or passphrases.

### Nexus / round trip

- `source-repo.json`
- `create-canary-repo.json`
- `created-canary-repo.json`
- `repomd.xml` and SHA-256
- `NEXUS-ROUNDTRIP-SHA256-VERIFY.txt`
- `NEXUS-ROUNDTRIP-RPM-VERIFY.txt`
- isolated RHEL 9 `dnf makecache` result
- `camdx-7.8.3-canary-repoquery.txt`

Only after these gates pass should the RHEL 9 non-production installation/upgrade validation begin.
