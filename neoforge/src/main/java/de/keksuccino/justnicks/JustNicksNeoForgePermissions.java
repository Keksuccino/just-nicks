package de.keksuccino.justnicks;

import de.keksuccino.justnicks.util.permission.Permission;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JustNicksNeoForgePermissions {

    public static final PermissionNode<Boolean> NICK = new PermissionNode<>(Permission.NICK.getAsLocation(), PermissionTypes.BOOLEAN, (player, playerUUID, context) -> false);
    public static final PermissionNode<Boolean> UNNICK = new PermissionNode<>(Permission.UNNICK.getAsLocation(), PermissionTypes.BOOLEAN, (player, playerUUID, context) -> false);

    public static void registerAll(PermissionGatherEvent.Nodes e) {
        e.addNodes(NICK);
        e.addNodes(UNNICK);
    }

    @Nullable
    public static PermissionNode<Boolean> resolveNode(@NotNull Permission permission) {
        if (permission == Permission.NICK) return NICK;
        if (permission == Permission.UNNICK) return UNNICK;
        return null;
    }

}
