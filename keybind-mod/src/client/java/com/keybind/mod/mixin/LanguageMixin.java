package com.keybind.mod.mixin;

import com.keybind.mod.client.KeybindManager;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public class LanguageMixin {
    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onGetOrDefault(String id, CallbackInfoReturnable<String> cir) {
        if (id != null && id.startsWith("key.keybind.")) {
            String actionName = id.substring("key.keybind.".length());
            String displayName = KeybindManager.getDisplayName(actionName);
            if (displayName != null) {
                cir.setReturnValue(displayName);
            }
        }
    }
}
