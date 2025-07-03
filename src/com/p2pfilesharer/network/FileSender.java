package com.p2pfilesharer.network;

import com.p2pfilesharer.encryption.CryptoUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileSender {

    private final String host;
    private final int port;

    public FileSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendFile(File file, String password) throws Exception {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connection established with " + host);

            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);

            boolean isEncrypted = (password != null && !password.isEmpty());
            dos.writeBoolean(isEncrypted);

            OutputStream finalOutStream;
            if (isEncrypted) {
                System.out.println("Sending file with AES encryption.");
                SecretKey key = CryptoUtils.getKeyFromPassword(password);
                IvParameterSpec iv = CryptoUtils.generateIv();

                dos.write(iv.getIV()); // Send the IV first

                Cipher encryptCipher = CryptoUtils.getEncryptCipher(key, iv);
                finalOutStream = new CipherOutputStream(dos, encryptCipher);
            } else {
                System.out.println("Sending file without encryption.");
                finalOutStream = dos;
            }

            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            System.out.println("Sending file: " + file.getName() + " (" + (file.length() / 1024 / 1024) + " MB)");

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalSent = 0;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    finalOutStream.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                    printProgress(totalSent, file.length());
                }
            }
            finalOutStream.flush();
            // Important: Close the CipherOutputStream to ensure final block is encrypted and written
            if (finalOutStream instanceof CipherOutputStream) {
                finalOutStream.close();
            }
            System.out.println("\nFile transfer complete.");
        }
    }

    private void printProgress(long totalSent, long fileSize) {
        if (fileSize == 0) return;
        int progress = (int) ((totalSent * 100) / fileSize);
        System.out.print("\rProgress: " + progress + "% ");
    }
}
