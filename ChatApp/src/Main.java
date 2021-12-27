import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        boolean isServer = true;

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(() -> new ChatUI("User 1", isServer));
        executor.submit(() -> new ChatUI("User 2", !isServer));
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
