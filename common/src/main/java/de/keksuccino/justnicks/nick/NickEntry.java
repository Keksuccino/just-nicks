package de.keksuccino.justnicks.nick;

import java.util.UUID;

/**
 * Runtime-only data for a nicked player.
 */
public record NickEntry(UUID uuid, String realName, String nickname) {
}
