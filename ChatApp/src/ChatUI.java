import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatUI extends JFrame {
    public static final String IP = "localhost";
    public static final int PORT = 1234;
    private final boolean isHost;
    private final ArrayList<FileReceived> fileReceivedArrayList = new ArrayList<>();

    private JPanel mainPanel, chatBox;
    private JButton btnSendText, btnSendFile;
    private JTextArea msgBox;
    private JScrollPane chatBoxScrollPane;

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private boolean isFile;
    private int fileID = 0;

    public ChatUI(String title, boolean isHost) {
        setTitle(title);
        this.isHost = isHost;

        initialize();
        initListeners();
        receiveMsg();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setPreferredSize(new Dimension(600, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS));
        chatBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        msgBox.grabFocus();
    }

    private void initListeners() {
        btnSendText.addActionListener(e -> {
            String text = msgBox.getText().strip();
            if (!text.isBlank()) {
                sendText(text);
            }
        });

        btnSendFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose a file to send");
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                int dialogResult = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to send this file?\n" + fileChooser.getSelectedFile().getName(),
                        "Send file", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    sendFile(fileChooser.getSelectedFile());
                }
            }
        });

    }

    private void closeEverything(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
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

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendText(String text) {
        isFile = false;

        try {
            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(isFile);

            //Send username
            dataOutputStream.writeInt(getTitle().getBytes().length);
            dataOutputStream.write(getTitle().getBytes());

            //Send text
            dataOutputStream.writeInt(text.getBytes().length);
            dataOutputStream.write(text.getBytes());

            dataOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }

        msgBox.setText("");
        msgBox.grabFocus();
        updateUIForText(false, "Me", text);
    }


    private void sendFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
            String fileName = file.getName();

            byte[] fileContentBytes = new byte[(int) file.length()];
            fileInputStream.read(fileContentBytes);

            isFile = true;

            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(isFile);

            //Send username
            dataOutputStream.writeInt(getTitle().getBytes().length);
            dataOutputStream.write(getTitle().getBytes());

            //Send file name
            dataOutputStream.writeInt(fileName.getBytes().length);
            dataOutputStream.write(fileName.getBytes());

            //Send file content
            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            dataOutputStream.flush();

            updateUIForFile(false, "Me", fileName);

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    private void updateUIForText(boolean isReceived, String userName, String text) {
        text = text.replaceAll("\n", "<br>");
        String msgSender = isReceived ? userName : "Me";

        //Add new msg label to chat box panel
        JLabel label = new JLabel(String.format("<html><b>%s: </b>%s</html>", msgSender, text));
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        chatBox.add(label);
        chatBox.add(Box.createVerticalStrut(20));
        scrollToNewMsg();
    }

    private void updateUIForFile(boolean isReceived, String userName, String fileName) {
        fileName = "<u><i>" + fileName + "</i></u>";
        String fileSender = isReceived ? userName : "Me";

        //Add new msg label to chat box panel
        JLabel label = new JLabel(String.format("<html><b>%s: </b>%s</html>", fileSender, fileName));
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        if (isReceived) {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setName(String.valueOf(fileID));
            label.addMouseListener(getLabelClickListeners());
        }

        chatBox.add(label);
        chatBox.add(Box.createVerticalStrut(20));
        scrollToNewMsg();
    }

    private MouseListener getLabelClickListeners() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(
                        null,
                        "Do you want to download this file?",
                        "Warning",
                        JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {
                    JLabel label = (JLabel) e.getSource();

                    for (FileReceived fileReceived : fileReceivedArrayList) {
                        if (fileReceived.getId() == Integer.parseInt(label.getName())) {
                            try {
                                File file = new File(System.getProperty("user.home") + "/Downloads/" + fileReceived.getName());
                                FileOutputStream fileOutputStream = new FileOutputStream(file);
                                fileOutputStream.write(fileReceived.getData());
                                fileOutputStream.close();

                                JOptionPane.showMessageDialog(null, file, "File downloaded to", JOptionPane.PLAIN_MESSAGE);

                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        };
    }

    //Scroll to bottom of the mag list
    private void scrollToNewMsg() {
        JScrollBar verticalScrollBar = chatBoxScrollPane.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
        validate();
    }

    //This receives the massages and this will run on a separate thread because waiting for a msg is a blocking operation
    private void receiveMsg() {
        new Thread(() -> {
            try {
                //Connect to the socket. If this is the host, create the server socket
                socket = isHost ? new ServerSocket(PORT).accept() : new Socket(IP, PORT);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                //Send username while connecting to the server
                dataOutputStream.writeInt(getTitle().getBytes().length);
                dataOutputStream.write(getTitle().getBytes());

                //Receive massages while the socket is connected
                while (socket.isConnected()) {
                    try {
                        //Receive the boolean for a file. true if file is received
                        isFile = dataInputStream.readBoolean();

                        //Receive the username
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
                                    fileReceivedArrayList.add(new FileReceived(fileID, new String(msgBytes), fileContentBytes));
                                }
                            }

                            if (msgBytes != null) {
                                if (!isFile) {
                                    //If the msg was a text
                                    updateUIForText(true, new String(userNameBytes), new String(msgBytes));
                                } else {
                                    //If a msg was a file
                                    updateUIForFile(true, new String(userNameBytes), new String(msgBytes));
                                    fileID++;
                                }
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        closeEverything(socket, dataInputStream, dataOutputStream);
                        break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                closeEverything(socket, dataInputStream, dataOutputStream);
            }
        }).start();
    }

}
