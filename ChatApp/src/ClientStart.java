public class ClientStart {

    public static void main(String[] args) {
        try {
            Thread thread1 = new Thread(() -> new ChatUI("User 1"));
            thread1.start();
            thread1.join();

            Thread thread2 = new Thread(() -> new ChatUI("User 2"));
            thread2.start();
            thread2.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
