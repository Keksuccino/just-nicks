package de.keksuccino.justnicks;

import net.fabricmc.api.ModInitializer;

public class JustNicksFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        JustNicks.init();

        JustNicksFabricServerEvents.registerAll();

    }

}
