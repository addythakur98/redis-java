package com.saksham.server;

import com.saksham.command.CommandHandler;
import com.saksham.protocol.RespParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket client;
    private final CommandHandler handler;          // was: DataStore store

    public ClientHandler(Socket client, CommandHandler handler) {
        this.client = client;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (client) {
            InputStream in = new BufferedInputStream(client.getInputStream());
            OutputStream out = client.getOutputStream();

            while (true) {
                List<String> command = RespParser.readCommand(in);
                if (command == null) break;
                if (command.isEmpty() || command.get(0).isEmpty()) continue;

                out.write(handler.execute(command));   // was: handle(command)
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}