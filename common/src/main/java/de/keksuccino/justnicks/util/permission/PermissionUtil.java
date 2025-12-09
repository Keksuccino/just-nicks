package de.keksuccino.justnicks.util.permission;

import de.keksuccino.justnicks.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class PermissionUtil {

    public static boolean hasPermission(@NotNull CommandSourceStack stack, @NotNull Permission permission) {
        try {
            ServerPlayer player = stack.getPlayerOrException();
            return (stack.hasPermission(4) || Services.PLATFORM.hasPermission(player, permission));
        } catch (Exception ignore) {}
        return false;
    }

}
