package com.p2pfilesharer.network;

import com.p2pfilesharer.encryption.CryptoUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileReceiver {

    private final int port;

    public FileReceiver(int port) {
        this.port = port;
    }

    public void start(String saveDir, String password) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Receiver started. Waiting for a sender on port " + port + "...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Sender connected: " + clientSocket.getInetAddress().getHostAddress());

            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                boolean isEncrypted = dis.readBoolean();
                InputStream inputStream;

                if (isEncrypted) {
                    if (password == null || password.isEmpty()) {
                        throw new IOException("Received an encrypted file but no password was provided.");
                    }
                    System.out.println("Receiving an encrypted file.");

                    byte[] ivBytes = new byte[16];
                    dis.readFully(ivBytes);
                    IvParameterSpec iv = new IvParameterSpec(ivBytes);

                    SecretKey key = CryptoUtils.getKeyFromPassword(password);
                    Cipher decryptCipher = CryptoUtils.getDecryptCipher(key, iv);

                    inputStream = new CipherInputStream(dis, decryptCipher);
                } else {
                    System.out.println("Receiving an unencrypted file.");
                    inputStream = dis;
                }

                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                System.out.println("Receiving file: " + fileName + " (" + (fileSize / 1024 / 1024) + " MB)");
                File fileToSave = new File(saveDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalReceived = 0;

                    // We need to use DataInputStream again to read from the potentially decrypted stream
                    DataInputStream dataStream = new DataInputStream(inputStream);

                    while ((bytesRead = dataStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalReceived += bytesRead;
                        printProgress(totalReceived, fileSize);
                    }
                    fos.flush();
                }
                System.out.println("\nFile received successfully!");
            }
        }
    }

    private void printProgress(long totalReceived, long fileSize) {
        if (fileSize == 0) return;
        int progress = (int) ((totalReceived * 100) / fileSize);
        System.out.print("\rProgress: " + progress + "% ");
    }
}
