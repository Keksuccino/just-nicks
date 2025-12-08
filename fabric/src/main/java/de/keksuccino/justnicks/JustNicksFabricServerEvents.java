package de.keksuccino.justnicks;

import de.keksuccino.justnicks.commands.Commands;
import de.keksuccino.justnicks.nick.NickHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class JustNicksFabricServerEvents {

    public static void registerAll() {

        registerServerCommands();

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> NickHandler.clear(handler.getPlayer()));

    }

    private static void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> Commands.registerAll(dispatcher));
    }

}
