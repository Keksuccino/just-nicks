package de.keksuccino.justnicks;

import de.keksuccino.justnicks.platform.Services;
import de.keksuccino.justnicks.util.GameDirectoryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;

public class JustNicks {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String VERSION = "1.0.0";
    public static final String LOADER = Services.PLATFORM.getPlatformName().toUpperCase();
    public static final String MOD_ID = "justnicks";
    public static final File MOD_DIR = createDirectory(new File(GameDirectoryUtils.getGameDirectory(), "/config/justnicks"));

    private static Options options;

    public static void init() {

        LOGGER.info("[JUST NICKS] Starting version " + VERSION + " on " + Services.PLATFORM.getPlatformDisplayName() + "..");

    }

    public static void updateOptions() {
        options = new Options();
    }

    @NotNull
    public static Options getOptions() {
        if (options == null) updateOptions();
        return options;
    }

    private static File createDirectory(@NotNull File file) {
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return file;
    }

}
