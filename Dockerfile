FROM adoptopenjdk/openjdk11:alpine-jre

RUN mkdir -p /usr/local/mockserver/dependency-jars

ADD ./target/mockserver-?.?*.jar /usr/local/mockserver/mockserver.jar
ADD ./target/dependency-jars/** /usr/local/mockserver/dependency-jars/

EXPOSE 80

ENV BITBUCKET_CLIENT_ID=gJ6cZsm2wDbcDYmtdg
ENV BITBUCKET_CLIENT_SECRET=ASp3r5ceNUCEZAy6bb9sjBt7juDZJm5t

ENTRYPOINT ["java", "-jar", "/usr/local/mockserver/mockserver.jar", "80"]