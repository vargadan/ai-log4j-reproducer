package com.gocompliant.mockserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class MockServerRunner {

    final static String GIT_SERVER = "https://api.bitbucket.org/2.0/repositories/joergmattes/";

    final static String GIT_REPOSITORY = "rest-integrator-mock-responses";

    final static String BRANCH = "HEAD";

    final static Consumer<String> logger = System.out::println;

    final static String CLIENT_ID = System.getenv("BITBUCKET_CLIENT_ID");

    final static String CLIENT_SECRET = System.getenv("BITBUCKET_CLIENT_SECRET");

    @Value
    private static class GitRequester {

        final String accessToken;

        HttpResponse serveGitContent(HttpRequest httpRequest) throws Exception {
            final String file = httpRequest.getPath() + "/response.json";
            var gitRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(new URI(GIT_SERVER + GIT_REPOSITORY + "/src/" + BRANCH + file))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .setHeader("Authorization","Bearer ".concat(accessToken))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            logger.accept("gitRequest.uri() : " + gitRequest.uri());
            try {
                var gitResponse = java.net.http.HttpClient.newBuilder().build()
                        .send(gitRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
                return HttpResponse.response()
                            .withStatusCode(gitResponse.statusCode())
                            .withBody(gitResponse.body());
            } catch (Exception e) {
                e.printStackTrace();
                return HttpResponse.response().withStatusCode(500);
            }
        }

    }

    static HttpResponse handleOauthCallBack(HttpRequest request) {
        // this is actually seems to be a dummy thing,
        // the bitbucket does the callback but it is just an empty request
        return HttpResponse.response().withStatusCode(200);
    }

    static Pair<String, String> callGitOauthAccessTokenService() throws Exception {
        var clientIdAndSecret = CLIENT_ID + ":" + CLIENT_SECRET;
        var base64AuthToken = Base64.getEncoder().encode(clientIdAndSecret.getBytes());
        String boundary = new BigInteger(256, new Random()).toString();
        var accessTokenRequest = java.net.http.HttpRequest.newBuilder()
                .uri(new URI("https://bitbucket.org/site/oauth2/access_token"))
                .timeout(Duration.ofSeconds(10))
                .setHeader("Content-Type", "multipart/form-data;boundary=" + boundary)
                .setHeader("Authorization","Basic ".concat(new String(base64AuthToken)))
                .POST(ofMimeMultipartData(Map.of("grant_type", "client_credentials"), boundary))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var accessTokenResponse = java.net.http.HttpClient.newBuilder().build()
                .send(accessTokenRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        var responseBody = accessTokenResponse.body();
        logger.accept(responseBody);
        ObjectMapper objectMapper = new ObjectMapper();
        var accessTokenResponseNode = objectMapper.reader().readTree(responseBody);
        var accessToken = accessTokenResponseNode.get("access_token").asText();
        var refreshToken = accessTokenResponseNode.get("refresh_token").asText();
        return ImmutablePair.of(accessToken, refreshToken);
    }

    public static java.net.http.HttpRequest.BodyPublisher ofMimeMultipartData(Map<Object, Object> data,
                                                                              String boundary) throws IOException {
        var byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
                .getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            byteArrays.add(separator);
            byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return java.net.http.HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    public static void main(String... args) throws Exception {
        var gitTokens = callGitOauthAccessTokenService();
        var gitRequester = new GitRequester(gitTokens.getLeft());
        var mockServer = ClientAndServer.startClientAndServer(1080);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/.*"))
                .respond(gitRequester::serveGitContent);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/oauth2/callback"))
                .respond(MockServerRunner::handleOauthCallBack);
    }
}




