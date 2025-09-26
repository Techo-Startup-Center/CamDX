FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get -qq update \
  && apt-get -qq upgrade \
  && apt-get -qq install -y curl software-properties-common gawk \
    openjdk-21-jdk-headless build-essential git unzip debhelper

# Define default UID/GID
ARG uid=1000
ARG gid=1000

# Delete existing builder user/group if they exist (prevents conflicts)
RUN groupdel builder || true \
    && userdel builder || true \
    && groupadd -g ${gid} builder \
    && useradd -m -u ${uid} -g ${gid} builder \
    && mkdir -p /mnt/gradle-cache \
    && chown -R builder:builder /mnt/gradle-cache

USER builder ENV GRADLE_USER_HOME /mnt/gradle-cache RUN mkdir -p /var/tmp/xroad WORKDIR /mnt
