package client;

import server.Server;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client implements Runnable {

    private final String clientName;

    private final ArrayList<ReceivedFile> receivedFileArrayList = new ArrayList<>();

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private int fileID = 0;

    private ClientStopListener clientStopListener;
    private MessageReceiveListener messageReceiveListener;


    public Client(String clientName) {
        this.clientName = clientName;
    }

    //This receives the massages and this will run on a separate thread because waiting for a msg is a blocking operation
    @Override
    public void run() {
        try {
            connectToServer();

            //Receive massages while the socket is connected
            while (socket.isConnected()) {
                receiveMessage();
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();
        }
    }

    private void connectToServer() throws IOException {
        //Connect to the socket. If this is the host, create the server socket
        socket = new Socket(Server.IP, Server.PORT);
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataInputStream = new DataInputStream(socket.getInputStream());

        //Send username to the server after connecting to the server
        dataOutputStream.writeInt(clientName.getBytes().length);
        dataOutputStream.write(clientName.getBytes());
    }

    private void receiveMessage() throws IOException {
        //Receive the boolean for a file. true if file is received
        boolean isFile = dataInputStream.readBoolean();

        //Receive the message sender name
        int userNameByteLen = dataInputStream.readInt();
        byte[] userNameBytes = new byte[userNameByteLen];

        if (userNameByteLen > 0) {
            dataInputStream.readFully(userNameBytes, 0, userNameByteLen);

            //Receive the msg
            int msgBytesLength = dataInputStream.readInt();
            byte[] msgBytes = new byte[msgBytesLength];

            if (msgBytesLength > 0)
                dataInputStream.readFully(msgBytes, 0, msgBytesLength);

            //If a file was sent as msg, receive the file content
            if (isFile) {
                int fileContentLength = dataInputStream.readInt();
                if (fileContentLength > 0) {
                    byte[] fileContentBytes = new byte[fileContentLength];
                    dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                    //Add the received file into the array list
                    receivedFileArrayList.add(new ReceivedFile(fileID, new String(msgBytes), fileContentBytes));
                    fileID++;
                }
            }

            if (messageReceiveListener != null) {
                messageReceiveListener.onMessageReceive(true, new String(userNameBytes), new String(msgBytes), isFile);
            }

        }
    }

    //Send the text msg through data stream
    public boolean sendText(String text) {
        try {
            boolean isFile = false;

            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(isFile);

            //Send username
            dataOutputStream.writeInt(clientName.getBytes().length);
            dataOutputStream.write(clientName.getBytes());

            //Send text
            dataOutputStream.writeInt(text.getBytes().length);
            dataOutputStream.write(text.getBytes());

            dataOutputStream.flush();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();
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
            dataOutputStream.writeInt(clientName.getBytes().length);
            dataOutputStream.write(clientName.getBytes());

            //Send file name
            dataOutputStream.writeInt(fileName.getBytes().length);
            dataOutputStream.write(fileName.getBytes());

            //Send file content
            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            dataOutputStream.flush();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();
        }

        return false;
    }

    private void closeEverything() {
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

            System.out.println("Error! Server not ready or something went wrong");

            int dialogResult = JOptionPane.showConfirmDialog(null, "The server might be down. Try to reconnect?", "Error!", JOptionPane.YES_NO_OPTION);

            if (dialogResult == JOptionPane.YES_OPTION) {
                connectToServer();
            } else if (clientStopListener != null) {
                clientStopListener.onClientStop();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ArrayList<ReceivedFile> getReceivedFileList() {
        return receivedFileArrayList;
    }

    public void setClientStopListener(ClientStopListener listener) {
        this.clientStopListener = listener;
    }

    public void setMessageReceiveListener(MessageReceiveListener listener) {
        this.messageReceiveListener = listener;
    }


    public interface ClientStopListener {
        void onClientStop();
    }

    public interface MessageReceiveListener {
        void onMessageReceive(boolean isReceived, String senderName, String message, boolean isFile);
    }

}
