package de.keksuccino.justnicks.nick;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Holds runtime nick mappings and utility helpers for applying them.
 */
public class NickHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<UUID, NickEntry> NICKED = new ConcurrentHashMap<>();
    private static final Map<String, UUID> NAME_TO_UUID = new ConcurrentHashMap<>();

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
        applyNick(player, nickname, null);
    }

    public static void applyNick(@NotNull ServerPlayer player, @NotNull String nickname, @Nullable SignedSkin skin) {
        nickname = nickname.trim();
        if (nickname.isEmpty()) {
            throw new IllegalArgumentException("Nickname must not be empty");
        }

        NickEntry previous = NICKED.get(player.getUUID());
        PropertyMap originalProperties = previous != null ? previous.originalProperties() : copyProperties(player.getGameProfile().properties());

        NickEntry newEntry = new NickEntry(player.getUUID(), player.getGameProfile().name(), nickname, originalProperties, skin);
        NickEntry replaced = NICKED.put(player.getUUID(), newEntry);
        if (replaced != null) {
            NAME_TO_UUID.remove(replaced.nickname().toLowerCase(Locale.ROOT));
        }
        NAME_TO_UUID.put(nickname.toLowerCase(Locale.ROOT), player.getUUID());

        refreshNickForAll(player, replaced != null ? replaced.nickname() : null);
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
        migrateScores(scoreboard, realName, oldNickname, nick);
        if (team != null) {
            if (oldNickname != null && !oldNickname.equals(nick)) {
                playerList.broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, oldNickname, ClientboundSetPlayerTeamPacket.Action.REMOVE));
            }
            playerList.broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, realName, ClientboundSetPlayerTeamPacket.Action.REMOVE));
            playerList.broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, nick, ClientboundSetPlayerTeamPacket.Action.ADD));
        }

        // Move scoreboard entries client-side: remove old owner and re-add with nick, preserving values & formatting.
        ScoreHolder nickHolder = ScoreHolder.forNameOnly(nick);
        scoreboard.listPlayerScores(nickHolder).forEach((objective, value) -> {
            if (oldNickname != null && !oldNickname.equals(nick)) {
                playerList.broadcastAll(new ClientboundResetScorePacket(oldNickname, objective.getName()));
            }
            playerList.broadcastAll(new ClientboundResetScorePacket(realName, objective.getName()));

            var info = scoreboard.getPlayerScoreInfo(nickHolder, objective);
            Optional<Component> display = info instanceof net.minecraft.world.scores.Score score ? Optional.ofNullable(score.display()) : Optional.empty();
            Optional<net.minecraft.network.chat.numbers.NumberFormat> numberFormat =
                    info != null ? Optional.ofNullable(info.numberFormat()) : Optional.empty();

            playerList.broadcastAll(new ClientboundSetScorePacket(nick, objective.getName(), value, display, numberFormat));
        });

        respawnEntityForSelf(player);
        respawnEntityForViewers(player);
    }

    private static void migrateScores(@NotNull Scoreboard scoreboard, @NotNull String realName, @Nullable String oldNickname, @NotNull String nick) {
        moveScores(scoreboard, realName, nick);
        if (oldNickname != null) {
            moveScores(scoreboard, oldNickname, nick);
        }
    }

    private static void moveScores(@NotNull Scoreboard scoreboard, @NotNull String fromName, @NotNull String toName) {
        if (fromName.equals(toName)) {
            return;
        }

        ScoreHolder fromHolder = ScoreHolder.forNameOnly(fromName);
        ScoreHolder toHolder = ScoreHolder.forNameOnly(toName);

        Object2IntMap<Objective> scores = new Object2IntOpenHashMap<>(scoreboard.listPlayerScores(fromHolder));
        scores.forEach((objective, value) -> {
            ScoreAccess newScore = scoreboard.getOrCreatePlayerScore(toHolder, objective, true);
            newScore.set(value);

            ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(fromHolder, objective);
            if (info != null) {
                if (info instanceof net.minecraft.world.scores.Score score) {
                    newScore.display(score.display());
                    newScore.numberFormatOverride(score.numberFormat());
                } else {
                    newScore.numberFormatOverride(info.numberFormat());
                }
                if (info.isLocked()) {
                    newScore.lock();
                } else {
                    newScore.unlock();
                }
            }

            scoreboard.resetSinglePlayerScore(fromHolder, objective);
        });
    }

    private static void respawnEntityForSelf(@NotNull ServerPlayer player) {

        ServerLevel level = player.level();

        // Cache position and body rotation so we can restore them after the dummy menu closes
        final double cachedX = player.getX();
        final double cachedY = player.getY();
        final double cachedZ = player.getZ();
        final float cachedYaw = player.getYRot();
        final float cachedPitch = player.getXRot();

        player.connection.send(new ClientboundRespawnPacket(player.createCommonSpawnInfo(level), ClientboundRespawnPacket.KEEP_ALL_DATA));
        player.openMenu(new SimpleMenuProvider((i, inventory, player1) -> new ChestMenu(MenuType.GENERIC_3x3, 2025, player.getInventory(), new SimpleContainer(0), 0), Component.translatableWithFallback("justnicks.nick.applying_nick", "Applying nickname..")));
        player.level().getServer().execute(() -> {
            player.connection.send(new ClientboundContainerClosePacket(2025));
            player.teleportTo(level, cachedX, cachedY, cachedZ, Set.of(), cachedYaw, cachedPitch, false);
        });

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

    static PropertyMap copyProperties(PropertyMap source) {
        ArrayListMultimap<String, Property> multimap = ArrayListMultimap.create();
        for (String key : source.keySet()) {
            Collection<Property> properties = source.get(key);
            for (Property property : properties) {
                multimap.put(key, new Property(property.name(), property.value(), property.signature()));
            }
        }
        return new PropertyMap(multimap); // PropertyMap copies to immutable internally
    }

    static PropertyMap copyWithSkin(PropertyMap base, SignedSkin skin) {
        ArrayListMultimap<String, Property> multimap = ArrayListMultimap.create();
        for (String key : base.keySet()) {
            if ("textures".equals(key)) continue; // we'll replace below
            Collection<Property> properties = base.get(key);
            for (Property property : properties) {
                multimap.put(key, new Property(property.name(), property.value(), property.signature()));
            }
        }
        multimap.put("textures", skin.asProperty());
        return new PropertyMap(multimap);
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
