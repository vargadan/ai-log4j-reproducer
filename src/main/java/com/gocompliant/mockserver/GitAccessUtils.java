package com.gocompliant.mockserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.experimental.UtilityClass;
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

@UtilityClass
@Slf4j
public class GitAccessUtils {

    final static String GIT_SERVER = "https://api.bitbucket.org/2.0/repositories/joergmattes/";
//    final static String GIT_SERVER = "https://api.bitbucket.org/2.0/repositories/vargadan/";

    final static String GIT_REPOSITORY = "rest-integrator-mock-responses";
//    final static String GIT_REPOSITORY = "config-test";

    final static String BRANCH = "HEAD";

    final static String CLIENT_ID = System.getenv("BITBUCKET_CLIENT_ID");

    final static String CLIENT_SECRET = System.getenv("BITBUCKET_CLIENT_SECRET");

    final static private GitRequester gitRequester;

    static {
        String authToken = "";
        try {
            var gitTokens = callGitOauthAccessTokenService();
            authToken = gitTokens.getLeft();
        } catch (Exception e) {
            log.error("Exception when fetching git repository access tokens", e);
        } finally {
            gitRequester = new GitAccessUtils.GitRequester(authToken);
        }

    }

    @Value
    private static class GitRequester {

        final String accessToken;

        HttpResponse<String> makeGitRequest(String path) throws URISyntaxException, IOException, InterruptedException {
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
    }

    public static HttpResponse<String> makeGitRequest(String path) throws URISyntaxException, IOException, InterruptedException{
        return gitRequester.makeGitRequest(path);
    }

    private static java.net.http.HttpRequest.BodyPublisher ofMimeMultipartData(Map<Object, Object> data,
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

    private static Pair<String, String> callGitOauthAccessTokenService() throws Exception {
        var clientIdAndSecret = CLIENT_ID + ":" + CLIENT_SECRET;
        var base64AuthToken = Base64.getEncoder().encode(clientIdAndSecret.getBytes());
        String boundary = new BigInteger(256, new Random()).toString();
        var accessTokenRequest = java.net.http.HttpRequest.newBuilder()
                .uri(new URI("https://bitbucket.org/site/oauth2/access_token"))
                .timeout(Duration.ofSeconds(10))
                .setHeader("Content-Type", "multipart/form-data;boundary=" + boundary)
                .setHeader("Authorization", "Basic ".concat(new String(base64AuthToken)))
                .POST(ofMimeMultipartData(Map.of("grant_type", "client_credentials"), boundary))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var accessTokenResponse = HttpClient.newBuilder().build()
                .send(accessTokenRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        var responseBody = accessTokenResponse.body();
        ObjectMapper objectMapper = new ObjectMapper();
        var accessTokenResponseNode = objectMapper.reader().readTree(responseBody);
        var accessToken = accessTokenResponseNode.get("access_token").asText();
        var refreshToken = accessTokenResponseNode.get("refresh_token").asText();
        if (StringUtils.isNotEmpty(accessToken)) {
            log.info("Successfully obtained token for git repository access.");
        } else {
            log.error("Could not obtain token for git repository access.");
        }
        return ImmutablePair.of(accessToken, refreshToken);
    }
}