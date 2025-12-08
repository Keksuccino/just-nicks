package de.keksuccino.justnicks.mixin.mixins.common.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface AccessorServerGamePacketListenerImpl {

    @Accessor("player") ServerPlayer get_player_JustNicks();

}
