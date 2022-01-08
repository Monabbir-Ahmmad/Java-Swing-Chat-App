import server.Server;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerStart {

    public static void main(String[] args) {
        try {
            Server server = new Server(new ServerSocket(Server.PORT));
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
