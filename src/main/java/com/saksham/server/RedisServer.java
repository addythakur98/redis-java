package com.saksham.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
            ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
            cleaner.scheduleAtFixedRate(() -> store.removeExpired(), 100, 100, TimeUnit.MILLISECONDS);

            while (true) {
                Socket client = serverSocket.accept();          // wait for the next client
                executor.submit(new ClientHandler(client,store));     // hand it to a new virtual thread
            }
        }
    }
}