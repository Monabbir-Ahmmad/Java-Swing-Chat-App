import gui.ChatUI;

public class ClientStart {

    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> new ChatUI("User 1"));
        thread1.start();

        Thread thread2 = new Thread(() -> new ChatUI("User 2"));
        thread2.start();
    }
}
