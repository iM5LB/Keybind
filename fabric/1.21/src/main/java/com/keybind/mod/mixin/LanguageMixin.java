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

    @Inject(method = "get(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetOld(String id, CallbackInfoReturnable<String> cir) {
        handle(id, cir);
    }

    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetNew(String id, CallbackInfoReturnable<String> cir) {
        handle(id, cir);
    }

    private void handle(String id, CallbackInfoReturnable<String> cir) {
        if (id == null) {
            return;
        }

        if (CATEGORY_KEY_NEW.equals(id) || CATEGORY_KEY_OLD.equals(id)) {
            cir.setReturnValue("Keybind Actions");
            return;
        }

        if (id.startsWith("key.keybind.")) {
            String actionName = id.substring("key.keybind.".length());
            String displayName = KeybindManager.getDisplayName(actionName);
            if (displayName != null) {
                cir.setReturnValue(displayName);
                return;
            }
            cir.setReturnValue(KeybindManager.formatActionName(actionName));
        }
    }
}
