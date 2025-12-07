package de.keksuccino.justnicks;

import de.keksuccino.justnicks.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class JustNicksFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        JustNicks.init();

        if (Services.PLATFORM.isOnClient()) {

            KeyBindingHelper.registerKeyBinding(KeyMappings.KEY_TOGGLE_ZOOM);

        }

        JustNicksFabricServerEvents.registerAll();

    }

}
