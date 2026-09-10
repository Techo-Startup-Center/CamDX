# do not repack jars
%define __jar_repack %{nil}
# produce .elX dist tag on both centos and redhat
%define dist %(/usr/lib/rpm/redhat/dist.sh)

# compare check assumes versions like "major.minor.bugfix-release", compares only major.minor parts
# also last_supported_version macro needs to be defined
%define upgrade_check \
version_gt() { \
    local major1 minor1 fix1 rel1 \
    local major2 minor2 fix2 rel2 \
    IFS=".- " read -r major1 minor1 fix1 rel1 <<<"$1" \
    IFS=".- " read -r major2 minor2 fix2 rel2 <<<"$2" \
    if [[ $major1 -gt $major2 || ( $major1 -eq $major2 && $minor1 -gt $minor2 ) ]]; then \
      return 0 \
    fi \
    return 1 \
} \
if [ "$1" -gt 1 ] ; then \
    # upgrade \
    installed_version=$(rpm -q "%name" --queryformat="%%{version}") \
    if version_gt "%{last_supported_version}" "$installed_version"; then \
      echo "ERROR: Upgrade supported from version %{last_supported_version} or newer." >&2\
      exit 1 \
    fi \
fi \
%{nil}

%define systemd_try_restart() \
systemctl --quiet try-restart %{?*} >/dev/null 2>&1 || : \
%{nil}

# RHEL9-PKG-02 fix. Default RPM "defattr(dash,root,root,dash)" style entries with no
# explicit attr override are packaged with whatever mode they happen to have in the
# buildroot when the install step finishes. Unlike Debian's dh_fixperms (which
# unconditionally normalizes packaged permissions regardless of the source checkout),
# rpmbuild has no equivalent safety net. A build or checkout environment with a
# restrictive umask then produces group or world inaccessible RPM payloads, that runtime
# service accounts such as xroad cannot traverse, execute, or read, independent of
# anything a spec's own file-list attr overrides can fix, since the install step's copy
# commands already staged the too restrictive mode before those overrides are applied.
# Invoking this macro as the last step of each package's install section restores the
# canonical baseline already proven in this same source's working Debian packaging for
# the identical files: root owned, directories and executables 0755, other files 0644.
# Git itself only ever tracks the executable bit, two possible modes for a regular file,
# so this also exactly restores the checkout umask independent, git intended permissions.
# It never weakens the tighter overrides that specs already declare in their file list for
# secrets or writable operational state, since those are applied by RPM at packaging time
# and always take precedence over whatever mode a file has on disk in the buildroot.
%define normalize_perms() \
find %{buildroot} -type d -exec chmod 0755 {} + ; \
find %{buildroot} -type f -perm -u+x -exec chmod 0755 {} + ; \
find %{buildroot} -type f ! -perm -u+x -exec chmod 0644 {} + \
%{nil}
