package de.keksuccino.justnicks.mixin.mixins.common.server;

import de.keksuccino.justnicks.nick.NickPacketTransformer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class MixinServerCommonPacketListenerImpl {

    @Final @Shadow protected MinecraftServer server;

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> wrap_sendSingle_JustNicks(Packet<?> packet) {
        return transformIfGameListener_JustNicks(packet);
    }

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> wrap_sendDouble_JustNicks(Packet<?> packet) {
        return transformIfGameListener_JustNicks(packet);
    }

    @Unique
    private Packet<?> transformIfGameListener_JustNicks(Packet<?> packet) {
        if (((Object)this) instanceof ServerGamePacketListenerImpl gameListener) {
            ServerPlayer player = ((AccessorServerGamePacketListenerImpl) gameListener).getPlayer_JustNicks();
            return NickPacketTransformer.transform(packet, player, this.server);
        }
        return packet;
    }

}
