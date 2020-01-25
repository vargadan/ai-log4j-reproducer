package com.gocompliant.mockserver;

import lombok.extern.slf4j.Slf4j;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

@Slf4j
public class MockServerRunner {

    public static void main(String... args) throws Exception {
        var mockServer = ClientAndServer.startClientAndServer(1080);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/oauth2/callback"))
                .respond((httpRequest -> HttpResponse.response().withStatusCode(200).withBody("OK")));
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/.*"))
                .respond((httpRequest -> {
                    try {
                        var gitResponse = GitAccessUtils.makeGitRequest(httpRequest.getPath().getValue());
                        return HttpResponse.response()
                                .withStatusCode(gitResponse.statusCode())
                                .withHeader("Content-Type", "application/json; charset=utf-8")
                                .withBody(gitResponse.body());
                    } catch (Exception e) {
                        log.error("Error fetching git content.", e);
                        return HttpResponse.response().withStatusCode(500);
                    }
                }));
    }
}




