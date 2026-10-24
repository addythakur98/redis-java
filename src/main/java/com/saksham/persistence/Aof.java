package com.saksham.persistence;

import com.saksham.protocol.RespWriter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.List;

import com.saksham.command.CommandHandler;
import com.saksham.protocol.RespParser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class Aof {

    public enum FsyncPolicy { ALWAYS, EVERYSEC, NO }

    private final FsyncPolicy policy;
    private final FileOutputStream file;          // the real file (needed for fsync)
    private final BufferedOutputStream out;       // fast buffered writes on top of it

    public Aof(String path, FsyncPolicy policy) throws IOException {
        this.policy = policy;
        this.file = new FileOutputStream(path, true);    // true = append, don't overwrite
        this.out = new BufferedOutputStream(file);
    }

    // called after every successful write command
    public synchronized void append(List<String> command) {
        try {
            out.write(RespWriter.command(command));
            if (policy == FsyncPolicy.ALWAYS) fsync();
        } catch (IOException e) {
            System.out.println("AOF write failed: " + e.getMessage());
        }
    }

    // called once per second by a background task
    public synchronized void tick() {
        try {
            if (policy == FsyncPolicy.EVERYSEC) fsync();
            else if (policy == FsyncPolicy.NO) out.flush();   // hand to the OS, no forced disk write
        } catch (IOException e) {
            System.out.println("AOF flush failed: " + e.getMessage());
        }
    }

    // push data all the way to the physical disk
    private void fsync() throws IOException {
        out.flush();                        // Java buffer → OS
        file.getChannel().force(false);     // OS cache → disk  (this is the "fsync")
    }
    // read the log and re-run every command; returns how many were loaded
    public static int replay(String path, CommandHandler handler) {
        File f = new File(path);
        if (!f.exists()) return 0;
        int count = 0;
        long goodBytes = 0;                 // file position right after the last complete command
        boolean truncated = false;

        try (CountingInputStream in = new CountingInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            while (true) {
                List<String> command = RespParser.readCommand(in);
                if (command == null) break;           // clean end of file
                handler.execute(command);
                count++;
                goodBytes = in.count;                 // everything up to here is valid
            }
        } catch (Exception e) {
            truncated = true;                         // half-written command at the end
        }

        if (truncated) {
            try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
                raf.setLength(goodBytes);             // chop off the broken tail
            } catch (IOException e) {
                System.out.println("Could not repair AOF: " + e.getMessage());
            }
            System.out.println("AOF had an incomplete last command; repaired it (kept " + goodBytes + " bytes)");
        }
        return count;
    }

    // wraps a stream and counts how many bytes have been read through it
    private static class CountingInputStream extends FilterInputStream {
        long count = 0;

        CountingInputStream(InputStream in) { super(in); }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) count++;
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = super.read(buf, off, len);
            if (n > 0) count += n;
            return n;
        }
    }
}