package de.keksuccino.justnicks;

import de.keksuccino.justnicks.util.AbstractOptions;
import de.keksuccino.konkrete.config.Config;

public class Options extends AbstractOptions {

    protected final Config config = new Config(JustNicks.MOD_DIR.getAbsolutePath().replace("\\", "/") + "/options.txt");

    public static final String PERSISTENT_NICKS_REMOTE_URL_PLACEHOLDER = "https://example.com/justnicks/api";
    public static final String PERSISTENT_NICKS_REMOTE_TOKEN_PLACEHOLDER = "example_token_here";

    public final Option<Boolean> showOriginalIdentityToSelf = register(new Option<>(config, "show_original_identity_to_self_player", true, "general"));
    public final Option<Boolean> refreshSelfOnNick = register(new Option<>(config, "refresh_self_player_on_nick", false, "general"));
    public final Option<Boolean> persistentNicks = register(new Option<>(config, "persistent_nicks", true, "general"));
    public final Option<String> persistentNicksRemoteUrl = register(new Option<>(config, "persistent_nicks_remote_url", PERSISTENT_NICKS_REMOTE_URL_PLACEHOLDER, "general"));
    public final Option<String> persistentNicksRemoteToken = register(new Option<>(config, "persistent_nicks_remote_token", PERSISTENT_NICKS_REMOTE_TOKEN_PLACEHOLDER, "general"));

    public Options() {
        this.config.syncConfig();
        this.config.clearUnusedValues();
    }

}
