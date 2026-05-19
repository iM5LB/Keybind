package com.keybind.mod.network;

import com.keybind.mod.KeybindMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeybindActionPayload(String action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeybindActionPayload> TYPE =
            new CustomPacketPayload.Type<>(KeybindMod.CHANNEL);

    public static final StreamCodec<FriendlyByteBuf, KeybindActionPayload> STREAM_CODEC =
            StreamCodec.of(KeybindActionPayload::write, KeybindActionPayload::read);

    private static KeybindActionPayload read(FriendlyByteBuf buf) {
        return new KeybindActionPayload(buf.readUtf());
    }

    private static void write(FriendlyByteBuf buf, KeybindActionPayload payload) {
        buf.writeUtf(payload.action);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
