package com.keybind.mod.mixin;

import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindCapturePayload;
import com.keybind.mod.network.KeybindHelloPayload;
import com.keybind.mod.network.RegisterChannelsPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.keybind.mod.KeybindMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

/**
 * Appends the Keybind client->server payload types to the vanilla custom
 * payload codec so they serialize. Replaces Fabric API's
 * PayloadTypeRegistry.playC2S().
 */
@Mixin(ServerboundCustomPayloadPacket.class)
public abstract class ServerboundPayloadCodecMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"
            ),
            index = 1
    )
    private static List keybind$addClientboundTypes(List types) {
        List<CustomPacketPayload.TypeAndCodec<?, ?>> list = new ArrayList<>(types);
        KeybindMod.LOGGER.info("[keybind-debug] {} payload codec injection fired, base size={}", "serverbound", list.size());
        list.add(new CustomPacketPayload.TypeAndCodec<>(KeybindActionPayload.TYPE, KeybindActionPayload.STREAM_CODEC));
        list.add(new CustomPacketPayload.TypeAndCodec<>(KeybindCapturePayload.TYPE, KeybindCapturePayload.STREAM_CODEC));
        list.add(new CustomPacketPayload.TypeAndCodec<>(KeybindHelloPayload.TYPE, KeybindHelloPayload.STREAM_CODEC));
        list.add(new CustomPacketPayload.TypeAndCodec<>(RegisterChannelsPayload.TYPE, RegisterChannelsPayload.STREAM_CODEC));
        return list;
    }
}
