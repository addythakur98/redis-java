package com.saksham.protocol;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RespParserTest {

    // helper: turn a String into a fake input stream
    private InputStream stream(String raw) {
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesASimpleCommand() throws IOException {
        InputStream in = stream("*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n");
        assertEquals(List.of("ECHO", "hello"), RespParser.readCommand(in));
    }

    @Test
    void parsesPipelinedCommands() throws IOException {
        InputStream in = stream("*1\r\n$4\r\nPING\r\n*2\r\n$3\r\nGET\r\n$1\r\nk\r\n");
        assertEquals(List.of("PING"), RespParser.readCommand(in));
        assertEquals(List.of("GET", "k"), RespParser.readCommand(in));
    }

    @Test
    void parsesInlineCommand() throws IOException {
        InputStream in = stream("PING\r\n");
        assertEquals(List.of("PING"), RespParser.readCommand(in));
    }

    @Test
    void valueCanContainCrlf() throws IOException {      // binary-safe
        InputStream in = stream("*2\r\n$4\r\nECHO\r\n$4\r\na\r\nb\r\n");
        assertEquals(List.of("ECHO", "a\r\nb"), RespParser.readCommand(in));
    }

    @Test
    void returnsNullWhenStreamEnds() throws IOException {
        assertNull(RespParser.readCommand(stream("")));
    }
}