package de.keksuccino.justnicks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod(JustNicks.MOD_ID)
public class JustNicksNeoForge {
    
    public JustNicksNeoForge(@NotNull IEventBus eventBus) {

        // JustNicks.init() got moved to MixinMinecraft

        JustNicksNeoForgeServerEvents.registerAll();

    }

}