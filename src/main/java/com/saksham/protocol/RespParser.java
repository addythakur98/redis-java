package com.saksham.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespParser {

    static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                in.read();
                return sb.toString();
            }
            sb.append((char) b);
        }
        return null;                         // stream ended: client disconnected
    }

    public static List<String> readCommand(InputStream in) throws IOException {
        String first = readLine(in);
        if (first == null) return null;      // client disconnected
        if (!first.startsWith("*")) {
            // inline command, e.g. "PING" or "ECHO hi": split on spaces
            return new ArrayList<>(List.of(first.trim().split("\\s+")));
        }

        int count = Integer.parseInt(first.substring(1));
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String lenLine = readLine(in);
            int length = Integer.parseInt(lenLine.substring(1));
            byte[] data = in.readNBytes(length);
            in.readNBytes(2);
            parts.add(new String(data, StandardCharsets.UTF_8));
        }
        return parts;
    }
}