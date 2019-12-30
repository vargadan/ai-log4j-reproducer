import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class MockServerRunner {

    final static String GIT_REPOSITORY = "sample_rest_responses";

    final static String BRANCH = "master";

    static String getGitContent(String repo, String branch, String file) throws Exception {
        var gitRequest = java.net.http.HttpRequest.newBuilder()
                .uri(new URI("https://raw.githubusercontent.com/vargadan/" + repo + "/" + branch + "/" + file))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        var gitResponse = java.net.http.HttpClient.newBuilder().build()
                .send(gitRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        var gitResponseBody = gitResponse.body();
        return gitResponseBody;
    }

    public static void main(String... args) throws Exception {
        var mockServer = ClientAndServer.startClientAndServer(1080);
        mockServer.when(HttpRequest.request().withMethod("GET").withPath("/api/ui")
        ).respond(
                HttpResponse.response(getGitContent(GIT_REPOSITORY, BRANCH,  "/api/ui/response.json")));
    }
}




