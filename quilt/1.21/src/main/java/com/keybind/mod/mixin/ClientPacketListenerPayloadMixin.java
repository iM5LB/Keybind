package com.keybind.mod.mixin;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.client.KeybindModClient;
import com.keybind.mod.network.KeybindCapturePayload;
import com.keybind.mod.network.KeybindSyncPayload;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes decoded Keybind payloads to the client handler. Replaces Fabric
 * API's ClientPlayNetworking.registerGlobalReceiver.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerPayloadMixin {

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void keybind$onCustomPayload(CustomPacketPayload payload, CallbackInfo ci) {
        KeybindMod.LOGGER.info("[keybind-debug] custom payload received: {} ({})", payload.type().id(), payload.getClass().getSimpleName());
        if (payload instanceof KeybindSyncPayload sync
                && KeybindSyncPayload.TYPE.id().equals(sync.type().id())) {
            ci.cancel();
            KeybindModClient.onSyncPayload(sync);
        } else if (payload instanceof KeybindCapturePayload capture
                && KeybindCapturePayload.TYPE.id().equals(capture.type().id())) {
            ci.cancel();
            KeybindModClient.onCapturePayload(capture);
        }
    }
}
