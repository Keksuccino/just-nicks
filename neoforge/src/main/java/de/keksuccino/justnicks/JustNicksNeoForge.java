package de.keksuccino.justnicks;

import de.keksuccino.justnicks.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.NotNull;

@Mod(JustNicks.MOD_ID)
public class JustNicksNeoForge {
    
    public JustNicksNeoForge(@NotNull IEventBus eventBus) {

        // JustNicks.init() got moved to MixinMinecraft

        if (Services.PLATFORM.isOnClient()) {

            eventBus.register(JustNicksNeoForge.class);

        }

    }

    @SubscribeEvent
    public static void onRegisterKeybinds(RegisterKeyMappingsEvent e) {

        e.register(KeyMappings.KEY_TOGGLE_ZOOM);

    }

}