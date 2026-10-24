package com.saksham.server;

import com.saksham.command.CommandHandler;
import com.saksham.persistence.Aof;
import com.saksham.store.DataStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RedisServer {
    private final int port;
    private final Aof.FsyncPolicy policy;

    public RedisServer(int port, Aof.FsyncPolicy policy) {
        this.port = port;
        this.policy = policy;
    }

    public void start() throws IOException {
        DataStore store = new DataStore();

        // 1. rebuild data from the log BEFORE accepting clients
        int loaded = Aof.replay("appendonly.aof", new CommandHandler(store, null));
        System.out.println("Loaded " + loaded + " commands from AOF");

        // 2. open the log for new writes
        Aof aof = new Aof("appendonly.aof", policy);
        CommandHandler handler = new CommandHandler(store, aof);

        // 3. background tasks: expiry cleaner + once-a-second fsync/flush
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> store.removeExpired(), 100, 100, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(() -> aof.tick(), 1, 1, TimeUnit.SECONDS);

        try (ServerSocket serverSocket = new ServerSocket(port);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("Listening on port : " + port + " (appendfsync " + policy + ")");
            while (true) {
                Socket client = serverSocket.accept();
                executor.submit(new ClientHandler(client, handler));
            }
        }
    }
}