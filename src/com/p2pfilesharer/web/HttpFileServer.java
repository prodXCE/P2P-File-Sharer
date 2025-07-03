package com.p2pfilesharer.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HttpFileServer {

    private HttpServer server;
    private final File fileToSend;
    private final int port;

    public HttpFileServer(File fileToSend, int port) {
        this.fileToSend = fileToSend;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/download", new FileDownloadHandler());

        server.setExecutor(null);

        new Thread(server::start).start();

        System.out.println("HTTP Server started on port " + port + ". Ready to serve the file.");

    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP Server stopped");
        }
    }

    private class FileDownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileToSend.getName() + "\"");
            exchange.sendResponseHeaders(200, fileToSend.length());
            try (
                OutputStream os = exchange.getResponseBody();
                FileInputStream fis = new FileInputStream(fileToSend)
            ) {
                System.out.println("Client connected: " + exchange.getRemoteAddress() + ". Starting download...");

                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                }
                os.flush();
                System.out.println("Download for " + exchange.getRemoteAddress() + " completed.");

            } catch (IOException e) {
                System.err.println("Error during file streaming: " + e.getMessage());
            } finally {
                exchange.close();
            }
        }
    }
}


