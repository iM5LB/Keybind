package com.keybind.mod.common.network;

import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.KeybindSyncData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class KeybindPacketCodec {

    private KeybindPacketCodec() {
    }

    public static byte[] encodeAction(String action) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            writeString(dos, action);
            return baos.toByteArray();
        }
    }

    public static String decodeAction(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            return readString(in);
        }
    }

    public static byte[] encodeSync(KeybindSyncData syncData) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            writeString(dos, syncData.version());
            writeVarInt(dos, syncData.actions().size());
            for (KeybindActionDefinition action : syncData.actions()) {
                writeString(dos, action.name());
                writeString(dos, action.displayName());
                writeString(dos, action.defaultKey());
            }
            return baos.toByteArray();
        }
    }

    public static KeybindSyncData decodeSync(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String version = readString(in);
            int count = readVarInt(in);
            List<KeybindActionDefinition> actions = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                actions.add(new KeybindActionDefinition(
                        readString(in),
                        readString(in),
                        readString(in)
                ));
            }
            return new KeybindSyncData(version, actions);
        }
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;
        do {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;
            position += 7;
            if (position > 35) {
                throw new IOException("VarInt is too large");
            }
        } while ((currentByte & 0x80) != 0);
        return value;
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        if (length <= 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
}
