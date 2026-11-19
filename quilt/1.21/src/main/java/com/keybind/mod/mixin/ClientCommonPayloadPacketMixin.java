package com.keybind.mod.mixin;

import com.keybind.mod.KeybindMod;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs every incoming custom payload packet before the DiscardedPayload
 * early-return, to diagnose whether packets arrive and how they decode.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPayloadPacketMixin {

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void keybind$logPayloadPacket(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        KeybindMod.LOGGER.info("[keybind-debug] payload packet: {} -> {}", packet.payload().type().id(), packet.payload().getClass().getSimpleName());
    }
}
