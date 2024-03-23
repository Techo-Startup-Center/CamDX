#!/bin/bash
source compile_env.sh

RELEASE="SNAPSHOT"

for i in "$@"; do
case "$i" in
    "-release")
        RELEASE="RELEASE"
        ;;
    "sonar"|"-sonar")
        SONAR=1
        ;;
    "-nodaemon")
        NODAEMON=1
        ;;
esac
done

# ARGUMENTS=("-PxroadBuildType=$RELEASE" --stacktrace build runProxyTest runMetaserviceTest runProxymonitorMetaserviceTest)
ARGUMENTS=("-PxroadBuildType=RELEASE" --stacktrace build runProxyTest runMetaserviceTest runProxymonitorMetaserviceTest -x :proxy:intTest -x :signer:intTest -x :central-server:admin-service:int-test:intTest -x :central-server:management-service:int-test:intTest -x :security-server:admin-service:int-test:intTest)

if [[ -n "$SONAR" ]]; then
    ARGUMENTS+=(dependencyCheckAnalyze sonarqube)
fi

if [[ -n "$NODAEMON" ]]; then
    ARGUMENTS+=(--no-daemon)
fi

./gradlew "${ARGUMENTS[@]}"

rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi
