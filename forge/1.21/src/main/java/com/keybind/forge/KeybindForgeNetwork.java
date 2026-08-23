package com.keybind.forge;

import com.keybind.mod.common.KeybindSyncData;
import com.keybind.mod.common.network.KeybindPacketCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.util.function.Consumer;

public final class KeybindForgeNetwork {
    private static final EventNetworkChannel ACTION = ChannelBuilder.named(Identifier.fromNamespaceAndPath("keybind", "main")).optional().eventNetworkChannel();
    private static final EventNetworkChannel SYNC = ChannelBuilder.named(Identifier.fromNamespaceAndPath("keybind", "sync")).optional().eventNetworkChannel();
    private static Consumer<KeybindSyncData> syncReceiver = ignored -> { };

    private KeybindForgeNetwork() { }

    public static void initialize() {
        SYNC.addListener(KeybindForgeNetwork::handleSync);
    }

    public static void setSyncReceiver(Consumer<KeybindSyncData> receiver) {
        syncReceiver = receiver;
    }

    public static void sendAction(String action) {
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeBytes(KeybindPacketCodec.encodeAction(action));
            ACTION.send(buffer, PacketDistributor.SERVER.noArg());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to encode Keybind action", exception);
        }
    }

    private static void handleSync(CustomPayloadEvent event) {
        if (!event.getSource().isClientSide()) return;
        FriendlyByteBuf buffer = event.getPayload();
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        try {
            KeybindSyncData data = KeybindPacketCodec.decodeSync(bytes);
            event.getSource().enqueueWork(() -> syncReceiver.accept(data));
            event.getSource().setPacketHandled(true);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid Keybind sync payload", exception);
        }
    }
}
