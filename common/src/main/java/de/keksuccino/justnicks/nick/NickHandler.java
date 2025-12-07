package de.keksuccino.justnicks.nick;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Holds runtime nick mappings and utility helpers for applying them.
 */
public final class NickHandler {

    private static final Map<UUID, NickEntry> NICKED = new ConcurrentHashMap<>();
    private static final Map<String, UUID> NAME_TO_UUID = new ConcurrentHashMap<>();

    private NickHandler() {
    }

    public static boolean isNicked(@NotNull ServerPlayer player) {
        return NICKED.containsKey(player.getUUID());
    }

    @Nullable
    public static String getNickname(@NotNull ServerPlayer player) {
        NickEntry entry = NICKED.get(player.getUUID());
        return entry == null ? null : entry.nickname();
    }

    public static Optional<NickEntry> getEntry(UUID uuid) {
        return Optional.ofNullable(NICKED.get(uuid));
    }

    public static void clear(@NotNull ServerPlayer player) {
        NickEntry removed = NICKED.remove(player.getUUID());
        if (removed != null) {
            NAME_TO_UUID.remove(removed.nickname().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Removes the nickname and re-syncs the player using their real identity.
     *
     * @return true if a nick was cleared, false if the player was already using their real name.
     */
    public static boolean removeNick(@NotNull ServerPlayer player) {
        NickEntry removed = NICKED.remove(player.getUUID());
        if (removed == null) {
            return false;
        }
        NAME_TO_UUID.remove(removed.nickname().toLowerCase(Locale.ROOT));
        refreshNickForAll(player, removed.nickname());
        return true;
    }

    public static void applyNick(@NotNull ServerPlayer player, @NotNull String nickname) {
        nickname = nickname.trim();
        if (nickname.isEmpty()) {
            throw new IllegalArgumentException("Nickname must not be empty");
        }

        NickEntry previous = NICKED.put(player.getUUID(), new NickEntry(player.getUUID(), player.getGameProfile().name(), nickname));
        if (previous != null) {
            NAME_TO_UUID.remove(previous.nickname().toLowerCase(Locale.ROOT));
        }
        NAME_TO_UUID.put(nickname.toLowerCase(Locale.ROOT), player.getUUID());

        refreshNickForAll(player, previous != null ? previous.nickname() : null);
    }

    @Nullable
    public static ServerPlayer findByNickname(@NotNull MinecraftServer server, @NotNull String nickname) {
        UUID uuid = NAME_TO_UUID.get(nickname.toLowerCase(Locale.ROOT));
        if (uuid == null) return null;
        return server.getPlayerList().getPlayer(uuid);
    }

    /**
     * For packets & chat components: return nicked display name if present, otherwise the real profile name.
     */
    public static String getDisplayName(@NotNull ServerPlayer player) {
        String nick = getNickname(player);
        return nick == null ? player.getGameProfile().name() : nick;
    }

    public static Component getDecoratedDisplayName(@NotNull ServerPlayer player) {
        String nick = getDisplayName(player);
        Scoreboard scoreboard = player.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(player.getScoreboardName());
        return PlayerTeam.formatNameForTeam(team, Component.literal(nick));
    }

    /**
     * Broadcast packets so every client knows the nicked name and receives updated scoreboard entries.
     */
    public static void refreshNickForAll(@NotNull ServerPlayer player, @Nullable String oldNickname) {
        MinecraftServer server = player.level().getServer();
        PlayerList playerList = server.getPlayerList();

        // Re-send player info (remove then add with new profile name)
        playerList.broadcastAll(new ClientboundPlayerInfoRemovePacket(ImmutableList.of(player.getUUID())));
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
        ), ImmutableList.of(player)));

        // Sync team membership to clients (remove old name, add new nick)
        Scoreboard scoreboard = player.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(player.getScoreboardName());
        String realName = player.getScoreboardName();
        String nick = getDisplayName(player);
        if (team != null) {
            if (oldNickname != null && !oldNickname.equals(nick)) {
                playerList.broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, oldNickname, ClientboundSetPlayerTeamPacket.Action.REMOVE));
            }
            playerList.broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, realName, ClientboundSetPlayerTeamPacket.Action.REMOVE));
            playerList.broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, nick, ClientboundSetPlayerTeamPacket.Action.ADD));
        }

        // Move scoreboard entries client-side: remove old owner and re-add with nick, preserving values & formatting.
        ScoreHolder holder = ScoreHolder.forNameOnly(realName);
        scoreboard.listPlayerScores(holder).forEach((objective, value) -> {
            if (oldNickname != null && !oldNickname.equals(nick)) {
                playerList.broadcastAll(new ClientboundResetScorePacket(oldNickname, objective.getName()));
            }
            // remove old entry from clients
            playerList.broadcastAll(new ClientboundResetScorePacket(realName, objective.getName()));

            var info = scoreboard.getPlayerScoreInfo(holder, objective);
            Optional<Component> display = info instanceof net.minecraft.world.scores.Score score ? Optional.ofNullable(score.display()) : Optional.empty();
            Optional<net.minecraft.network.chat.numbers.NumberFormat> numberFormat =
                    info != null ? Optional.ofNullable(info.numberFormat()) : Optional.empty();

            playerList.broadcastAll(new ClientboundSetScorePacket(nick, objective.getName(), value, display, numberFormat));
        });

        respawnEntityForViewers(player);
    }

    private static void respawnEntityForViewers(@NotNull ServerPlayer player) {
        ServerLevel level = player.level();
        List<ServerPlayer> viewers = level.getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false);
        if (viewers.isEmpty()) {
            return;
        }

        ServerEntity serverEntity = new ServerEntity(level, player, player.getType().updateInterval(), player.getType().trackDeltas(), NOOP_SYNCHRONIZER);
        ClientboundRemoveEntitiesPacket removePacket = new ClientboundRemoveEntitiesPacket(player.getId());

        for (ServerPlayer viewer : viewers) {
            if (viewer == player) {
                continue; // Avoid replacing the client's local player instance.
            }
            viewer.connection.send(removePacket);
            List<Packet<? super ClientGamePacketListener>> bundle = new ArrayList<>();
            serverEntity.sendPairingData(viewer, bundle::add);
            viewer.connection.send(new ClientboundBundlePacket(bundle));
        }
    }

    private static final ServerEntity.Synchronizer NOOP_SYNCHRONIZER = new ServerEntity.Synchronizer() {
        @Override
        public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
        }

        @Override
        public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
        }

        @Override
        public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate) {
        }
    };
}
