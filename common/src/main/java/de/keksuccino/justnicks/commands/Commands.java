package de.keksuccino.justnicks.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

public class Commands {

    public static void registerAll(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {

        NickCommand.register(dispatcher);
        UnnickCommand.register(dispatcher);
        OptionsCommand.register(dispatcher);

    }

}
