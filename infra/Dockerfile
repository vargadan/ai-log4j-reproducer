FROM adoptopenjdk/openjdk11:alpine-jre

RUN mkdir -p /usr/local/mockserver/dependency-jars

ADD ./target/mockservice-server-*.jar /usr/local/mockserver/mockserver.jar
ADD ./target/dependency-jars/** /usr/local/mockserver/dependency-jars/

EXPOSE 80

ENTRYPOINT ["java", "-jar", "/usr/local/mockserver/mockserver.jar", "80"]