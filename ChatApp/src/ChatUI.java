import javax.swing.*;
import java.awt.*;

public class ChatUI extends JFrame {
    private JPanel mainPanel;
    private JButton btnSendText, btnSendFile;
    private JTextArea msgBox;
    private JPanel chatBox;
    private JScrollPane chatBoxScrollPane;


    public ChatUI() {

        initialize();

        btnSendText.addActionListener(e -> {
            String msg = msgBox.getText().strip();

            if (!msg.isBlank()) {
                msg = msg.replaceAll("\n", "<br>");
                JLabel label = new JLabel("<html>" + msg + "</html");
                label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
                label.setForeground(Color.decode(msg.length() % 2 == 0 ? "#608FFF" : "#000000"));
                label.setHorizontalAlignment(msg.length() % 2 == 0 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                chatBox.add(label);
                chatBox.add(Box.createVerticalStrut(20));
                JScrollBar verticalScrollBar = chatBoxScrollPane.getVerticalScrollBar();
                SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
                msgBox.setText("");
                msgBox.grabFocus();
                validate();
            }
        });
    }

    private void initialize() {
        setTitle("Chat");
        setContentPane(mainPanel);
        setPreferredSize(new Dimension(600, 800));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);

        chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS));
        chatBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
}
