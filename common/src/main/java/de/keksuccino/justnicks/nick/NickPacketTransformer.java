package de.keksuccino.justnicks.nick;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import de.keksuccino.justnicks.JustNicks;
import de.keksuccino.justnicks.mixin.mixins.common.server.AccessorClientboundPlayerInfoUpdatePacketEntry;
import de.keksuccino.justnicks.mixin.mixins.common.server.AccessorClientboundSetPlayerTeamPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rewrites outbound packets so viewers see nicknames instead of real names.
 */
public class NickPacketTransformer {

    public static Packet<?> transform(Packet<?> packet, ServerPlayer viewer, MinecraftServer server) {
        NickEntry selfEntry = null;
        boolean keepSelfOriginal = false;
        if (JustNicks.getOptions().showOriginalIdentityToSelfPlayer.getValue()) {
            selfEntry = NickHandler.getEntry(viewer.getUUID()).orElse(null);
            keepSelfOriginal = selfEntry != null;
        }
        return transformInternal(packet, viewer, server, selfEntry, keepSelfOriginal);
    }

    private static Packet<?> transformInternal(Packet<?> packet, ServerPlayer viewer, MinecraftServer server, @Nullable NickEntry selfEntry, boolean keepSelfOriginal) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            List<Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>> rewritten = new ArrayList<>();
            for (Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener> sub : bundle.subPackets()) {
                Packet<?> repl = transformInternal((Packet<?>) sub, viewer, server, selfEntry, keepSelfOriginal);
                //noinspection unchecked
                rewritten.add((Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>) repl);
            }
            return new ClientboundBundlePacket(rewritten);
        }

        if (packet instanceof ClientboundPlayerInfoUpdatePacket info) {
            rewritePlayerInfo(info, server, selfEntry, keepSelfOriginal);
            return packet;
        }

        if (packet instanceof ClientboundSetScorePacket scorePacket) {
            String owner = scorePacket.owner();
            String replaced = replaceName(owner, server, selfEntry, keepSelfOriginal);
            if (!owner.equals(replaced)) {
                return new ClientboundSetScorePacket(
                        replaced,
                        scorePacket.objectiveName(),
                        scorePacket.score(),
                        scorePacket.display(),
                        scorePacket.numberFormat()
                );
            }
            return packet;
        }

        if (packet instanceof ClientboundResetScorePacket resetScorePacket) {
            String owner = resetScorePacket.owner();
            String replaced = replaceName(owner, server, selfEntry, keepSelfOriginal);
            if (!owner.equals(replaced)) {
                return new ClientboundResetScorePacket(replaced, resetScorePacket.objectiveName());
            }
            return packet;
        }

        if (packet instanceof ClientboundSetPlayerTeamPacket teamPacket) {
            rewriteTeamPlayers(teamPacket, server, selfEntry, keepSelfOriginal);
            return packet;
        }

        if (packet instanceof ClientboundCommandSuggestionsPacket suggestionsPacket) {
            Suggestions suggestions = suggestionsPacket.toSuggestions();
            List<Suggestion> list = suggestions.getList();
            boolean changed = false;
            List<Suggestion> replaced = new ArrayList<>(list.size());
            for (Suggestion s : list) {
                String newText = replaceName(s.getText(), server, selfEntry, keepSelfOriginal);
                if (!newText.equals(s.getText())) {
                    changed = true;
                    replaced.add(new Suggestion(s.getRange(), newText, s.getTooltip()));
                } else {
                    replaced.add(s);
                }
            }
            if (changed) {
                Suggestions newSuggestions = new Suggestions(suggestions.getRange(), replaced);
                return new ClientboundCommandSuggestionsPacket(suggestionsPacket.id(), newSuggestions);
            }
            return packet;
        }

        return packet;
    }

    private static void rewritePlayerInfo(ClientboundPlayerInfoUpdatePacket info, MinecraftServer server, @Nullable NickEntry selfEntry, boolean keepSelfOriginal) {
        PlayerList list = server.getPlayerList();
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : info.entries()) {
            ServerPlayer target = list.getPlayer(entry.profileId());
            if (target == null) continue;
            NickEntry nickEntry = NickHandler.getEntry(target.getUUID()).orElse(null);
            if (nickEntry == null) continue;
            if (keepSelfOriginal && selfEntry != null && target.getUUID().equals(selfEntry.uuid())) {
                continue; // Keep real profile/skin for the nicked player's own client.
            }

            GameProfile original = entry.profile();
            if (original == null) continue;
            PropertyMap properties = NickHandler.copyProperties(original.properties());
            if (nickEntry.appliedSkin() != null) {
                properties = NickHandler.copyWithSkin(properties, nickEntry.appliedSkin());
            }
            GameProfile newProfile = new GameProfile(original.id(), nickEntry.nickname(), properties);

            Component display = NickHandler.getDecoratedDisplayName(target);

            AccessorClientboundPlayerInfoUpdatePacketEntry accessor = (AccessorClientboundPlayerInfoUpdatePacketEntry) (Object) entry;
            accessor.setProfile(newProfile);
            accessor.setDisplayName(display);
        }
    }

    private static void rewriteTeamPlayers(ClientboundSetPlayerTeamPacket packet, MinecraftServer server, @Nullable NickEntry selfEntry, boolean keepSelfOriginal) {
        AccessorClientboundSetPlayerTeamPacket accessor = (AccessorClientboundSetPlayerTeamPacket) packet;
        Collection<String> current = packet.getPlayers();
        if (current.isEmpty()) return;

        List<String> rewritten = current.stream().map(name -> replaceName(name, server, selfEntry, keepSelfOriginal)).collect(Collectors.toList());
        if (!rewritten.equals(new ArrayList<>(current))) {
            accessor.setPlayers(ImmutableList.copyOf(rewritten));
        }
    }

    @NotNull
    private static String replaceName(@NotNull String input, @NotNull MinecraftServer server, @Nullable NickEntry selfEntry, boolean keepSelfOriginal) {
        ServerPlayer byRealName = server.getPlayerList().getPlayerByName(input);
        if (byRealName != null) {
            if (keepSelfOriginal && selfEntry != null && byRealName.getUUID().equals(selfEntry.uuid())) {
                return input; // Preserve own real name.
            }
            String nick = NickHandler.getNickname(byRealName);
            if (nick != null) return nick;
        }
        ServerPlayer byNick = NickHandler.findByNickname(server, input);
        if (byNick != null) {
            if (keepSelfOriginal && selfEntry != null && byNick.getUUID().equals(selfEntry.uuid())) {
                return selfEntry.realName(); // Show original identity to the nicked player.
            }
            String nick = NickHandler.getNickname(byNick);
            if (nick != null) return nick;
        }
        return input;
    }

}
