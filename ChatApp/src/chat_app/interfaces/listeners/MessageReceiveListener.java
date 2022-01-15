package chat_app.interfaces.listeners;

import chat_app.client.ReceivedFile;

public interface MessageReceiveListener {
    void onMessageReceive(String senderName, String message, boolean isFile, ReceivedFile receivedFile);
}
