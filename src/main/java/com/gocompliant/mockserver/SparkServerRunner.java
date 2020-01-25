package com.gocompliant.mockserver;

import lombok.extern.slf4j.Slf4j;

import static spark.Spark.*;

@Slf4j
public class SparkServerRunner {

    public static void main(String... args) throws Exception {
        secure("", "", "", "", true);
        port(1080);
        get("/oauth2/callback", (request, response) -> "OK");
        get("/*", (request, response) -> {
            try {
                String jsonBody = GitAccessUtils.makeGitRequest(request.pathInfo()).body();
                response.header("Content-Type", "application/json; charset=utf-8");
                response.status(200);
                return jsonBody;
            } catch (Exception e) {
                log.error("Error when fetching git content", e);
                response.status(500);
                return "Error when fetching git content: ".concat(e.getMessage());
            }
        });

    }
}




