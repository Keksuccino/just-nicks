package de.keksuccino.justnicks;

import de.keksuccino.justnicks.commands.Commands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class JustNicksFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

        // Handle join server stuff on server-side
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
//            PacketHandler.sendHandshakeToClient(handler.getPlayer());
        });

    }

    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> Commands.registerAll(dispatcher));
    }

}
