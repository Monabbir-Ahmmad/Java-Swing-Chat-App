public class Main {

    public static void main(String[] args) {
        try {
            Thread thread1 = new Thread(() -> new ChatUI("User 1", false));
            Thread thread2 = new Thread(() -> new ChatUI("User 2", false));
            Thread thread3 = new Thread(() -> new ChatUI("User 3", false));

            thread1.start();
            thread1.join();
            thread2.start();
            thread2.join();
            thread3.start();
            thread3.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
