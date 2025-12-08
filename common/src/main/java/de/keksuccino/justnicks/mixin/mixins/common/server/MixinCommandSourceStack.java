package de.keksuccino.justnicks.mixin.mixins.common.server;

import de.keksuccino.justnicks.nick.NickHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.stream.Collectors;

@Mixin(CommandSourceStack.class)
public abstract class MixinCommandSourceStack {

    @Shadow public abstract MinecraftServer getServer();

    @Inject(method = "getOnlinePlayerNames", at = @At("HEAD"), cancellable = true)
    private void head_getOnlinePlayerNames_JustNicks(CallbackInfoReturnable<java.util.Collection<String>> info) {
        var list = this.getServer().getPlayerList().getPlayers().stream().map(NickHandler::getDisplayName).collect(Collectors.toList());
        info.setReturnValue(list);
    }

}
