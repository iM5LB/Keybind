package com.keybind.mod.mixin;

import com.keybind.mod.client.KeybindModClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the per-tick client logic (hello handshake, keybind ticks,
 * disconnect detection). Replaces Fabric API's ClientTickEvents and
 * ClientPlayConnectionEvents.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void keybind$onClientTick(CallbackInfo ci) {
        KeybindModClient.onClientTick((Minecraft) (Object) this);
    }
}
