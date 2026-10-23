package com.saksham.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.saksham.store.DataStore;

public class RedisServer {
    private final int port;

    public RedisServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            System.out.println("Listening on port : " + port);
            
            DataStore store = new DataStore();

            while (true) {
                Socket client = serverSocket.accept();          // wait for the next client
                executor.submit(new ClientHandler(client,store));     // hand it to a new virtual thread
            }
        }
    }
}