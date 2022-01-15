package chat_app.ui;

import chat_app.client.ClientConnectionController;
import chat_app.client.ClientController;
import chat_app.client.ReceivedFileManager;
import chat_app.server.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

public class ChatUI extends JFrame {

    private final ClientController clientController;
    private final ReceivedFileManager receivedFileManager;
    private final ClientConnectionController clientConnectionController;

    private JPanel mainPanel, chatBox;
    private JButton buttonSendText, buttonSendFile;
    private JTextArea textBox;
    private JScrollPane chatBoxScrollPane;

    private int fileLabelID = 0;

    public ChatUI(String title) {
        setTitle(title);
        clientConnectionController = new ClientConnectionController(Server.IP, Server.PORT);
        clientController = new ClientController(title, clientConnectionController);
        receivedFileManager = new ReceivedFileManager();

        initialize();
        initListeners();

        new Thread(clientController).start();
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
            sendTextMessage();
        });

        buttonSendFile.addActionListener(e -> {
            sendFileMessage();
        });

        clientConnectionController.setDisconnectListener(() -> {
            JOptionPane.showMessageDialog(null, "The server might be down. Try restarting the app.");
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });

        clientController.setMessageReceiveListener((senderName, message, isFile, receivedFile) -> {
            if (isFile && receivedFile != null)
                receivedFileManager.addFileToList(receivedFile);

            createMessageLabel(true, senderName, message, isFile);
        });

        receivedFileManager.setFileDownloadListener(file -> {
            JOptionPane.showMessageDialog(null, file, "File downloaded to", JOptionPane.PLAIN_MESSAGE);
        });
    }

    private void sendTextMessage() {
        String text = textBox.getText().trim();
        if (!text.isBlank()) {
            if (clientController.sendText(text)) {
                createMessageLabel(false, "Me", text, false);

                textBox.setText("");
                textBox.grabFocus();
            }
        }
    }

    private void sendFileMessage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose a file to send");
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            int dialogResult = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to send this file?\n" + fileChooser.getSelectedFile().getName(),
                    "Send file",
                    JOptionPane.YES_NO_OPTION
            );

            if (dialogResult == JOptionPane.YES_OPTION) {
                if (fileChooser.getSelectedFile().length() > 0) {
                    if (clientController.sendFile(fileChooser.getSelectedFile()))
                        createMessageLabel(false, "Me", fileChooser.getSelectedFile().getName(), true);
                } else
                    JOptionPane.showMessageDialog(
                            null,
                            "Can't send empty file",
                            "Error!!!",
                            JOptionPane.PLAIN_MESSAGE
                    );
            }
        }
    }

    //Create a label for messages
    private void createMessageLabel(boolean isReceived, String userName, String text, boolean isFile) {
        ChatItem chatItem = new ChatItem(userName, text, isFile);
        if (isReceived && isFile) {
            chatItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chatItem.setName(String.valueOf(fileLabelID));
            chatItem.addMouseListener(getLabelClickListeners());

            fileLabelID++;
        }
        chatBox.add(chatItem);
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
    private MouseClickListener getLabelClickListeners() {
        return new MouseClickListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int dialogResult = JOptionPane.showConfirmDialog(
                        null,
                        "Do you want to download this file?",
                        "Warning",
                        JOptionPane.YES_NO_OPTION
                );

                if (dialogResult == JOptionPane.YES_OPTION) {
                    JLabel label = (JLabel) e.getSource();
                    receivedFileManager.downloadFile(Integer.parseInt(label.getName()));
                }
            }
        };
    }

}
