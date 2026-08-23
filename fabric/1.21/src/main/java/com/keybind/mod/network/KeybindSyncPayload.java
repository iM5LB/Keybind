package com.keybind.mod.network;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.KeybindSyncData;
import com.keybind.mod.common.network.KeybindPacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record KeybindSyncPayload(String version, List<KeybindActionDefinition> actions) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeybindSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(KeybindMod.SYNC_CHANNEL);

    public static final StreamCodec<FriendlyByteBuf, KeybindSyncPayload> STREAM_CODEC =
            StreamCodec.of(KeybindSyncPayload::write, KeybindSyncPayload::read);

    private static KeybindSyncPayload read(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        try {
            KeybindSyncData data = KeybindPacketCodec.decodeSync(bytes);
            return new KeybindSyncPayload(data.version(), data.actions());
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid Keybind sync payload", exception);
        }
    }

    private static void write(FriendlyByteBuf buf, KeybindSyncPayload payload) {
        try {
            buf.writeBytes(KeybindPacketCodec.encodeSync(new KeybindSyncData(payload.version, payload.actions)));
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid Keybind sync payload", exception);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
