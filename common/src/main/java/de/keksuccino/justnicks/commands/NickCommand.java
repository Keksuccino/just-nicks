package de.keksuccino.justnicks.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.keksuccino.justnicks.nick.NickHandler;
import de.keksuccino.justnicks.nick.Nicknames;
import de.keksuccino.justnicks.nick.SignedSkin;
import de.keksuccino.justnicks.nick.SkinFetcher;
import de.keksuccino.justnicks.nick.Skins;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class NickCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nick")
                .requires(stack -> stack.hasPermission(4))
                .executes(ctx -> applyRandom(ctx.getSource()))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> applyCustom(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
    }

    private static int applyRandom(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = getPlayerOrFail(source);
        if (player == null) return 0;
        String nickname = Nicknames.randomNickname();
        SignedSkin skin = Skins.randomSkin();
        NickHandler.applyNick(player, nickname, skin);
        // Only tell the executing player; don't broadcast to others (vanilla clients would see the raw key).
        source.sendSuccess(() -> Component.translatableWithFallback("commands.justnicks.nick.applied_random", "Your nickname is now %s.", nickname), false);
        return 1;
    }

    private static int applyCustom(CommandSourceStack source, String nickname) throws CommandSyntaxException {
        ServerPlayer player = getPlayerOrFail(source);
        if (player == null) return 0;
        SignedSkin skin = SkinFetcher.fetchByUsername(nickname).orElse(null);
        NickHandler.applyNick(player, nickname, skin);
        source.sendSuccess(() -> Component.translatableWithFallback("commands.justnicks.nick.applied_custom", "Your nickname is now %s.", nickname), false);
        return 1;
    }

    private static ServerPlayer getPlayerOrFail(CommandSourceStack source) throws CommandSyntaxException {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            source.sendFailure(Component.translatable("commands.justnicks.nick.only_player"));
            return null;
        }
    }
}
