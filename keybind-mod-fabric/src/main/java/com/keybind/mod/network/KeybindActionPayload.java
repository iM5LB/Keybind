package com.keybind.mod.network;

import com.keybind.mod.KeybindMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record KeybindActionPayload(String action) implements CustomPayload {

    public static final CustomPayload.Id<KeybindActionPayload> ID =
            new CustomPayload.Id<>(KeybindMod.CHANNEL);

    public static final PacketCodec<PacketByteBuf, KeybindActionPayload> CODEC =
            PacketCodec.of(KeybindActionPayload::write, KeybindActionPayload::read);

    private static KeybindActionPayload read(PacketByteBuf buf) {
        return new KeybindActionPayload(buf.readString());
    }

    private static void write(KeybindActionPayload payload, PacketByteBuf buf) {
        buf.writeString(payload.action);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
