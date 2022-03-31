FROM ubuntu
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update
RUN apt-get purge openjdk*
RUN apt-get install -y openjdk-11-jdk iputils-ping net-tools ntp sysstat
