package com.saksham.protocol;

import java.nio.charset.StandardCharsets;

public class RespWriter {
    public static byte[] simpleString(String s){
        return ("+"+s+"\r\n").getBytes(StandardCharsets.UTF_8);
    }
    public static byte[] bulkyString(String s){
        byte[] data=s.getBytes(StandardCharsets.UTF_8);
        return ("$"+data.length+"\r\n"+s+"\r\n").getBytes(StandardCharsets.UTF_8);
    }
    public static byte[] error(String msg){
        return ("-"+msg+"\r\n").getBytes(StandardCharsets.UTF_8);
    }
}