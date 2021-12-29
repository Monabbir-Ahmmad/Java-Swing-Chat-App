import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlerList = new ArrayList<>();

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private String clientName;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            int clientNameByteLen = dataInputStream.readInt();
            byte[] clientNameBytes = new byte[clientNameByteLen];
            if (clientNameByteLen > 0) {
                dataInputStream.readFully(clientNameBytes, 0, clientNameByteLen);

                if (clientNameBytes != null) {
                    this.clientName = new String(clientNameBytes);
                    clientHandlerList.add(this);

                    broadcastMsg(false, "SERVER", clientName + " has entered the chat", null);

                    if (clientHandlerList.size() > 1)
                        sendConnectedClients("SERVER");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    private void closeEverything(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        removeClientHandler();
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

        } catch (IOException e) {
            e.printStackTrace();
        }

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

                    if (msgBytes != null) {
                        //Broadcast the msg to other clients except the one who sent it
                        broadcastMsg(isFile, new String(userNameBytes), new String(msgBytes), fileContentBytes);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                closeEverything(socket, dataInputStream, dataOutputStream);
                break;
            }
        }

    }

    //Broadcast the msg to other clients except the one who sent it
    private void broadcastMsg(boolean isFile, String userName, String text, byte[] fileContentBytes) {
        for (ClientHandler clientHandler : clientHandlerList) {
            try {
                if (clientHandler != this) {
                    //Send boolean for file or text. true if file is sent
                    clientHandler.dataOutputStream.writeBoolean(isFile);

                    //Send username
                    clientHandler.dataOutputStream.writeInt(userName.getBytes().length);
                    clientHandler.dataOutputStream.write(userName.getBytes());

                    //Send text msg for file name
                    clientHandler.dataOutputStream.writeInt(text.getBytes().length);
                    clientHandler.dataOutputStream.write(text.getBytes());

                    if (isFile) {
                        //Send file content
                        clientHandler.dataOutputStream.writeInt(fileContentBytes.length);
                        clientHandler.dataOutputStream.write(fileContentBytes);
                    }
                    clientHandler.dataOutputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeEverything(socket, dataInputStream, dataOutputStream);
            }
        }
    }

    //Send the list of connected clients to the client that connected now
    private void sendConnectedClients(String userName) {
        StringBuilder text = new StringBuilder("Clients connected to this server: ");
        for (int i = 0; i < clientHandlerList.size(); i++) {
            if (clientHandlerList.get(i) != this) {
                if (i > 0)
                    text.append(", ");
                text.append(clientHandlerList.get(i).clientName);
            }
        }

        try {
            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(false);

            //Send username
            dataOutputStream.writeInt(userName.getBytes().length);
            dataOutputStream.write(userName.getBytes());

            //Send text msg
            dataOutputStream.writeInt(text.toString().getBytes().length);
            dataOutputStream.write(text.toString().getBytes());

            dataOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    private void removeClientHandler() {
        System.out.println("A client has left the chat");
        clientHandlerList.remove(this);
    }
}
