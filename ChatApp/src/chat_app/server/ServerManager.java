package chat_app.server;

import chat_app.interfaces.IConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerManager implements Runnable, IConnection {

    private static final ArrayList<ServerManager> serverManagerList = new ArrayList<>();

    private final Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private String clientName;

    public ServerManager(Socket socket) {
        this.socket = socket;
        createConnection();
    }

    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                byte[] fileContentBytes = null;

                //Receive the boolean for a file. true if file is received
                boolean isFile = dataInputStream.readBoolean();

                //Receive the username
                int userNameByteLen = dataInputStream.readInt();
                byte[] userNameBytes = new byte[userNameByteLen];

                if (userNameByteLen > 0) {
                    dataInputStream.readFully(userNameBytes, 0, userNameByteLen);

                    //Receive the massage
                    int msgBytesLength = dataInputStream.readInt();
                    byte[] msgBytes = new byte[msgBytesLength];

                    if (msgBytesLength > 0)
                        dataInputStream.readFully(msgBytes, 0, msgBytesLength);

                    //If a file was sent as msg, receive the file content
                    if (isFile) {
                        int fileContentLength = dataInputStream.readInt();
                        if (fileContentLength > 0) {
                            fileContentBytes = new byte[fileContentLength];
                            dataInputStream.readFully(fileContentBytes, 0, fileContentLength);
                        }
                    }

                    //Broadcast the msg to other clients except the one who sent it
                    broadcastMsg(isFile, new String(userNameBytes), new String(msgBytes), fileContentBytes);
                }

            } catch (IOException e) {
                e.printStackTrace();
                closeConnection();
                break;
            }
        }

    }

    //Broadcast the msg to other clients except the one who sent it
    private void broadcastMsg(boolean isFile, String userName, String text, byte[] fileContentBytes) {
        for (ServerManager serverManager : serverManagerList) {
            try {
                if (serverManager != this) {
                    //Send boolean for file or text. true if file is sent
                    serverManager.dataOutputStream.writeBoolean(isFile);

                    //Send username
                    serverManager.dataOutputStream.writeInt(userName.getBytes().length);
                    serverManager.dataOutputStream.write(userName.getBytes());

                    //Send text msg for file name
                    serverManager.dataOutputStream.writeInt(text.getBytes().length);
                    serverManager.dataOutputStream.write(text.getBytes());

                    if (isFile) {
                        //Send file content
                        serverManager.dataOutputStream.writeInt(fileContentBytes.length);
                        serverManager.dataOutputStream.write(fileContentBytes);
                    }
                    serverManager.dataOutputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeConnection();
            }
        }
    }

    //Send the list of connected clients to the client that connected now
    private void sendConnectedClients(String senderName) {
        StringBuilder text = new StringBuilder("Clients connected to this server: ");
        for (int i = 0; i < serverManagerList.size(); i++) {
            if (serverManagerList.get(i) != this) {
                if (i > 0)
                    text.append(", ");
                text.append(serverManagerList.get(i).clientName);
            }
        }

        try {
            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(false);

            //Send username
            dataOutputStream.writeInt(senderName.getBytes().length);
            dataOutputStream.write(senderName.getBytes());

            //Send text msg
            dataOutputStream.writeInt(text.toString().getBytes().length);
            dataOutputStream.write(text.toString().getBytes());

            dataOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    private void removeClientHandler() {
        System.out.println("A client has left the chat");
        serverManagerList.remove(this);
    }

    @Override
    public void createConnection() {
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            int clientNameByteLen = dataInputStream.readInt();
            byte[] clientNameBytes = new byte[clientNameByteLen];

            if (clientNameByteLen > 0) {
                dataInputStream.readFully(clientNameBytes, 0, clientNameByteLen);

                this.clientName = new String(clientNameBytes);
                serverManagerList.add(this);

                broadcastMsg(false, "SERVER", clientName + " has entered the chat", null);

                if (serverManagerList.size() > 1)
                    sendConnectedClients("SERVER");
            }

        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    @Override
    public void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }

            removeClientHandler();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
