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
    private final boolean isServer;
    private final ArrayList<FileReceived> fileReceivedArrayList = new ArrayList<>();
    private boolean isFile;
    private JPanel mainPanel, chatBox;
    private JButton btnSendText, btnSendFile;
    private JTextArea msgBox;
    private JScrollPane chatBoxScrollPane;
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private int fileID = 0;

    public ChatUI(String title, boolean isServer) {
        setTitle(title);
        this.isServer = isServer;

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

    private void receiveMsg() {
        new Thread(() -> {
            try {
                socket = isServer ? new ServerSocket(PORT).accept() : new Socket(IP, PORT);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                while (socket.isConnected()) {
                    try {
                        isFile = dataInputStream.readBoolean();

                        int msgBytesLength = dataInputStream.readInt();
                        byte[] msgBytes = new byte[msgBytesLength];
                        if (msgBytesLength > 0) {
                            dataInputStream.readFully(msgBytes, 0, msgBytesLength);

                            if (isFile) {
                                int fileContentLength = dataInputStream.readInt();
                                if (fileContentLength > 0) {
                                    byte[] fileContentBytes = new byte[fileContentLength];
                                    dataInputStream.readFully(fileContentBytes, 0, fileContentLength);

                                    fileReceivedArrayList.add(new FileReceived(fileID, new String(msgBytes), fileContentBytes));
                                }

                            }

                            if (msgBytes != null) {
                                if (!isFile) {
                                    updateUIForText(true, new String(msgBytes));
                                } else {
                                    updateUIForFile(true, new String(msgBytes));
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
                System.out.println("Error! Server not ready");
                e.printStackTrace();
                closeEverything(socket, dataInputStream, dataOutputStream);
            }
        }).start();
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

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendText(String text) {
        isFile = false;

        try {
            dataOutputStream.writeBoolean(isFile);
            dataOutputStream.writeInt(text.getBytes().length);
            dataOutputStream.write(text.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

        msgBox.setText("");
        msgBox.grabFocus();
        updateUIForText(false, text);
    }


    private void sendFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
            String fileName = file.getName();

            byte[] fileContentBytes = new byte[(int) file.length()];
            fileInputStream.read(fileContentBytes);

            isFile = true;

            dataOutputStream.writeBoolean(isFile);

            //Send file name
            dataOutputStream.writeInt(fileName.getBytes().length);
            dataOutputStream.write(fileName.getBytes());

            //Send file content
            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);

            updateUIForFile(false, fileName);

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void updateUIForText(boolean isReceived, String text) {
        text = text.replaceAll("\n", "<br>");
        String msgSender = isReceived ? "Sender" : "Me";


        //Add new msg label to chat box panel
        JLabel label = new JLabel(String.format("<html><b>%s: </b>%s</html>", msgSender, text));
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        chatBox.add(label);
        chatBox.add(Box.createVerticalStrut(20));
        scrollToNewMsg();
    }

    private void updateUIForFile(boolean isReceived, String fileName) {
        fileName = "<u><i>" + fileName + "</i></u>";
        String fileSender = isReceived ? "Sender" : "Me";

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

    private void scrollToNewMsg() {
        //Scroll to bottom
        JScrollBar verticalScrollBar = chatBoxScrollPane.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
        validate();
    }
}
