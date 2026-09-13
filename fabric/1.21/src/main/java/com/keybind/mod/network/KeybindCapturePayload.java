package com.keybind.mod.network;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.network.KeybindPacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeybindCapturePayload(String key) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeybindCapturePayload> TYPE =
            new CustomPacketPayload.Type<>(KeybindMod.CAPTURE_CHANNEL);

    public static final StreamCodec<FriendlyByteBuf, KeybindCapturePayload> STREAM_CODEC =
            StreamCodec.of(KeybindCapturePayload::write, KeybindCapturePayload::read);

    private static KeybindCapturePayload read(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        try {
            return new KeybindCapturePayload(KeybindPacketCodec.decodeAction(bytes));
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid Keybind capture payload", exception);
        }
    }

    private static void write(FriendlyByteBuf buf, KeybindCapturePayload payload) {
        try {
            buf.writeBytes(KeybindPacketCodec.encodeAction(payload.key));
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid Keybind capture payload", exception);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
