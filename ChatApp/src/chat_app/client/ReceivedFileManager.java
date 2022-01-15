package chat_app.client;

import chat_app.interfaces.listeners.FileDownloadListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ReceivedFileManager {
    private final ArrayList<ReceivedFile> receivedFiles = new ArrayList<>();

    private FileDownloadListener fileDownloadListener;

    public void downloadFile(int id) {
        for (ReceivedFile receivedFile : receivedFiles) {
            if (receivedFile.getId() == id) {
                try {
                    File file = new File(System.getProperty("user.home") + "/Downloads/" + receivedFile.getName());
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(receivedFile.getData());
                    fileOutputStream.close();

                    fileDownloadListener.onDownloadComplete(file);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addFileToList(ReceivedFile receivedFile) {
        receivedFiles.add(receivedFile);
    }

    public void setFileDownloadListener(FileDownloadListener fileDownloadListener) {
        this.fileDownloadListener = fileDownloadListener;
    }
}
