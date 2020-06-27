package com.gocompliant.mockserver;

import lombok.extern.slf4j.Slf4j;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static spark.Spark.port;

@Slf4j
public class MockServerRunner {

    static GitAccess gitAccess = new GitAccess();

    public static void main(String... args) throws Exception {
        int port = 1080;
        if (args != null && args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        var mockServer = ClientAndServer.startClientAndServer(port);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/oauth2/callback"))
                .respond((httpRequest -> HttpResponse.response().withStatusCode(200).withBody("OK")));
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/reset"))
                .respond((httpRequest -> {
                    try {
                        gitAccess = new GitAccess();
                        return HttpResponse.response().withStatusCode(200).withBody("git access has been reset");
                    } catch (Throwable e) {
                        log.error("Error fetching git content.", e);
                        return HttpResponse.response().withStatusCode(500);
                    }
                }));
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/.*"))
                .respond((httpRequest -> {
                    try {
                        var gitResponse = gitAccess.request.apply(httpRequest.getPath().getValue());
                        return HttpResponse.response()
                                .withStatusCode(gitResponse.statusCode())
                                .withHeader("Content-Type", "application/json; charset=utf-8")
                                .withBody(gitResponse.body());
                    } catch (Throwable e) {
                        log.error("Error fetching git content.", e);
                        return HttpResponse.response().withStatusCode(500);
                    }
                }));
    }
}




