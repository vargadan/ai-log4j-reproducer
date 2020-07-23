package com.gocompliant.smarthub.mockserver;

import io.vavr.CheckedFunction1;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Slf4j
public class AzureGitAccess implements ResponseReader {

    private final static String URL = "https://dev.azure.com/goco/product/_apis/git/repositories/mockservice-responses/items";

    private final static String BRANCH = "master";

    private final static String AZ_USERNAME = System.getenv("AZ_USERNAME");

    private final static String AZ_PERSONAL_ACCESS_TOKEN = System.getenv("AZ_PERSONAL_ACCESS_TOKEN");

    private final static String BASIC_AUTH_TOKEN;

    static {
        var unencoded = AZ_USERNAME + ":" + AZ_PERSONAL_ACCESS_TOKEN;
        BASIC_AUTH_TOKEN = Base64.getEncoder().encodeToString(unencoded.getBytes());
    }

    public final CheckedFunction1<String, HttpResponse<String>> request;

    {
        CheckedFunction1<String, HttpResponse<String>> f = this::makeContentRequest;
        this.request = f.memoized();
    }

    @Override
    public Response read(String path) throws Throwable {
        var response = this.request.apply(path);
        return Response.builder().statusCode(response.statusCode()).body(response.body()).build();
    }

    private HttpResponse<String> makeContentRequest(String path) throws URISyntaxException, IOException, InterruptedException {
        final String fullPath = path.concat("/response.json");
        var uriBuilder = new StringBuilder(URL)
                .append("?path=").append(fullPath)
                .append("&apiVersion=5.1")
                .append("&versionDescriptor.version=").append(BRANCH)
                .append("&versionDescriptor.versionType=").append("branch");
        var gitRequest = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(uriBuilder.toString()))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .setHeader("Authorization", "Basic ".concat(BASIC_AUTH_TOKEN))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        log.info("gitRequest.uri() : " + gitRequest.uri());
        HttpResponse<String> send = HttpClient.newBuilder().build()
                .send(gitRequest, HttpResponse.BodyHandlers.ofString());
        return send;
    }
}
