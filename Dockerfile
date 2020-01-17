FROM adoptopenjdk/openjdk11:alpine-jre

RUN mkdir -p /usr/local/mock-server/dependency-jars

ADD ./target/mock-server-1.0-SNAPSHOT.jar /usr/local/mock-server/goco-mock-server.jar
ADD ./target/dependency-jars/** /usr/local/mock-server/dependency-jars/

EXPOSE 1080

ENTRYPOINT ["java", "-jar", "/usr/local/mock-server/goco-mock-server.jar"]