package com.keybind.mod.network;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.network.KeybindPacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeybindActionPayload(String action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeybindActionPayload> TYPE =
            new CustomPacketPayload.Type<>(KeybindMod.CHANNEL);

    public static final StreamCodec<FriendlyByteBuf, KeybindActionPayload> STREAM_CODEC =
            StreamCodec.of(KeybindActionPayload::write, KeybindActionPayload::read);

    private static KeybindActionPayload read(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        try {
            return new KeybindActionPayload(KeybindPacketCodec.decodeAction(bytes));
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid Keybind action payload", exception);
        }
    }

    private static void write(FriendlyByteBuf buf, KeybindActionPayload payload) {
        try {
            buf.writeBytes(KeybindPacketCodec.encodeAction(payload.action));
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid Keybind action payload", exception);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
