package chat_app.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChatItem extends JLabel {
    private final boolean isFile;
    private String sender;
    private String message;

    public ChatItem(String sender, String message, boolean isFile) {
        this.sender = sender;
        this.message = message.replaceAll("\n", "<br>");
        this.isFile = isFile;

        init();
    }

    private void init() {
        sender = "<b>" + sender + ":" + "</b>";
        if (isFile)
            message = "<u><i>" + message + "</i></u>";

        String text = String.format("<html>%s %s</html>", sender, message);

        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setText(text);
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFile() {
        return isFile;
    }

}
