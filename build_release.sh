#!/bin/sh

BUILD_IMAGE=xroad-package-build:1.0
PKG_DEB_IMAGE=xroad-package-deb:1.0
PKG_DEB_FOCAL_IMAGE=xroad-package-deb-focal:1.0
PKG_RPM_IMAGE=xroad-package-rpm:1.0

cd "`dirname "$0"`"

BUILDUSER_UID="`ls -nd . | awk '{ print $3 }'`"
BUILDUSER_GID="`ls -nd . | awk '{ print $4 }'`"
#echo "Build user has UID '$BUILDUSER_UID' and GID '$BUILDUSER_GID'."

errorExit() {
	echo "*** $*" 1>&2
	exit 1
}

buildBuildImage() {
	echo "Build image '$BUILD_IMAGE' does not yet exist, building it now."
	( cd src/packages/docker-compile && pwd && docker build --build-arg uid=$BUILDUSER_UID --build-arg gid=$BUILDUSER_GID -t $BUILD_IMAGE . )
}
buildPkgDebImage() {
	echo "Package deb image '$BUILD_IMAGE' does not yet exist, building it now."
	( cd src/packages/docker/deb-bionic/ && pwd && docker build -t $PKG_DEB_IMAGE . )
}
buildPkgDebFocalImage() {
	echo "Package deb image '$BUILD_IMAGE' does not yet exist, building it now."
	( cd src/packages/docker/deb-focal/ && pwd && docker build -t $PKG_DEB_FOCAL_IMAGE . )
}
buildPkgRpmImage() {
	echo "Package rpm image '$BUILD_IMAGE' does not yet exist, building it now."
	( cd src/packages/docker/rpm/ && pwd && docker build -t $PKG_RPM_IMAGE . )
}

echo "$BUILDUSER_UID" | grep -q '^[0-9]\+$' || errorExit "Error determining build user UID, got '$BUILDUSER_UID'."
echo "$BUILDUSER_GID" | grep -q '^[0-9]\+$' || errorExit "Error determining build user GID, got '$BUILDUSER_GID'."
which docker >/dev/null 2>&1 || errorExit "Error, docker is not available."


IMAGES="`docker images --format "{{.Repository}}:{{.Tag}}"`"
test $? = 0 || errorExit "Error getting docker image list"

echo "$IMAGES" | grep -q "^${BUILD_IMAGE}$" || buildBuildImage || errorExit "Error building build image."
echo "$IMAGES" | grep -q "^${PKG_DEB_IMAGE}$" || buildPkgDebImage || errorExit "Error building pkg deb image."
echo "$IMAGES" | grep -q "^${PKG_DEB_FOCAL_IMAGE}$" || buildPkgDebFocalImage || errorExit "Error building pkg deb focal image."
echo "$IMAGES" | grep -q "^${PKG_RPM_IMAGE}$" || buildPkgRpmImage || errorExit "Error building pkg rpm image."

make -C src clean || /bin/true
trap "docker run -v `pwd`:`pwd` -w `pwd` -u root ${BUILD_IMAGE} chown -R ${BUILDUSER_UID}:${BUILDUSER_GID} src/packages/build 2>/dev/null || /bin/true" EXIT

echo
echo "Step 1: build of binaries..."
echo
docker run -u builder -v `pwd`:`pwd` -w `pwd`/src "$BUILD_IMAGE" bash -c "./update_ruby_dependencies.sh && ./compile_code.sh -nodaemon" || errorExit "Error running build of binaries."
echo "Done: build of binaries."
echo

echo
echo "Step 2: build of rpm packages..."
echo
docker run -v `pwd`:`pwd` -w `pwd` "$PKG_RPM_IMAGE" bash -c "./src/packages/build-rpm.sh -release" || errorExit "Error running build of rpm packages."
echo "Done: build of rpm packages."
echo

echo
echo "Step 3: build of deb packages for ubuntu 18.04..."
echo
docker run -v `pwd`:`pwd` -w `pwd` "$PKG_DEB_IMAGE" bash -c "./src/packages/build-deb.sh bionic -release" || errorExit "Error running build of deb packages."
echo "Done: build of deb packages."
echo

echo
echo "Step 4: build of deb packages for ubuntu 20.04..."
echo
docker run -v `pwd`:`pwd` -w `pwd` "$PKG_DEB_FOCAL_IMAGE" bash -c "./src/packages/build-deb.sh focal -release" || errorExit "Error running build of deb packages."
echo "Done: build of deb packages."
echo
