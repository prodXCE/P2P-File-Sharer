package com.p2pfilesharer.network;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class FileSender {

    private final String host;
    private final int port;

    public FileSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendFile(File file) throws IOException {
        try (
                Socket socket = new Socket(host, port);
                OutputStream out = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                FileInputStream fis = new FileInputStream(file)
                ) {
            System.out.println("Connection established with " + host);

            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            System.out.println("Sending file: " + file.getName() + " (" + (file.length() / 1024 / 1024) + " MB)");

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
                printProgress(totalSent, file.length());
            }

            dos.flush();
            System.out.println("\nFile transfer complete.");

        } catch (UnknownHostException e) {
            System.err.println("Error: Host could not be found: " + host);
            throw e;
        } catch (IOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
            throw e;
        }

    }

    private void printProgress(long totalSent, long fileSize) {
        if (fileSize == 0) return;
        int progress = (int) ((totalSent * 100) / fileSize);
        System.out.print("\rProgress: " + progress + "% ");
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java com.p2pfilesharer.network.FileSender <receiver_ip> <port> <file_path>");
            System.out.println("Example: java com.p2pfilesharer.network.FileSender localhost 12345 C:/Users/Me/Desktop/myvideo.mp4");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String filePath = args[2];
        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("Error: File does not exist at the specified path: " + filePath);
            return;
        }

        FileSender sender = new FileSender(host, port);
        try {
            sender.sendFile(file);
        } catch (IOException e) {
            System.out.println("File Sending Failed.");
            e.printStackTrace();
        }
    }
}
