package com.keybind.mod.network;

import com.keybind.mod.KeybindMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record KeybindSyncPayload(List<ActionEntry> actions) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeybindSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(KeybindMod.SYNC_CHANNEL);

    public static final StreamCodec<FriendlyByteBuf, KeybindSyncPayload> STREAM_CODEC =
            StreamCodec.of(KeybindSyncPayload::write, KeybindSyncPayload::read);

    private static KeybindSyncPayload read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ActionEntry> actions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            String defaultKey = buf.readUtf();
            actions.add(new ActionEntry(name, defaultKey));
        }
        return new KeybindSyncPayload(actions);
    }

    private static void write(FriendlyByteBuf buf, KeybindSyncPayload payload) {
        buf.writeVarInt(payload.actions.size());
        for (ActionEntry entry : payload.actions) {
            buf.writeUtf(entry.name);
            buf.writeUtf(entry.defaultKey);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ActionEntry(String name, String defaultKey) {}
}
