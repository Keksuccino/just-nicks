package de.keksuccino.justnicks.mixin.mixins.neoforge.client;

import de.keksuccino.justnicks.JustNicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_construct_JustNicks(GameConfig gameConfig, CallbackInfo info) {

        JustNicks.init();

    }

}
