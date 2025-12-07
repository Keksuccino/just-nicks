package de.keksuccino.justnicks;

import de.keksuccino.justnicks.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class JustNicksNeoForgeServerEvents {

    public static void registerAll() {

        NeoForge.EVENT_BUS.register(new JustNicksNeoForgeServerEvents());

    }

    @SubscribeEvent
    public void onRegisterServerCommands(RegisterCommandsEvent e) {
        Commands.registerAll(e.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {

        }
    }

}
