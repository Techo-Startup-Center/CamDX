#!/bin/bash
#
# Database setup
#

is_rhel8or9() {
    ([[ -f /etc/os-release ]] && source /etc/os-release && [[ "$ID" == *rhel* && ("$VERSION_ID" == 8* || "$VERSION_ID" == 9*) ]])
}

get_prop() {
  local tmp="$(crudini --get "$1" '' "$2" 2>/dev/null)"
  echo "${tmp:-$3}"
}

init_local_postgres() {
    if [ -f /etc/xroad/xroad.properties ]; then
      local -r root_properties=/etc/xroad/xroad.properties
    else
      local -r root_properties=/etc/xroad.properties
    fi
    SERVICE_NAME=postgresql

    if [[ -f ${root_properties} && $(get_prop ${root_properties} postgres.connection.password) != "" ]]; then
      # using remote db
      return 0
    fi

    # check if postgres is already running
    systemctl -q is-active $SERVICE_NAME && return 0

    # Copied from postgresql-setup. Determine default data directory
    PGDATA=$(systemctl -q show -p Environment "${SERVICE_NAME}.service" | sed 's/^Environment=//' | tr ' ' '\n' | sed -n 's/^PGDATA=//p' | tail -n 1)
    if [ -z "$PGDATA" ]; then
        echo "failed to find PGDATA setting in ${SERVICE_NAME}.service"
        return 1
    fi

    if [ ! -e "$PGDATA/PG_VERSION" ]; then
        if is_rhel8or9; then
            cmd="--initdb"
        else
            cmd="initdb"
        fi
        PGSETUP_INITDB_OPTIONS="--auth-host=md5 -E UTF8" postgresql-setup $cmd || return 1
    fi

    # RHEL9-PKG-01: on a fresh install, the postgresql-server package (and its
    # /usr/lib/tmpfiles.d/postgresql.conf rule declaring "d /run/postgresql 0755 postgres
    # postgres -") has just been installed by this same transaction. systemd only applies
    # tmpfiles.d rules at boot (systemd-tmpfiles-setup.service) or when explicitly asked to,
    # so /run/postgresql does not exist yet even though the rule is now on disk. Starting
    # PostgreSQL before that directory exists fails with:
    #   FATAL: could not create lock file "/var/run/postgresql/.s.PGSQL.5432.lock": No such
    #   file or directory
    # Explicitly (re-)apply all tmpfiles.d rules first so the runtime directory exists
    # before the first service start, exactly as a reboot would have done.
    if command -v systemd-tmpfiles &>/dev/null; then
        systemd-tmpfiles --create &>/dev/null || true
    fi
    # Defensive fallback matching the exact ownership/mode from postgresql's own
    # tmpfiles.d rule, in case tmpfiles processing above did not create it for any reason.
    if [ ! -d /run/postgresql ]; then
        mkdir -p /run/postgresql
        chown postgres:postgres /run/postgresql
        chmod 0755 /run/postgresql
    fi

    # ensure that PostgreSQL is running
    systemctl start $SERVICE_NAME || return 1

    # Defense in depth: postgresql.service is Type=notify, so a successful "systemctl
    # start" should already mean PostgreSQL is accepting connections. Confirm this
    # explicitly with a bounded readiness poll (not a fixed sleep) before handing off to
    # DB provisioning, so any other startup-timing variance is caught here rather than
    # surfacing as a confusing failure in setup_serverconf_db.sh/setup_messagelog_db.sh.
    local -i attempt
    for ((attempt = 1; attempt <= 10; attempt++)); do
        pg_isready -q && return 0
        sleep 1
    done
    echo "PostgreSQL service started but did not become ready to accept connections"
    return 1
}

init_local_postgres
