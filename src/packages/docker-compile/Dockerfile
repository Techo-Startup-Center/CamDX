# ----------------------------------------
# Base image
# ----------------------------------------
FROM ubuntu:24.04

# ----------------------------------------
# Set locale
# ----------------------------------------
RUN apt-get clean && apt-get -y update && apt-get install -y locales && locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

ENV DEBIAN_FRONTEND=noninteractive

# ----------------------------------------
# Install required packages
# ----------------------------------------
RUN apt-get -qq update \
    && apt-get -qq upgrade \
    && apt-get -qq install -y \
       wget \
       unzip \
       ca-certificates \
       software-properties-common \
       gawk \
       openjdk-21-jdk-headless \
       build-essential \
       git \
       debhelper \
    && apt-get -qq autoremove \
    && apt-get -qq clean

# ----------------------------------------
# Create builder user
# ----------------------------------------
ARG uid=1000
ARG gid=1000
RUN groupadd builder \
    && useradd -m -g builder builder \
    && mkdir -p /mnt/gradle-cache \
    && chown -R builder:builder /mnt/gradle-cache

USER builder
WORKDIR /mnt
ENV GRADLE_USER_HOME=/mnt/gradle-cache
ENV GRADLE_VERSION=9.0.0

# ----------------------------------------
# Pre-download Gradle using wget
# ----------------------------------------
USER root
RUN mkdir -p $GRADLE_USER_HOME \
    && wget --no-check-certificate -q --show-progress --progress=bar:force:noscroll \
       https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
       -O $GRADLE_USER_HOME/gradle-${GRADLE_VERSION}-bin.zip \
    && unzip $GRADLE_USER_HOME/gradle-${GRADLE_VERSION}-bin.zip -d $GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION} \
    && rm $GRADLE_USER_HOME/gradle-${GRADLE_VERSION}-bin.zip

# ----------------------------------------
# Set Gradle PATH
# ----------------------------------------
ENV PATH=$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}/gradle-${GRADLE_VERSION}/bin:$PATH

# ----------------------------------------
# Verify Gradle installation
# ----------------------------------------
USER builder
RUN gradle --version
