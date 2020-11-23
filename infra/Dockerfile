FROM adoptopenjdk/openjdk11:alpine-jre

RUN mkdir -p /usr/local/mockserver/dependency-jars

ADD ./target/ai-log4j-reproducer-0.1-SNAPSHOT.jar /usr/local/reproducer/reproducer.jar
ADD ./target/dependency-jars/** /usr/local/reproducer/dependency-jars/

ADD ./infra/start.sh /usr/local/reproducer/start.sh
RUN chmod +x /usr/local/reproducer/start.sh

EXPOSE 8080

ENTRYPOINT ["/usr/local/reproducer/start.sh"]