package com.keybind.mod.mixin;

import com.keybind.mod.network.KeybindCapturePayload;
import com.keybind.mod.network.KeybindSyncPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.keybind.mod.KeybindMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

/**
 * Appends the Keybind server->client payload types to the vanilla custom
 * payload codec so they decode instead of being discarded. Replaces Fabric
 * API's PayloadTypeRegistry.playS2C().
 */
@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class ClientboundPayloadCodecMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"
            ),
            index = 1
    )
    private static List keybind$addServerboundTypes(List types) {
        List<CustomPacketPayload.TypeAndCodec<?, ?>> list = new ArrayList<>(types);
        KeybindMod.LOGGER.info("[keybind-debug] {} payload codec injection fired, base size={}", "clientbound", list.size());
        list.add(new CustomPacketPayload.TypeAndCodec<>(KeybindSyncPayload.TYPE, KeybindSyncPayload.STREAM_CODEC));
        list.add(new CustomPacketPayload.TypeAndCodec<>(KeybindCapturePayload.TYPE, KeybindCapturePayload.STREAM_CODEC));
        return list;
    }
}
