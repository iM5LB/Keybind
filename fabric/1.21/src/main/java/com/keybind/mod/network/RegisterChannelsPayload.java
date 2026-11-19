package com.keybind.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/**
 * Vanilla {@code minecraft:register} plugin-channel announcement. Some server
 * software (Paper) only forwards plugin messages on channels the client has
 * registered, so clients that bypass loader networking must send this
 * themselves. Payload body is the channel names joined by NUL bytes.
 */
public record RegisterChannelsPayload(String channels) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RegisterChannelsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("minecraft", "register"));

    public static final StreamCodec<FriendlyByteBuf, RegisterChannelsPayload> STREAM_CODEC =
            StreamCodec.of(RegisterChannelsPayload::write, RegisterChannelsPayload::read);

    private static RegisterChannelsPayload read(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new RegisterChannelsPayload(new String(bytes, StandardCharsets.UTF_8));
    }

    private static void write(FriendlyByteBuf buf, RegisterChannelsPayload payload) {
        buf.writeBytes(payload.channels.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
