package com.saksham.server;

import com.saksham.protocol.RespParser;
import com.saksham.protocol.RespWriter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket client;
    public ClientHandler(Socket client){
        this.client=client;
    }
    @Override
    public void run() {
        try (client) {
            InputStream in = new BufferedInputStream(client.getInputStream());
            OutputStream out = client.getOutputStream();

            while (true) {
                List<String> command = RespParser.readCommand(in);
                if (command == null) break;                 // client disconnected
                if (command.isEmpty()) continue;            // blank line: ignore

                out.write(handle(command));
                out.flush();
                if (command.isEmpty() || command.get(0).isEmpty()) continue;
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private byte[] handle(List<String> command) {
        String name = command.get(0).toUpperCase();

        switch (name) {
            case "PING":
                if (command.size() == 1) return RespWriter.simpleString("PONG");
                return RespWriter.bulkyString(command.get(1));

            case "ECHO":
                if (command.size() != 2)
                    return RespWriter.error("ERR wrong number of arguments for 'echo' command");
                return RespWriter.bulkyString(command.get(1));

            default:
                return RespWriter.error("ERR unknown command '" + command.get(0) + "'");
        }
    }
}