package de.keksuccino.justnicks.mixin.mixins.common.server;

import de.keksuccino.justnicks.nick.NickHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Shadow public abstract MinecraftServer getServer();

    @Inject(method = "getPlayerByName", at = @At("HEAD"), cancellable = true)
    private void head_getPlayerByName_JustNicks(String name, CallbackInfoReturnable<ServerPlayer> info) {
        ServerPlayer byNick = NickHandler.findByNickname(this.getServer(), name);
        if (byNick != null) {
            info.setReturnValue(byNick);
        }
    }

}
