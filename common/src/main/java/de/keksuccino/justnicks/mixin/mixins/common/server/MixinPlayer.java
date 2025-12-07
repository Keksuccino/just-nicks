package de.keksuccino.justnicks.mixin.mixins.common.server;

import de.keksuccino.justnicks.nick.NickHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void head_getDisplayName_JustNicks(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer serverPlayer)) return;
        String nickname = NickHandler.getNickname(serverPlayer);
        if (nickname != null) {
            Scoreboard scoreboard = serverPlayer.level().getScoreboard();
            PlayerTeam team = scoreboard.getPlayersTeam(serverPlayer.getScoreboardName());
            Component base = Component.literal(nickname);
            cir.setReturnValue(PlayerTeam.formatNameForTeam(team, base));
        }
    }

}
