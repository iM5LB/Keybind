package com.keybind.mod;

import com.keybind.mod.common.KeybindConstants;
import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindCapturePayload;
import com.keybind.mod.network.KeybindSyncPayload;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KeybindMod {
    public static final String MOD_ID = KeybindConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, KeybindConstants.ACTION_CHANNEL_PATH);
    public static final ResourceLocation SYNC_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, KeybindConstants.SYNC_CHANNEL_PATH);
    public static final ResourceLocation CAPTURE_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, KeybindConstants.CAPTURE_CHANNEL_PATH);
    private static Consumer<KeybindSyncPayload> syncReceiver = payload -> { };
    private static Runnable captureReceiver = () -> { };

    private KeybindMod() { }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Optional payloads allow connecting to Paper/vanilla servers that use plugin messaging.
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(KeybindActionPayload.TYPE, KeybindActionPayload.STREAM_CODEC, (payload, context) -> { });
        registrar.playToClient(KeybindSyncPayload.TYPE, KeybindSyncPayload.STREAM_CODEC, (payload, context) -> syncReceiver.accept(payload));
        registrar.playToClient(KeybindCapturePayload.TYPE, KeybindCapturePayload.STREAM_CODEC, (payload, context) -> captureReceiver.run());
    }

    public static void setSyncReceiver(Consumer<KeybindSyncPayload> receiver) {
        syncReceiver = receiver;
    }

    public static void setCaptureReceiver(Runnable receiver) {
        captureReceiver = receiver == null ? () -> { } : receiver;
    }
}
