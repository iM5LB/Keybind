package com.keybind.mod.network;

import com.keybind.mod.common.KeybindConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KeybindHelloPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KeybindHelloPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    KeybindConstants.MOD_ID, KeybindConstants.HELLO_CHANNEL_PATH));

    public static final StreamCodec<FriendlyByteBuf, KeybindHelloPayload> STREAM_CODEC =
            StreamCodec.unit(new KeybindHelloPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
