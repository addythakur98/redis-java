package com.saksham;

import com.saksham.persistence.Aof;
import com.saksham.server.RedisServer;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        Aof.FsyncPolicy policy = Aof.FsyncPolicy.EVERYSEC;              // default, like real Redis
        if (args.length > 0) policy = Aof.FsyncPolicy.valueOf(args[0].toUpperCase());
        new RedisServer(6379, policy).start();
    }
}