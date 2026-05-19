package com.keybind.mod.network;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.KeybindActionDefinition;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record KeybindSyncPayload(String version, List<KeybindActionDefinition> actions) implements CustomPayload {

    public static final CustomPayload.Id<KeybindSyncPayload> ID =
            new CustomPayload.Id<>(KeybindMod.SYNC_CHANNEL);

    public static final PacketCodec<PacketByteBuf, KeybindSyncPayload> CODEC =
            PacketCodec.of(KeybindSyncPayload::write, KeybindSyncPayload::read);

    private static KeybindSyncPayload read(PacketByteBuf buf) {
        String version = buf.readString();
        int count = buf.readVarInt();
        List<KeybindActionDefinition> actions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readString();
            String displayName = buf.readString();
            String defaultKey = buf.readString();
            actions.add(new KeybindActionDefinition(name, displayName, defaultKey));
        }
        return new KeybindSyncPayload(version, actions);
    }

    private static void write(KeybindSyncPayload payload, PacketByteBuf buf) {
        buf.writeString(payload.version);
        buf.writeVarInt(payload.actions.size());
        for (KeybindActionDefinition entry : payload.actions) {
            buf.writeString(entry.name());
            buf.writeString(entry.displayName());
            buf.writeString(entry.defaultKey());
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
