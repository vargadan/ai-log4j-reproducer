package com.gocompliant.mockserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.CheckedFunction1;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

@Slf4j
public class BitBucketAccess implements ResponseReader {

    private final static String GIT_SERVER = "https://api.bitbucket.org/2.0/repositories/joergmattes/";

    private final static String GIT_REPOSITORY = "rest-integrator-mock-responses";

    private final static String BRANCH = "HEAD";

    private final static String CLIENT_ID = System.getenv("BITBUCKET_CLIENT_ID");

    private final static String CLIENT_SECRET = System.getenv("BITBUCKET_CLIENT_SECRET");

    public final CheckedFunction1<String, HttpResponse<String>> request;

    public BitBucketAccess() {
        CheckedFunction1<String, HttpResponse<String>> f = this::makeGitRequest;
        this.request = f.memoized();
        this.gitRequester = new GitRequester();
    }

    private GitRequester gitRequester = null;

    private String refreshToken = null;

    private final Object refreshLock = new Object();

    @Override
    public Response read(String path) throws Throwable {
        var response = this.request.apply(path);
        return Response.builder().statusCode(response.statusCode()).body(response.body()).build();
    }

    private class GitRequester {

        final String accessToken;

        GitRequester() {
            String accessToken = null;
            try {
                var gitTokens = callGitOauthAccessTokenService(refreshToken);
                accessToken = gitTokens.getLeft();
                refreshToken = gitTokens.getRight();
            } catch (Exception e) {
                log.error("Exception when fetching git repository access tokens", e);
            }
            this.accessToken = accessToken;
        }

        private HttpResponse<String> makeContentRequest(String path) throws URISyntaxException, IOException, InterruptedException {
            final String file = path.concat("/response.json");
            var gitRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(new URI(GIT_SERVER + GIT_REPOSITORY + "/src/" + BRANCH + file))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .setHeader("Authorization", "Bearer ".concat(accessToken))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            log.info("gitRequest.uri() : " + gitRequest.uri());
            HttpResponse<String> send = HttpClient.newBuilder().build()
                    .send(gitRequest, HttpResponse.BodyHandlers.ofString());
            return send;
        }

        HttpResponse<String> makeGitRequest(String path) throws URISyntaxException, IOException, InterruptedException {
            return makeContentRequest(path);
        }
    }

    private HttpResponse<String> makeGitRequest(String path) throws URISyntaxException, IOException, InterruptedException{
        var response = gitRequester.makeGitRequest(path);
        var expiredToken = response.statusCode() == 401;
        if (expiredToken) {
            synchronized (refreshLock) {
                gitRequester = new GitRequester();
                response = gitRequester.makeGitRequest(path);
            }
        }
        return response;
    }

    private java.net.http.HttpRequest.BodyPublisher ofMimeMultipartData(Map<?, ?> data,
                                                                              String boundary) throws IOException {
        var byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
                .getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            byteArrays.add(separator);
            byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return java.net.http.HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    private Pair<String, String> callGitOauthAccessTokenService(String oldRefreshToken) throws Exception {
        var clientIdAndSecret = CLIENT_ID + ":" + CLIENT_SECRET;
        var base64AuthToken = Base64.getEncoder().encode(clientIdAndSecret.getBytes());
        String boundary = new BigInteger(256, new Random()).toString();
        var data = Map.of("grant_type", "client_credentials");
        if (oldRefreshToken != null) {
            data = Map.of("grant_type", "refresh_token", "refresh_token", oldRefreshToken);
        }
        var accessTokenRequest = java.net.http.HttpRequest.newBuilder()
                .uri(new URI("https://bitbucket.org/site/oauth2/access_token"))
                .timeout(Duration.ofSeconds(10))
                .setHeader("Content-Type", "multipart/form-data;boundary=" + boundary)
                .setHeader("Authorization", "Basic ".concat(new String(base64AuthToken)))
                .POST(ofMimeMultipartData(data, boundary))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var accessTokenResponse = HttpClient.newBuilder().build()
                .send(accessTokenRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        var responseBody = accessTokenResponse.body();
        ObjectMapper objectMapper = new ObjectMapper();
        var accessTokenResponseNode = objectMapper.reader().readTree(responseBody);
        var newAccessToken = accessTokenResponseNode.get("access_token").asText();
        var newRefreshToken = accessTokenResponseNode.get("refresh_token").asText();
        if (StringUtils.isNotEmpty(newAccessToken)) {
            log.info("Successfully obtained token for git repository access.");
        } else {
            log.error("Could not obtain token for git repository access.");
        }
        return ImmutablePair.of(newAccessToken, newRefreshToken);
    }
}