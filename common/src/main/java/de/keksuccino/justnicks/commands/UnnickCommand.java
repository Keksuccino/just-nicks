package de.keksuccino.justnicks.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.keksuccino.justnicks.nick.NickHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class UnnickCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unnick")
                .requires(stack -> stack.hasPermission(4))
                .executes(ctx -> clear(ctx.getSource())));
    }

    private static int clear(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = getPlayerOrFail(source);
        if (player == null) {
            return 0;
        }

        boolean removed = NickHandler.removeNick(player);
        if (removed) {
            source.sendSuccess(() -> Component.translatableWithFallback("commands.justnicks.unnick.cleared", "Your real name has been restored."), false);
            return 1;
        }

        source.sendFailure(Component.translatableWithFallback("commands.justnicks.unnick.not_nicked", "You are not nicked."));
        return 0;
    }

    private static ServerPlayer getPlayerOrFail(CommandSourceStack source) throws CommandSyntaxException {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            source.sendFailure(Component.translatable("commands.justnicks.unnick.only_player"));
            return null;
        }
    }
}
