package de.keksuccino.justnicks.mixin.mixins.common.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundPlayerInfoUpdatePacket.Entry.class)
public interface AccessorClientboundPlayerInfoUpdatePacketEntry {

    @Accessor("profile") @Mutable void setProfile(GameProfile profile);

    @Accessor("displayName") @Mutable void setDisplayName(Component displayName);

}
