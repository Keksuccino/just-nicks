package de.keksuccino.justnicks.nick;

import com.mojang.authlib.properties.PropertyMap;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Runtime-only data for a nicked player.
 */
public record NickEntry(UUID uuid, String realName, String nickname, PropertyMap originalProperties, @Nullable SignedSkin appliedSkin) {
}
