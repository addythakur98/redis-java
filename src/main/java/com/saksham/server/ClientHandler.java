package com.saksham.server;

import com.saksham.protocol.RespParser;
import com.saksham.protocol.RespWriter;
import com.saksham.store.DataStore;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket client;
    private final DataStore store;
    public ClientHandler(Socket client,DataStore store){
        this.client=client;
        this.store=store;
    }
    @Override
    public void run() {
        try (client) {
            InputStream in = new BufferedInputStream(client.getInputStream());
            OutputStream out = client.getOutputStream();

            while (true) {
                List<String> command = RespParser.readCommand(in);
                if (command == null) break;                 // client disconnected
                if (command.isEmpty() || command.get(0).isEmpty()) continue;            // blank line: ignore

                out.write(handle(command));
                out.flush();
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

            case "SET":
                if (command.size()==3){
                    store.set(command.get(1),command.get(2));
                    return RespWriter.simpleString("OK");
                }
                else if (command.size()==5){
                    String option = command.get(3).toUpperCase();
                    long amount;
                    try {
                        amount = Long.parseLong(command.get(4));
                    } catch (NumberFormatException e){
                        return RespWriter.error("ERR value is not an integer or out of range");
                    }
                    long ttlMillis;
                    if (option.equals("EX")) ttlMillis = amount * 1000;
                    else if (option.equals("PX")) ttlMillis = amount;
                    else return RespWriter.error("ERR syntax error");

                    store.setWithTtl(command.get(1), command.get(2), ttlMillis);
                    return RespWriter.simpleString("OK");
                }
                return RespWriter.error("ERR wrong number of arguments for 'set' command");

            case "GET":
                if (command.size()!=2)
                    return RespWriter.error("ERR wrong number of arguments for 'get' command");
                String value=store.get(command.get(1));
                if (value==null) return RespWriter.nullBulk();
                return RespWriter.bulkyString(value);

            case "DEL":
                if (command.size()<2)
                    return RespWriter.error("ERR wrong number of arguments for 'del' command");
                int deleted=0;
                for (int i=1;i<command.size();i++) if (store.delete(command.get(i))) deleted++;
                return RespWriter.integer(deleted);

            case "EXISTS":
                if (command.size()<2)
                    return RespWriter.error("ERR wrong number of arguments for 'exists' command");
                int found=0;
                for (int i=1;i<command.size();i++) if (store.exists(command.get(i))) found++;
                return RespWriter.integer(found);

            case "EXPIRE":
                if (command.size()!=3) return RespWriter.error("ERR wrong number of arguments for 'expire' command");
                long seconds;
                try {
                    seconds = Long.parseLong(command.get(2));
                } catch (NumberFormatException e) {
                    return RespWriter.error("ERR value is not an integer or out of range");
                }
                return RespWriter.integer(store.expire(command.get(1), seconds) ? 1 : 0);

            case "DBSIZE":
                return RespWriter.integer(store.size());
            
            case "TTL":
                if (command.size()!=2) return RespWriter.error("ERR wrong number of arguments for 'ttl' command");
                long data=store.ttl(command.get(1));
                return RespWriter.integer(data);
            default:
                return RespWriter.error("ERR unknown command '" + command.get(0) + "'");
        }
    }
}