package com.saksham.command;

import com.saksham.persistence.Aof;
import com.saksham.protocol.RespWriter;
import com.saksham.store.DataStore;

import java.util.List;

public class CommandHandler {
    private final DataStore store;
    private final Aof aof;              // null during replay, so replayed commands aren't logged again

    public CommandHandler(DataStore store, Aof aof) {
        this.store = store;
        this.aof = aof;
    }

    public byte[] execute(List<String> command) {
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
                return set(command);

            case "GET": {
                if (command.size() != 2)
                    return RespWriter.error("ERR wrong number of arguments for 'get' command");
                String value = store.get(command.get(1));
                if (value == null) return RespWriter.nullBulk();
                return RespWriter.bulkyString(value);
            }

            case "DEL": {
                if (command.size() < 2)
                    return RespWriter.error("ERR wrong number of arguments for 'del' command");
                int deleted = 0;
                for (int i = 1; i < command.size(); i++)
                    if (store.delete(command.get(i))) deleted++;
                if (deleted > 0) log(command);                                   // AOF
                return RespWriter.integer(deleted);
            }

            case "EXISTS": {
                if (command.size() < 2)
                    return RespWriter.error("ERR wrong number of arguments for 'exists' command");
                int found = 0;
                for (int i = 1; i < command.size(); i++)
                    if (store.exists(command.get(i))) found++;
                return RespWriter.integer(found);
            }

            case "EXPIRE": {
                if (command.size() != 3)
                    return RespWriter.error("ERR wrong number of arguments for 'expire' command");
                long seconds;
                try { seconds = Long.parseLong(command.get(2)); }
                catch (NumberFormatException e) {
                    return RespWriter.error("ERR value is not an integer or out of range");
                }
                long at = System.currentTimeMillis() + seconds * 1000;
                boolean ok = store.expireAt(command.get(1), at);
                if (ok) log(List.of("PEXPIREAT", command.get(1), String.valueOf(at)));  // AOF: absolute time
                return RespWriter.integer(ok ? 1 : 0);
            }

            case "PEXPIREAT": {                                                   // AOF: used by replay
                if (command.size() != 3)
                    return RespWriter.error("ERR wrong number of arguments for 'pexpireat' command");
                long at;
                try { at = Long.parseLong(command.get(2)); }
                catch (NumberFormatException e) {
                    return RespWriter.error("ERR value is not an integer or out of range");
                }
                boolean ok = store.expireAt(command.get(1), at);
                if (ok) log(command);
                return RespWriter.integer(ok ? 1 : 0);
            }

            case "TTL": {
                if (command.size() != 2)
                    return RespWriter.error("ERR wrong number of arguments for 'ttl' command");
                return RespWriter.integer(store.ttl(command.get(1)));
            }

            case "DBSIZE":
                return RespWriter.integer(store.size());

            default:
                return RespWriter.error("ERR unknown command '" + command.get(0) + "'");
        }
    }

    // SET k v  |  SET k v EX s  |  SET k v PX ms  |  SET k v PXAT absolute-ms
    private byte[] set(List<String> command) {
        if (command.size() == 3) {
            store.set(command.get(1), command.get(2));
            log(command);                                                        // AOF
            return RespWriter.simpleString("OK");
        }
        if (command.size() == 5) {
            String option = command.get(3).toUpperCase();
            long amount;
            try { amount = Long.parseLong(command.get(4)); }
            catch (NumberFormatException e) {
                return RespWriter.error("ERR value is not an integer or out of range");
            }
            long now = System.currentTimeMillis();
            long expiresAt;
            if (option.equals("EX")) expiresAt = now + amount * 1000;
            else if (option.equals("PX")) expiresAt = now + amount;
            else if (option.equals("PXAT")) expiresAt = amount;                 // already absolute
            else return RespWriter.error("ERR syntax error");

            store.setWithExpireAt(command.get(1), command.get(2), expiresAt);
            log(List.of("SET", command.get(1), command.get(2), "PXAT", String.valueOf(expiresAt)));  // AOF
            return RespWriter.simpleString("OK");
        }
        return RespWriter.error("ERR wrong number of arguments for 'set' command");
    }

    private void log(List<String> command) {
        if (aof != null) aof.append(command);
    }
}