package chat_app.client;

import chat_app.interfaces.IConnection;
import chat_app.interfaces.listeners.ConnectListener;
import chat_app.interfaces.listeners.DisconnectListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientConnectionController implements IConnection {
    private final String ip;
    private final int port;

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private DisconnectListener disconnectListener;
    private ConnectListener connectListener;

    public ClientConnectionController(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void createConnection() {
        try {
            //Connect to the server socket
            socket = new Socket(ip, port);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            if (connectListener != null)
                connectListener.onConnect();

        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    @Override
    public void closeConnection() {
        try {
            if (dataInputStream != null)
                dataInputStream.close();

            if (dataOutputStream != null)
                dataOutputStream.close();

            if (socket != null)
                socket.close();

            if (disconnectListener != null)
                disconnectListener.onDisconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Socket getSocket() {
        return socket;
    }

    public DataOutputStream getDataOutputStream() {
        return dataOutputStream;
    }

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    public void setDisconnectListener(DisconnectListener listener) {
        this.disconnectListener = listener;
    }

    public void setConnectListener(ConnectListener listener) {
        this.connectListener = listener;
    }
}
