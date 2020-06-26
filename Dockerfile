FROM adoptopenjdk/openjdk11:alpine-jre

RUN mkdir -p /usr/local/mockserver/dependency-jars

ADD ./target/mockserver-?.?*.jar /usr/local/mockserver/mockserver.jar
ADD ./target/dependency-jars/** /usr/local/mockserver/dependency-jars/

EXPOSE 1080

ENTRYPOINT ["java", "-jar", "/usr/local/mockserver/mockserver.jar"]