package com.saksham;

import com.saksham.server.RedisServer;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        new RedisServer(6379).start();
    }
}