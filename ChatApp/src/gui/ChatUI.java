package gui;

import client.Client;
import client.FileReceived;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ChatUI extends JFrame {

    private final Client client;
    private JPanel mainPanel, chatBox;
    private JButton buttonSendText, buttonSendFile;
    private JTextArea textBox;
    private JScrollPane chatBoxScrollPane;
    private int fileLabelID = 0;

    public ChatUI(String title) {
        setTitle(title);
        client = new Client(title);

        initialize();
        initListeners();

        new Thread(client).start();
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
                if (client.sendText(text)) {
                    createMessageLabel(false, "Me", text, false);

                    textBox.setText("");
                    textBox.grabFocus();
                }
            }
        });

        buttonSendFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose a file to send");
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                int dialogResult = JOptionPane.showConfirmDialog(null, "Are you sure you want to send this file?\n" + fileChooser.getSelectedFile().getName(), "Send file", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    if (fileChooser.getSelectedFile().length() > 0) {
                        if (client.sendFile(fileChooser.getSelectedFile()))
                            createMessageLabel(false, "Me", fileChooser.getSelectedFile().getName(), true);
                    } else
                        JOptionPane.showMessageDialog(null, "Can't send empty file", "Error!!!", JOptionPane.PLAIN_MESSAGE);
                }
            }
        });

        client.setClientStopListener(() -> {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });

        client.setMessageReceiveListener((isReceived, senderName, message, isFile) -> {
            createMessageLabel(isReceived, senderName, message, isFile);
        });
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
                int dialogResult = JOptionPane.showConfirmDialog(null, "Do you want to download this file?", "Warning", JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {
                    JLabel label = (JLabel) e.getSource();

                    for (FileReceived fileReceived : client.getReceivedFileList()) {
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
        };
    }

}
