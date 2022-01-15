package chat_app.server;

import chat_app.interfaces.IConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements IConnection {

    public static final int PORT = 1234;
    public static final String IP = "localhost";

    private final ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void createConnection() {
        System.out.println("Server is running");
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                ServerManager serverManager = new ServerManager(socket);

                Thread thread = new Thread(serverManager);
                thread.start();

                System.out.println("A new client has connected");
            }

        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    @Override
    public void closeConnection() {
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
