import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.time.Duration;

public class MockServerRunner {

    final static String GIT_SERVER = "https://bitbucket.org/joergmattes/";

    final static String GIT_REPOSITORY = "rest-integrator-mock-responses/raw";

    final static String BRANCH = "HEAD";

    static HttpResponse respondGitContent(String repo, String branch, String file) throws Exception {
        var gitRequest = java.net.http.HttpRequest.newBuilder()
                .uri(new URI(GIT_SERVER + repo + "/" + branch + "/" + file))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        var gitResponse = java.net.http.HttpClient.newBuilder().build()
                .send(gitRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (gitResponse.statusCode() == 404) {
            return HttpResponse.notFoundResponse();
        } else {
            return HttpResponse.response()
                    .withStatusCode(gitResponse.statusCode())
                    .withBody(gitResponse.body());
        }
    }

    public static void main(String... args) throws Exception {
        var mockServer = ClientAndServer.startClientAndServer(1080);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/.*")
        ).respond(httpRequest -> respondGitContent(GIT_REPOSITORY, BRANCH, httpRequest.getPath() + "/response.json"));
    }
}




