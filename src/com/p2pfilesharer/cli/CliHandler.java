package com.p2pfilesharer.cli;

import com.p2pfilesharer.discovery.PeerDiscovery;
import com.p2pfilesharer.network.FileReceiver;
import com.p2pfilesharer.network.FileSender;
import com.p2pfilesharer.web.HttpFileServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;


public class CliHandler {

    private final Scanner scanner;
    private final PeerDiscovery peerDiscovery;
    private static final int P2P_PORT = 12345;
    private static final int HTTP_PORT = 8080;

    public CliHandler() {
        this.scanner = new Scanner(System.in);
        this.peerDiscovery = new PeerDiscovery();
    }


    public void start() {

        Thread discoveryThread = new Thread(peerDiscovery);
        discoveryThread.setDaemon(true);
        discoveryThread.start();

        while (true) {
            showMenu();
            try {
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        handleSendFileP2P();
                        break;
                    case 2:
                        handleReceiveFile();
                        break;
                    case 3:
                        handleSendFileWeb();
                        break;
                    case 4:
                        System.out.println("Exiting application. Goodbye!");
                        peerDiscovery.stop();
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 4.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }


    private void showMenu() {
        System.out.println("\n--- P2P File Sharer ---");
        System.out.println("1. Send a file (P2P)");
        System.out.println("2. Receive a file (P2P)");
        System.out.println("3. Send a file (Web Link)");
        System.out.println("4. Exit");
        System.out.print("Enter your choice: ");
    }


    private void handleSendFileP2P() {
        Set<String> peers = peerDiscovery.discoverPeers();
        List<String> peerList = new ArrayList<>(peers);

        String host;
        if (peerList.isEmpty()) {
            System.out.println("No peers found on the network.");
            System.out.print("Enter the receiver's IP address manually: ");
            host = scanner.nextLine();
        } else {
            System.out.println("Discovered Peers:");
            for (int i = 0; i < peerList.size(); i++) {
                System.out.println((i + 1) + ". " + peerList.get(i));
            }
            System.out.println((peerList.size() + 1) + ". Enter IP manually");
            System.out.print("Choose a peer to send to: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice > 0 && choice <= peerList.size()) {
                host = peerList.get(choice - 1);
            } else {
                System.out.print("Enter the receiver's IP address manually: ");
                host = scanner.nextLine();
            }
        }

        System.out.print("Enter the full path of the file to send: ");
        String filePath = expandPath(scanner.nextLine());

        System.out.print("Enable encryption? (y/n): ");
        String encryptChoice = scanner.nextLine();
        String password = null;
        if ("y".equalsIgnoreCase(encryptChoice)) {
            System.out.print("Enter a password for encryption: ");
            password = scanner.nextLine();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Error: File does not exist at the specified path.");
            return;
        }

        FileSender sender = new FileSender(host, P2P_PORT);
        try {
            sender.sendFile(file, password);
        } catch (Exception e) {
            System.err.println("File sending failed: " + e.getMessage());
        }
    }


    private void handleReceiveFile() {
        System.out.print("Enter the directory where you want to save the file: ");
        String saveDir = expandPath(scanner.nextLine());

        System.out.print("Is the incoming file encrypted? (y/n): ");
        String encryptChoice = scanner.nextLine();
        String password = null;
        if ("y".equalsIgnoreCase(encryptChoice)) {
            System.out.print("Enter the password for decryption: ");
            password = scanner.nextLine();
        }

        File dir = new File(saveDir);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("Error: Could not create the save directory.");
            return;
        }

        FileReceiver receiver = new FileReceiver(P2P_PORT);
        try {
            receiver.start(saveDir, password);
        } catch (Exception e) {
            System.err.println("Failed to receive file: " + e.getMessage());
            if (e instanceof javax.crypto.BadPaddingException) {
                System.err.println("This might be due to an incorrect password.");
            }
        }
    }


    private void handleSendFileWeb() {
        System.out.print("Enter the full path of the file to make available for download: ");
        String filePath = expandPath(scanner.nextLine());

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Error: File does not exist.");
            return;
        }

        try {
            String localIp = getLocalIpAddress();
            String publicIp = getPublicIpAddress();
            String downloadLink = "http://" + localIp + ":" + HTTP_PORT + "/download";

            HttpFileServer server = new HttpFileServer(file, HTTP_PORT);
            server.start();

            System.out.println("\n--- ✅ Download Ready ---");
            System.out.println("\n--- For users on the SAME Wi-Fi network ---");
            System.out.println("Link: " + downloadLink);


            System.out.println("\n--- For users on the INTERNET ---");
            System.out.println("Link: http://" + publicIp + ":" + HTTP_PORT + "/download");
            System.out.println("   (Requires port forwarding on your router for port " + HTTP_PORT + ")");

            System.out.println("\nThe server is running. Press Enter in this window to stop it.");
            scanner.nextLine();
            server.stop();

        } catch (IOException e) {
            System.err.println("Could not start the web server: " + e.getMessage());
        }
    }


    private String expandPath(String path) {
        if (path.startsWith("~" + File.separator)) {
            return System.getProperty("user.home") + path.substring(1);
        } else if (path.equals("~")) {
            return System.getProperty("user.home");
        }
        return path;
    }


    private String getPublicIpAddress() {
        try {
            java.net.URL url = new java.net.URL("https://api.ipify.org");
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream()))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            return "Could not resolve public IP";
        }
    }


    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(networkInterfaces)) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress inetAddress : Collections.list(ni.getInetAddresses())) {
                    if (inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Could not read network interfaces: " + e.getMessage());
        }
        return "127.0.0.1"; // Fallback
    }
}
