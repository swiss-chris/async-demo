import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;

public class AllOfTest {

    @Test
    public void allOf() throws IOException, ExecutionException, InterruptedException {
        final var httpClient = HttpClient.newHttpClient();
        final var bodyHandler = HttpResponse.BodyHandlers.ofString();

        // get a list of URLs to call
        final List<String> urls = Files.readAllLines(Paths.get("src/test/resources/urls.txt"));

        // map to a list of Futures containing the static html from these urls
        final List<CompletableFuture<String>> pageContentFutures = urls
            .stream()
            .map(url -> {
                    System.out.println("requesting url = " + url);
                    return httpClient
                        .sendAsync(createRequest(url), bodyHandler)
                        .thenApply(response -> {
                            System.out.println("received response from url = " + url);
                            return response.body();
                        });
                }
            ).collect(toList());

        //// ---- ANY OF ---- ////

        System.out.println("ANY OF (BEGIN)");

        // call all these futures in parallel and store only the first result in another future
        CompletableFuture.anyOf(
            pageContentFutures.toArray(new CompletableFuture[0]))
            .thenAccept(o -> System.out.println((String) o))
            .get();

        System.out.println("ANY OF (END)");

        //// ---- ALL OF ---- ////

        System.out.println("ALL OF (BEGIN)");

        // call all these futures in parallel and store the result in another future
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            pageContentFutures.toArray(new CompletableFuture[0]));

        // convert the type from Void to List<String> using .join()
        final CompletableFuture<List<String>> allPageContentsFuture = allFutures.thenApply(v -> pageContentFutures
            .stream()
            .map(CompletableFuture::join)
            .collect(toList())
        );

        // Count the number of web pages containing a certain word
        final String term = "html";
        CompletableFuture<Long> countFuture = allPageContentsFuture.thenApply(pageContents -> pageContents.stream()
            .filter(pageContent -> pageContent.toLowerCase().contains(term))
            .count());
        System.out.format("'%s' was found in %s websites.\r\n", term.toLowerCase(), countFuture.get());

        System.out.println("ALL OF (END)");
    }

    private HttpRequest createRequest(final String url) {
        return HttpRequest.newBuilder(URI.create(url)).header("accept", "text/html").build();
    }
}
