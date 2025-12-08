package de.keksuccino.justnicks.mixin.mixins.common.server;

import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Collection;

@Mixin(ClientboundSetPlayerTeamPacket.class)
public interface AccessorClientboundSetPlayerTeamPacket {

    @Accessor("players") @Mutable void set_players_JustNicks(Collection<String> players);

}
