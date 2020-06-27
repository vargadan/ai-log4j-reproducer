package com.gocompliant.mockserver;

import lombok.extern.slf4j.Slf4j;

import static spark.Spark.*;

@Slf4j
public class SparkServerRunner {

    static GitAccess gitAccess = new GitAccess();

    public static void main(String... args) {
//        secure("", "", "", "", true);
        int port = 1080;
        if (args != null && args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        port(port);
        get("/oauth2/callback", (request, response) -> "OK");
        get("/reset", (request, response) -> {
            try {
                gitAccess = new GitAccess();
                response.status(200);
                return "git access has been reset";
            } catch (Throwable e) {
                log.error("Error when fetching git content", e);
                response.status(500);
                return "Error when fetching git content: ".concat(e.getMessage());
            }
        });
        get("/*", (request, response) -> {
            try {
                String jsonBody = gitAccess.request.apply(request.pathInfo()).body();
                response.header("Content-Type", "application/json; charset=utf-8");
                response.status(200);
                return jsonBody;
            } catch (Throwable e) {
                log.error("Error when fetching git content", e);
                response.status(500);
                return "Error when fetching git content: ".concat(e.getMessage());
            }
        });

    }
}




