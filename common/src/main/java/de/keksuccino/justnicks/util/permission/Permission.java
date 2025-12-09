package de.keksuccino.justnicks.util.permission;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public enum Permission {

    NICK("permission.nick"),
    UNNICK("permission.unnick");

    private static final String PREFIX = "justnicks";

    @NotNull
    private final String permission;
    @NotNull
    private final ResourceLocation location;

    private Permission(@NotNull String permission) {
        this.permission = PREFIX + "." + permission;
        this.location = ResourceLocation.fromNamespaceAndPath(PREFIX, permission);
    }

    @NotNull
    public String getPermission() {
        return permission;
    }

    @NotNull
    public ResourceLocation getAsLocation() {
        return this.location;
    }

}
