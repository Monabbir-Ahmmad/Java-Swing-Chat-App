package chat_app.client;

import chat_app.interfaces.listeners.MessageReceiveListener;

import java.io.*;
import java.net.Socket;

public class ClientController implements Runnable {
    private final String clientName;
    private final ClientConnectionController clientConnectionController;

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private int fileID = 0;

    private MessageReceiveListener messageReceiveListener;

    public ClientController(String clientName, ClientConnectionController clientConnectionController) {
        this.clientName = clientName;
        this.clientConnectionController = clientConnectionController;
    }

    //This receives the massages and this will run on a separate thread because waiting for a msg is a blocking operation
    @Override
    public void run() {
        //This will trigger then the client creates the connection
        clientConnectionController.setConnectListener(() -> {
            socket = clientConnectionController.getSocket();
            dataOutputStream = clientConnectionController.getDataOutputStream();
            dataInputStream = clientConnectionController.getDataInputStream();

            //Send username to the server after connecting to the server
            sendClientName();
        });

        clientConnectionController.createConnection();

        try {
            //Receive massages while the socket is connected
            while (socket.isConnected()) {
                receiveMessage();
            }
        } catch (IOException e) {
            e.printStackTrace();
            clientConnectionController.closeConnection();
        }
    }

    //Send username to the server
    private void sendClientName() {
        try {
            dataOutputStream.writeInt(clientName.getBytes().length);
            dataOutputStream.write(clientName.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            clientConnectionController.closeConnection();
        }
    }

    //Send the text msg through data stream
    public boolean sendText(String text) {
        try {
            boolean isFile = false;

            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(isFile);

            //Send username
            sendClientName();

            //Send text
            dataOutputStream.writeInt(text.getBytes().length);
            dataOutputStream.write(text.getBytes());

            dataOutputStream.flush();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            clientConnectionController.closeConnection();
        }
        return false;
    }

    //Send the file through data stream
    public boolean sendFile(File file) {
        try {
            boolean isFile = true;

            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
            String fileName = file.getName();

            byte[] fileContentBytes = new byte[(int) file.length()];
            fileInputStream.read(fileContentBytes);

            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(isFile);

            //Send username
            sendClientName();

            //Send file name
            dataOutputStream.writeInt(fileName.getBytes().length);
            dataOutputStream.write(fileName.getBytes());

            //Send file content
            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            dataOutputStream.flush();
            fileInputStream.close();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            clientConnectionController.closeConnection();
        }

        return false;
    }

    //Receive message from other clients
    private void receiveMessage() throws IOException {
        ReceivedFile receivedFile = null;

        //Receive the boolean for a file. true if file is received
        boolean isFile = dataInputStream.readBoolean();

        //Receive the message sender name
        int userNameByteLen = dataInputStream.readInt();
        byte[] userNameBytes = new byte[userNameByteLen];

        if (userNameByteLen > 0)
            dataInputStream.readFully(userNameBytes, 0, userNameByteLen);

        //Receive the message
        int msgBytesLength = dataInputStream.readInt();
        byte[] msgBytes = new byte[msgBytesLength];

        if (msgBytesLength > 0)
            dataInputStream.readFully(msgBytes, 0, msgBytesLength);

        //If a file was sent as message, receive the file content
        if (isFile) {
            int fileContentLength = dataInputStream.readInt();
            if (fileContentLength > 0) {
                byte[] fileContentBytes = new byte[fileContentLength];
                dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                //Add the received file into the array list
                receivedFile = new ReceivedFile(fileID, new String(msgBytes), fileContentBytes);
                fileID++;
            }
        }

        if (messageReceiveListener != null) {
            messageReceiveListener.onMessageReceive(new String(userNameBytes), new String(msgBytes), isFile, receivedFile);
        }
    }

    public void setMessageReceiveListener(MessageReceiveListener listener) {
        this.messageReceiveListener = listener;
    }

}
