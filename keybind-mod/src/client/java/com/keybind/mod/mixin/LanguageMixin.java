package com.keybind.mod.mixin;

import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public class LanguageMixin {
    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onGetOrDefault(String id, String defaultValue, CallbackInfoReturnable<String> cir) {
        if (id.startsWith("key.keybind.")) {
            String actionName = id.substring("key.keybind.".length());
            // If the key is not in the language file (defaultValue == id), provide a formatted name
            if (defaultValue.equals(id)) {
                cir.setReturnValue(formatActionName(actionName));
            }
        }
    }

    private String formatActionName(String name) {
        if (name == null || name.isEmpty()) return "Unknown";
        
        // Replace underscores/dashes with spaces and capitalize words
        String[] parts = name.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
