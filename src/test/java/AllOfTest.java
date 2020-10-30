import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

public class AllOfTest {

    @Test
    public void allOf() throws IOException, ExecutionException, InterruptedException {
        final var httpClient = HttpClient.newHttpClient();
        final var bodyHandler = HttpResponse.BodyHandlers.ofString();

        // get a list of URLs to call
        final List<String> urls = Files.readAllLines(Paths.get("src/test/resources/urls.txt"));

        // map to a list of Futures containing the static html from these urls
        final CompletableFuture<String>[] pageContentFuturesArray = urls
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
            ).toArray(CompletableFuture[]::new);

        //// ---- ALL OF ---- ////
        CompletableFuture allOf = CompletableFuture.runAsync(() -> {
            System.out.println("ALL OF (BEGIN)");

            final String term = "html";
            CompletableFuture<Long> countFuture = CompletableFuture
                // call all these futures in parallel ...
                .allOf(pageContentFuturesArray)
                // ... and when all responses have been received ...
                .thenApply(v -> Arrays.stream(pageContentFuturesArray)
                    // ... join the results into a list of Strings
                    .map(CompletableFuture::join)
                    .collect(toList())
                )
                // ... and count the number of web pages containing a certain word
                .thenApply(pageContents -> pageContents
                    .stream()
                    .filter(pageContent -> pageContent.toLowerCase().contains(term))
                    .count()
                );
            Long count = 0L;
            try {
                count = countFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            System.out.format("'%s' was found in %s websites.\r\n", term.toLowerCase(), count);

            System.out.println("ALL OF (END)");
        });

        //// ---- ANY OF ---- ////
        CompletableFuture anyOf = CompletableFuture.runAsync(() -> {
            System.out.println("ANY OF (BEGIN)");

            // call all these futures in parallel and store only the first result in another future
            try {
                CompletableFuture
                    .anyOf(pageContentFuturesArray)
                    .thenAccept(o -> {
                        final String string = (String) o;
                        System.out.println(string.length() > 0 ? string : "EMPTY RESPONSE");
                    })
                    .get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            System.out.println("ANY OF (END)");
        });


        //// ---- FIRST MATCHING ---- ////
        CompletableFuture firstMatching = CompletableFuture.runAsync(() -> {
            System.out.println("FIRST MATCHING (BEGIN)");

            // call all these futures in parallel and store only the first result in another future
            firstMatching(not(String::isEmpty), pageContentFuturesArray)
                .thenAccept(System.out::println)
                .orTimeout(2, SECONDS)
            ;

            System.out.println("FIRST MATCHING (END)");
        });

        CompletableFuture.allOf(allOf, anyOf, firstMatching).get();
    }

    private HttpRequest createRequest(final String url) {
        return HttpRequest.newBuilder(URI.create(url)).header("accept", "text/html").build();
    }

    private <T> CompletableFuture<T> firstMatching(Predicate<T> predicate, CompletableFuture<T>... futures) {
        final AtomicBoolean completed = new AtomicBoolean();
        final CompletableFuture<T> promise = new CompletableFuture<>();
        for (CompletableFuture<T> future : futures) {
            future.thenAccept(result -> {
                if (predicate.test(result) && completed.compareAndSet(false, true))
                    promise.complete(result);
            });
        }
        return promise;
    }
}
