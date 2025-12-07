package de.keksuccino.justnicks.mixin.mixins.common.client;

import de.keksuccino.justnicks.KeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.Category.class)
public class MixinKeyMapping_Category {

    @Shadow @Final private ResourceLocation id;

    @Inject(method = "label", at = @At("HEAD"), cancellable = true)
    private void head_label_JustNicks(CallbackInfoReturnable<Component> info) {
        if (this.id == KeyMappings.JUST_ZOOM_KEYMAPPING_CATEGORY_ID) info.setReturnValue(Component.translatable("justnicks.keybinds.category"));
    }

}
