package de.keksuccino.justnicks;

import de.keksuccino.justnicks.util.AbstractOptions;
import de.keksuccino.konkrete.config.Config;

public class Options extends AbstractOptions {

    protected final Config config = new Config(JustNicks.MOD_DIR.getAbsolutePath().replace("\\", "/") + "/options.txt");

    public final Option<Float> exampleOption = new Option<>(config, "example_option", 0.25F, "general");
    public final Option<Boolean> anotherExampleOption = new Option<>(config, "another_example_option", true, "general");

    public Options() {
        this.config.syncConfig();
        this.config.clearUnusedValues();
    }

}