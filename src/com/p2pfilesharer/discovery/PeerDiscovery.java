package com.p2pfilesharer.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public class PeerDiscovery implements Runnable {

    private static final int DISCOVERY_PORT = 12346;
    private static final String DISCOVERY_REQUEST = "P2P_FILE_SHARER_DISCOVERY_REQUEST";
    private static final String DISCOVERY_RESPONSE = "P2P_FILE_SHARER_DISCOVERY_RESPONSE";

    private DatagramSocket listeningSocket;

    @Override
    public void run() {
        try {
            listeningSocket = new DatagramSocket(DISCOVERY_PORT);
            listeningSocket.setBroadcast(true);
            System.out.println("Peer discovery listener started on UDP port " + DISCOVERY_PORT);

            byte[] receiveBuffer = new byte[1024];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                listeningSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if (message.equals(DISCOVERY_REQUEST)) {
                    byte[] responseData = DISCOVERY_RESPONSE.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, receivePacket.getAddress(), receivePacket.getPort());
                    listeningSocket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.out.println("PeerDiscovery: Could not bind to port " + DISCOVERY_PORT + ". Another instance is likely running. This instance will not respond to discovery requests.");
        } finally {
            if (listeningSocket != null && !listeningSocket.isClosed()) {
                listeningSocket.close();
            }
        }
    }


    public Set<String> discoverPeers() {
        Set<String> discoveredPeers = new HashSet<>();
        try (DatagramSocket discoverySocket = new DatagramSocket()) {
            discoverySocket.setBroadcast(true);
            System.out.println("Searching for peers on the network...");

            byte[] requestData = DISCOVERY_REQUEST.getBytes();

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) continue;
                    DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, broadcast, DISCOVERY_PORT);
                    discoverySocket.send(sendPacket);
                }
            }

            long startTime = System.currentTimeMillis();
            byte[] receiveBuffer = new byte[1024];
            while (System.currentTimeMillis() - startTime < 3000) { // 3-second discovery window
                discoverySocket.setSoTimeout(1000); // Set a timeout for each receive call
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try {
                    discoverySocket.receive(receivePacket);
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    if (message.equals(DISCOVERY_RESPONSE)) {
                        discoveredPeers.add(receivePacket.getAddress().getHostAddress());
                    }
                } catch (java.net.SocketTimeoutException e) {
                }
            }
        } catch (IOException e) {
            System.err.println("PeerDiscovery: Error during discovery broadcast. " + e.getMessage());
        }
        return discoveredPeers;
    }

    public void stop() {
        if (listeningSocket != null && !listeningSocket.isClosed()) {
            listeningSocket.close();
            System.out.println("Peer discovery listener stopped.");
        }
    }
}
