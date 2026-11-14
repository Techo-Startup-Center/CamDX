# CamDX RHEL 9 7.8.3-1 Release Control Record

## Candidate identity

- Approved product source: `3887c949ec5d02c87fb5351e1c960b61607bbfd6`
- Full CI validation run: `43408359021`
- Controlled release-packaging run: `43543209160`
- Release NEVR: `7.8.3-1.el9`
- Package set: 19 RPMs, 12 x86_64 and 7 noarch
- Candidate artifacts: unsigned and immutable after GitHub build
- No production publication or deployment is authorized by this record.

## Workflow lineage

Release run `43543209160` executed the release workflow from commit:

`476f651e52f08c7f50f50758e915b831a185b08d`

The package payload was built from the separately pinned approved source:

`3887c949ec5d02c87fb5351e1c960b61607bbfd6`

After the successful release run:

1. `a268eb7aa77f99015c3897eacd11115b2e760fb7` removed the temporary push trigger and made the release workflow manual-only.
2. `e88e0f9317c931d77ce96e3547c3159508a4b26f` added validation-only dispatch, corrected signing-stage provenance, and made RPM verification failures release-blocking.

These workflow-control commits do not modify the candidate RPM bytes from run `43543209160`.

## Provenance correction for run 43543209160

The immutable `BUILD-PROVENANCE.json` artifact from run `43543209160` contains:

`"signing_stage": "Package Builder VM after artifact retrieval and checksum verification"`

That description is incorrect and is superseded by this release-control record.

The approved process is:

1. Package Builder retrieves the exact GitHub artifacts and independently verifies inventory and checksums.
2. Package Builder does not rebuild or sign the RPMs.
3. Signing occurs on the existing controlled signing host using the established CamDX RPM signing identity.
4. Signed RPMs are published only to the isolated 7.8.3 canary repository.
5. Promotion to the member-facing repository group requires a separate release approval after non-production validation.

The original GitHub metadata artifact remains unchanged to preserve its immutable provenance and audit trail.

## Pre-signing verification assessment

The release workflow used for run `43543209160` captured `rpm -Kv` output but used `|| true`, which could have suppressed a non-zero verification exit code.

Review of the immutable `PRE-SIGNING-RPM-VERIFY.txt` from that run found:

- 19 package verification blocks
- 19/19 Header SHA256 digest: OK
- 19/19 Header SHA1 digest: OK
- 19/19 Payload SHA256 digest: OK
- 19/19 MD5 digest: OK
- no FAILED, BAD, NOT OK, or NOKEY result in the captured report

This supports continued use of the existing candidate, but does not justify keeping the suppression behavior.

The final workflow removes `|| true`, records every package result, and exits non-zero before artifact upload if any `rpm -Kv` invocation fails.

## Independent artifact verification

The metadata artifact from run `43543209160` was independently re-hashed and all four metadata files matched `METADATA-SHA256SUMS.txt`.

The seven noarch RPMs were independently retrieved and all seven matched the SHA-256 values recorded in `SHA256SUMS.txt`.

The x86_64 artifact is approximately 581 MB. Independent verification of the 12 x86_64 RPMs must be completed on the Package Builder using the exact GitHub artifact before signing. Signing must not proceed unless all 19 RPMs match the immutable GitHub checksum manifest.

## Final manual-only workflow validation

The final workflow remains under normal review in PR #18 and is not to be merged outside the normal review process.

After the workflow is merged to the repository default branch, perform one manual validation-only dispatch using:

- `source_sha=3887c949ec5d02c87fb5351e1c960b61607bbfd6`
- `rpm_release=1`
- `validation_only=true`

Validation-only mode checks:

- workflow is manual-only
- RPM verification failure suppression is absent
- controlled-signing-host provenance wording is present
- approved source SHA matches the pinned release source
- release version is `7.8.3`

Validation-only mode does not run Gradle, rebuild RPMs, or upload artifacts.

A normal package build must not be dispatched merely to validate these control changes.

## Canary boundary

The intended canary repository is:

`camdx-rpm-7.8.3-r1-canary`

with release assets under:

`rhel/9/7.8.3/`

The existing member-facing `camdx-release-rpm` group must remain unchanged until the non-production canary passes and a separate release decision is recorded.
