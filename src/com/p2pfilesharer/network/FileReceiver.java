package com.p2pfilesharer.network;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileReceiver {

    private final int port;

    public FileReceiver(int port) {

        this.port = port;
    }

    public void start(String saveDir) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Receiver started. Waiting for a sender on port " + port + "...");

            Socket clientSocket =  serverSocket.accept();
            System.out.println("Sender connected: " + clientSocket.getInetAddress().getHostAddress());

            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                System.out.println("Receiving file: " + fileName + " (" + (fileSize / 1024 / 1024) + " MB)");

                File fileToSave = new File(saveDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                    byte[] buffer = new byte[8192]; // 8KB buffer
                    int bytesRead;
                    long totalReceived = 0;

                    while ((bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalReceived))) != -1) {
                        if (totalReceived >= fileSize) break;

                        fos.write(buffer, 0, bytesRead);
                        totalReceived += bytesRead;

                        printProgress(totalReceived, fileSize);
                    }
                    fos.flush();

                }

                System.out.println("\nFile received successfully! Saved to: " + fileToSave.getAbsolutePath());
            }

        } catch (IOException e) {
            System.err.println("An error occurred in the receiver: " + e.getMessage());
            throw e;
        }
    }

    private void printProgress(long totalReceived, long fileSize) {
        if (fileSize == 0) return;
        int progress = (int) ((totalReceived * 100) / fileSize);
        System.out.print("\rProgress: " + progress + "% ");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java com.p2pfilesharer.network.FileReceiver <port> <save_directory>");
            System.out.println("Example: java com.p2pfilesharer.network.FileReceiver 12345 C:/Users/Me/Downloads");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String saveDir = args[1];

        FileReceiver receiver = new FileReceiver(port);
        try {
            receiver.start(saveDir);
        } catch (IOException e) {
            System.out.println("Failed to start receiver or receive file");
            e.printStackTrace();
        }
    }

}
