import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ChatUI extends JFrame implements Runnable {
    public static final String IP = "localhost";
    public static final int PORT = 1234;
    private final ArrayList<FileReceived> fileReceivedArrayList = new ArrayList<>();

    private JPanel mainPanel, chatBox;
    private JButton buttonSendText, buttonSendFile;
    private JTextArea textBox;
    private JScrollPane chatBoxScrollPane;

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private boolean isFile;
    private int fileID = 0;

    public ChatUI(String title) {
        setTitle(title);

        initialize();
        initListeners();
        connectToServer();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setPreferredSize(new Dimension(600, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
        chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS));
    }

    private void initListeners() {
        buttonSendText.addActionListener(e -> {
            String text = textBox.getText().trim();
            if (!text.isBlank()) {
                sendText(text);
            }
        });

        buttonSendFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose a file to send");
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "Are you sure you want to send this file?\n" + fileChooser.getSelectedFile().getName(), "Send file", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    if (fileChooser.getSelectedFile().length() > 0)
                        sendFile(fileChooser.getSelectedFile());
                    else
                        JOptionPane.showMessageDialog(null, "Can't send empty file", "Error!!!", JOptionPane.PLAIN_MESSAGE);
                }
            }
        });

    }

    private void connectToServer() {
        try {
            //Connect to the socket. If this is the host, create the server socket
            socket = new Socket(IP, PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            //Send username to the server after connecting to the server
            dataOutputStream.writeInt(getTitle().getBytes().length);
            dataOutputStream.write(getTitle().getBytes());

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
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

            int dialogResult = JOptionPane.showConfirmDialog(null, "The server might be down. Try to reconnect?", "Error", JOptionPane.YES_NO_OPTION);

            if (dialogResult == JOptionPane.YES_OPTION) {
                connectToServer();
            } else {
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendText(String text) {
        try {
            isFile = false;

            //Send boolean for file or text. true if file is sent
            dataOutputStream.writeBoolean(isFile);

            //Send username
            dataOutputStream.writeInt(getTitle().getBytes().length);
            dataOutputStream.write(getTitle().getBytes());

            //Send text
            dataOutputStream.writeInt(text.getBytes().length);
            dataOutputStream.write(text.getBytes());

            dataOutputStream.flush();

            createTextLabel(false, "Me", text);
            textBox.setText("");
            textBox.grabFocus();

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
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

            createFileLabel(false, "Me", fileName);

        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    private void createTextLabel(boolean isReceived, String userName, String text) {
        text = text.replaceAll("\n", "<br>");
        String msgSender = isReceived ? userName : "Me";

        JLabel label = new JLabel(String.format("<html><b>%s: </b>%s</html>", msgSender, text));
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        label.setBorder(new EmptyBorder(10, 10, 10, 10));
        addLabelToChatBox(label);
    }

    private void createFileLabel(boolean isReceived, String userName, String fileName) {
        String fileSender = isReceived ? userName : "Me";

        JLabel label = new JLabel(String.format("<html><b>%s: </b><u><i>%s</i></u></html>", fileSender, fileName));
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        label.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (isReceived) {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setName(String.valueOf(fileID));
            label.addMouseListener(getLabelClickListeners());
        }

        addLabelToChatBox(label);
    }

    //Add new msg label to chat box panel
    private void addLabelToChatBox(JLabel label) {
        chatBox.add(label);
        chatBox.add(Box.createVerticalStrut(20));
        scrollToNewMsg();
    }

    //Scroll to bottom of the mag list
    private void scrollToNewMsg() {
        JScrollBar verticalScrollBar = chatBoxScrollPane.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
        validate();
    }

    //Mouse listener for label
    private MouseListener getLabelClickListeners() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "Do you want to download this file?", "Warning", JOptionPane.YES_NO_OPTION);

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

    //This receives the massages and this will run on a separate thread because waiting for a msg is a blocking operation
    @Override
    public void run() {
        try {
            //Receive massages while the socket is connected
            while (socket.isConnected()) {
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

                    if (msgBytesLength > 0) dataInputStream.readFully(msgBytes, 0, msgBytesLength);

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
                            createTextLabel(true, new String(userNameBytes), new String(msgBytes));
                        } else {
                            //If a msg was a file
                            createFileLabel(true, new String(userNameBytes), new String(msgBytes));
                            fileID++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

}
