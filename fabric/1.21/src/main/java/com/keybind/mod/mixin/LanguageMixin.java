package com.keybind.mod.mixin;

import com.keybind.mod.client.KeybindManager;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public class LanguageMixin {
    private static final String CATEGORY_KEY_NEW = "key.category.keybind.actions";
    private static final String CATEGORY_KEY_OLD = "key.categories.keybind.actions";

    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onGetOrDefault(String id, CallbackInfoReturnable<String> cir) {
        String custom = resolveCustom(id);
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }

    private static String resolveCustom(String id) {
        if (id == null) {
            return null;
        }

        if (CATEGORY_KEY_NEW.equals(id) || CATEGORY_KEY_OLD.equals(id)) {
            return "Keybind Actions";
        }

        if (id.startsWith("key.keybind.")) {
            String actionName = id.substring("key.keybind.".length());
            String displayName = KeybindManager.getDisplayName(actionName);
            if (displayName != null) {
                return displayName;
            }
            return KeybindManager.formatActionName(actionName);
        }
        return null;
    }
}
