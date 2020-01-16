import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Consumer;

public class MockServerRunner {

    final static String GIT_SERVER = "https://api.bitbucket.org/2.0/repositories/joergmattes/";

    final static String GIT_REPOSITORY = "rest-integrator-mock-responses";

    final static String BRANCH = "HEAD";

    final static Consumer<String> logger = System.out::println;

    static HttpResponse serveGitContent(HttpRequest httpRequest) throws Exception {
        final String file = httpRequest.getPath() + "/response.json";
        var gitRequest = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(GIT_SERVER + GIT_REPOSITORY + "/src/" + BRANCH + file))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        logger.accept("gitRequest.uri() : " + gitRequest.uri());
        try {
            var gitResponse = java.net.http.HttpClient.newBuilder().build()
                    .send(gitRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (gitResponse.statusCode() == 404) {
                return HttpResponse.notFoundResponse();
            } else {
                return HttpResponse.response()
                        .withStatusCode(gitResponse.statusCode())
                        .withBody(gitResponse.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.response().withStatusCode(500);
        }
    }

    static HttpResponse extractOauth2Token(HttpRequest request) {
        logger.accept(request.getBodyAsString());
        return HttpResponse.response().withStatusCode(200);
    }

    public static void main(String... args) throws Exception {
        var mockServer = ClientAndServer.startClientAndServer(1080);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/.*"))
                .respond(MockServerRunner::serveGitContent);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/oauth2/callback"))
                .respond(MockServerRunner::extractOauth2Token);
    }
}




