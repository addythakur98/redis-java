package com.saksham.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class RedisServer {
    private final int port;

    public RedisServer(int port) {
        this.port = port;
    }
    public void start() throws IOException{
        try (ServerSocket serverSocket=new ServerSocket(port)){
            System.out.println("Listening on port : "+port);
            try (Socket client=serverSocket.accept()){
                System.out.println("Client Connected");
                InputStream in=client.getInputStream();
                OutputStream out=client.getOutputStream();
                byte[] buf=new byte[1024];
                int n;
                while ((n=in.read(buf))!=-1){
                    String text=new String(buf,0,n,StandardCharsets.UTF_8);
                    System.out.println("Got " + n + " bytes : " + text);
                    out.write("+PONG\r\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
                System.out.println("Client Disconnected!");
            }
        }
    }
}
