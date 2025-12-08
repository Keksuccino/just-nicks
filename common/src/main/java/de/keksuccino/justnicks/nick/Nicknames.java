package de.keksuccino.justnicks.nick;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loads the bundled nickname pool (assets/justnicks/nicknames.json).
 */
public class Nicknames {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String RESOURCE_PATH = "/assets/justnicks/nicknames.json";
    private static final List<String> NICKNAMES = loadNicknames();

    public static String randomNickname() {
        if (NICKNAMES.isEmpty()) {
            return "Player";
        }
        return NICKNAMES.get(ThreadLocalRandom.current().nextInt(NICKNAMES.size()));
    }

    private static List<String> loadNicknames() {
        try (InputStream stream = Nicknames.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                LOGGER.warn("[JUST NICKS] Could not find nicknames.json at {}", RESOURCE_PATH);
                return Collections.emptyList();
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("nicknames");
            List<String> out = new ArrayList<>();
            if (array != null) {
                for (JsonElement element : array) {
                    if (element.isJsonPrimitive()) {
                        String name = element.getAsString().trim();
                        if (!name.isEmpty()) {
                            out.add(name);
                        }
                    }
                }
            }
            LOGGER.info("[JUST NICKS] Loaded {} nicknames.", out.size());
            return Collections.unmodifiableList(out);
        } catch (Exception ex) {
            LOGGER.error("[JUST NICKS] Failed to load nicknames.json!", ex);
            return Collections.emptyList();
        }
    }
}
