package de.keksuccino.justnicks.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
        var base = Commands.literal("nick")
                .requires(stack -> stack.hasPermission(4))
                .executes(ctx -> applyRandom(ctx.getSource(), true))
                .then(Commands.argument("refresh_self", BoolArgumentType.bool())
                        .executes(ctx -> applyRandom(ctx.getSource(), BoolArgumentType.getBool(ctx, "refresh_self"))));

        var custom = Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> applyCustom(ctx.getSource(), StringArgumentType.getString(ctx, "name"), true))
                .then(Commands.argument("refresh_self", BoolArgumentType.bool())
                        .executes(ctx -> applyCustom(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "refresh_self"))));

        dispatcher.register(base.then(custom));
    }

    private static int applyRandom(CommandSourceStack source, boolean refreshSelf) throws CommandSyntaxException {
        ServerPlayer player = getPlayerOrFail(source);
        if (player == null) return 0;
        String nickname = Nicknames.randomNickname();
        SignedSkin skin = Skins.randomSkin();
        NickHandler.applyNick(player, nickname, skin, refreshSelf);
        // Only tell the executing player; don't broadcast to others (vanilla clients would see the raw key).
        source.sendSuccess(() -> Component.translatableWithFallback("commands.justnicks.nick.applied_random", "Your nickname is now %s.", nickname), false);
        return 1;
    }

    private static int applyCustom(CommandSourceStack source, String nickname, boolean refreshSelf) throws CommandSyntaxException {
        ServerPlayer player = getPlayerOrFail(source);
        if (player == null) return 0;
        SignedSkin skin = SkinFetcher.fetchByUsername(nickname).orElse(null);
        NickHandler.applyNick(player, nickname, skin, refreshSelf);
        source.sendSuccess(() -> Component.translatableWithFallback("commands.justnicks.nick.applied_custom", "Your nickname is now %s.", nickname), false);
        return 1;
    }

    private static ServerPlayer getPlayerOrFail(CommandSourceStack source) throws CommandSyntaxException {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            source.sendFailure(Component.translatableWithFallback("commands.justnicks.nick.only_player", "Only players can use this command."));
            return null;
        }
    }
}
