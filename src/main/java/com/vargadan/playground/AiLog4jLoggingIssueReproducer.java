package com.vargadan.playground;

import lombok.extern.log4j.Log4j2;

import static spark.Spark.*;

@Log4j2
public class AiLog4jLoggingIssueReproducer {

    public static void main(String... args) {
        port(8080);
        System.getenv().forEach((k,v) -> log.info("System.ENV : " + k + " = " + v));
        System.getProperties().forEach((k,v) -> log.info("System.Property : " + k + " = " + v));
        log.warn("Started Listening on HTTP port 8080 w/o TLS");
        get("/*", (request, response) -> {
            try {
                var msg = "called : " + request.pathInfo();
                log.info(msg);
                return msg;
            } catch (Throwable e) {
                log.error("Error when fetching git content", e);
                response.status(500);
                return "Error when fetching git content: ".concat(e.getMessage());
            }
        });
    }
}




