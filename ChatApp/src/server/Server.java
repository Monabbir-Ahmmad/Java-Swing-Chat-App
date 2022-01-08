package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static final int PORT = 1234;
    public static final String IP = "localhost";

    private final ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        System.out.println("Server is running");
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                System.out.println("A new client has connected");

                ServerManager serverManager = new ServerManager(socket);

                Thread thread = new Thread(serverManager);
                thread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
            closeServerSocket();
        }
    }

    private void closeServerSocket() {
        System.out.println("Server is stopped");
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
