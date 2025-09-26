FROM ubuntu:24.04

# Set locale
RUN apt-get clean && apt-get -y update && apt-get install -y locales && locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

ENV DEBIAN_FRONTEND=noninteractive
ENV GRADLE_DOWNLOAD_TIMEOUT=600000
ENV GRADLE_VERSION=9.0.0

# Install required packages
RUN apt-get -qq update \
  && apt-get -qq upgrade \
  && apt-get -qq install -y curl software-properties-common gawk \
     openjdk-21-jdk-headless build-essential git unzip debhelper \
  && apt-get -qq autoremove \
  && apt-get -qq clean

# Create builder user
ARG uid=1000
ARG gid=1000

RUN groupadd builder \
    && useradd -m -g builder builder \
    && mkdir -p /mnt/gradle-cache \
    && chown -R builder:builder /mnt/gradle-cache

USER builder
WORKDIR /mnt
ENV GRADLE_USER_HOME=/mnt/gradle-cache

# --- Add network connectivity check here ---
USER root
RUN apt-get update && apt-get install -y iputils-ping curl \
    && echo "Checking network connectivity..." \
    && ping -c 2 8.8.8.8 \
    && ping -c 2 google.com \
    && curl -I https://services.gradle.org

USER builder

# Pre-download Gradle distribution for offline builds
RUN curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o $GRADLE_USER_HOME/gradle-${GRADLE_VERSION}-bin.zip \
    && mkdir -p $GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION} \
    && unzip $GRADLE_USER_HOME/gradle-${GRADLE_VERSION}-bin.zip -d $GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION} \
    && rm $GRADLE_USER_HOME/gradle-${GRADLE_VERSION}-bin.zip

# Add Gradle to PATH
ENV PATH=$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}/gradle-${GRADLE_VERSION}/bin:$PATH
