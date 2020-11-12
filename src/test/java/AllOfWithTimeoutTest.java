import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

// posted in https://stackoverflow.com/questions/64807597/unexpected-behavior-for-completablefuture-allof-ortimeout
public class AllOfWithTimeoutTest {

    public static final int TIMEOUT_IN_MILLIS = 100;

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
            System.out.format("Had a nap for %s milliseconds.\r\n", millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void allOfOrTimeout1() throws InterruptedException, ExecutionException, TimeoutException {
        getAllOfFuture().get(TIMEOUT_IN_MILLIS, MILLISECONDS);
    }

    @Test
    public void allOfOrTimeout2() throws ExecutionException, InterruptedException {
        getAllOfFuture().orTimeout(TIMEOUT_IN_MILLIS, MILLISECONDS);
    }

    private CompletableFuture<Void> getAllOfFuture() {
        return CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> sleep(1)),
            CompletableFuture.runAsync(() -> sleep(2)),
            CompletableFuture.runAsync(() -> sleep(3)),
            CompletableFuture.runAsync(() -> sleep(4)),
            CompletableFuture.runAsync(() -> sleep(5)),
            CompletableFuture.runAsync(() -> sleep(6)),
            CompletableFuture.runAsync(() -> sleep(7)),
            CompletableFuture.runAsync(() -> sleep(8))
        );
    }

    ;
}
