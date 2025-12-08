package de.keksuccino.justnicks;

import de.keksuccino.justnicks.util.AbstractOptions;
import de.keksuccino.konkrete.config.Config;

public class Options extends AbstractOptions {

    protected final Config config = new Config(JustNicks.MOD_DIR.getAbsolutePath().replace("\\", "/") + "/options.txt");

    public final Option<Boolean> showOriginalIdentityToSelfPlayer = new Option<>(config, "show_original_identity_to_self_player", true, "general");
    public final Option<Boolean> refreshSelfOnNick = new Option<>(config, "refresh_self_player_on_nick", true, "general");

    public Options() {
        this.config.syncConfig();
        this.config.clearUnusedValues();
    }

}
