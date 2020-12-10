import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

//https://www.callicoder.com/java-8-completablefuture-tutorial/
public class CompletableFutureTest {

    @Test
    public void complete() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        final String firstResult = "First and only result";
        completableFuture.complete(firstResult);
        assertThat(completableFuture.get(), equalTo(firstResult));
        completableFuture.complete("This second result is ignored"); // is ignored
        assertThat(completableFuture.get(), equalTo(firstResult));
    }

    @Test
    public void joinDoesntUseCheckedExceptions() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        final String firstResult = "First and only result";
        completableFuture.complete(firstResult);
        assertThat(completableFuture.join(), equalTo(firstResult));
        completableFuture.complete("This second result is ignored"); // is ignored
        assertThat(completableFuture.join(), equalTo(firstResult));
    }

    @Test
    public void runAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            sleep(1);
            System.out.println("ASYNC runAsync: I'll run in a separate thread than the main thread.");
        });
        System.out.println("END runAsync SYNC part");
        future.get();
    }

    @Test
    public void supplyAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            sleep(1);
            return "ASYNC sypplyAsync: Result of the asynchronous computation";
        });
        System.out.println(future.get());
    }

    @Test
    public void thenApply() throws ExecutionException, InterruptedException {
        CompletableFuture<String> greetingFuture = CompletableFuture.supplyAsync(() -> {
            sleep(1);
            return "Chris";
        }).thenApply(name -> {
            sleep(1);
            return "Hello " + name;
        }).thenApply(greeting -> greeting + ", welcome to Ibiza!");

        assertThat(greetingFuture.get(), equalTo("Hello Chris, welcome to Ibiza!"));
    }

    @Test
    public void thenAccept() {
        CompletableFuture.supplyAsync(
            () -> "{product name}"
        ).thenAccept(product -> System.out.println("Got product detail from remote service " + product));
    }

    @Test
    public void thenRun() {
        CompletableFuture.supplyAsync(
            () -> "some result"
        ).thenRun(() -> System.out.println("Computation finished"));
    }

    @Test
    public void thenCompose() throws ExecutionException, InterruptedException {
        CompletableFuture<String> name = getFirstName().thenCompose(this::addLastName);

        assertThat(name.get(), equalTo("Chris Dickinson"));
    }

    @Test
    public void thenCombine() throws ExecutionException, InterruptedException {
        CompletableFuture<String> name = getFirstName()
            .thenCombine(getLastName(), (firstName, lastName) -> firstName + " " + lastName);

        assertThat(name.get(), equalTo("Chris Dickinson"));
    }

    /**
     * see {@link AllOfTest}
     */
    @Ignore
    @Test
    public void allOf() {}

    /**
     * see {@link AllOfTest}
     */
    @Ignore
    @Test
    public void anyOf() {}

    @Test
    public void exceptionally() throws ExecutionException, InterruptedException {
        CompletableFuture<Object> result = CompletableFuture.supplyAsync(() -> {
                throw new IllegalArgumentException();
        }).exceptionally(ex -> "My Exception!");

        assertThat(result.get(), equalTo("My Exception!"));
    }

    @Test
    public void handle() throws ExecutionException, InterruptedException {
        CompletableFuture<Object> result1 = CompletableFuture
            .supplyAsync(() -> "Some Result")
            .handle((res, ex) -> res);

        assertThat(result1.get(), equalTo("Some Result"));

        CompletableFuture<Object> result2 = CompletableFuture.supplyAsync(() -> {
            throw new IllegalArgumentException();
        }).handle((res, ex) -> "My Exception!");

        assertThat(result2.get(), equalTo("My Exception!"));
    }

    private void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private CompletableFuture<String> getFirstName() {
        return CompletableFuture.supplyAsync(() -> "Chris");
    }

    private CompletableFuture<String> getLastName() {
        return CompletableFuture.supplyAsync(() -> "Dickinson");
    }

    private CompletableFuture<String> addLastName(String firstName) {
        return CompletableFuture.supplyAsync(() -> firstName + " Dickinson");
    }
}
