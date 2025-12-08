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
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loads the bundled pool of pre-fetched signed skins (assets/justnicks/skins.json).
 */
public class Skins {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String RESOURCE_PATH = "/assets/justnicks/skins.json";
    private static final List<SignedSkin> SKINS = loadSkins();

    public static SignedSkin randomSkin() {
        if (SKINS.isEmpty()) {
            return null;
        }
        return SKINS.get(ThreadLocalRandom.current().nextInt(SKINS.size()));
    }

    public static Optional<SignedSkin> findByName(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        String needle = name.trim();
        for (SignedSkin skin : SKINS) {
            if (skin.name() != null && skin.name().equalsIgnoreCase(needle)) {
                return Optional.of(skin);
            }
        }
        return Optional.empty();
    }

    private static List<SignedSkin> loadSkins() {
        try (InputStream stream = Skins.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                LOGGER.warn("[JUST NICKS] Could not find skins.json at {}", RESOURCE_PATH);
                return Collections.emptyList();
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("skins");
            List<SignedSkin> out = new ArrayList<>();
            if (array != null) {
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject obj = element.getAsJsonObject();
                    if (!obj.has("value") || !obj.has("signature")) continue;
                    String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
                    String name = obj.has("name") ? obj.get("name").getAsString() : "";
                    String value = obj.get("value").getAsString();
                    String signature = obj.get("signature").getAsString();
                    out.add(new SignedSkin(uuid, name, value, signature));
                }
            }
            LOGGER.info("[JUST NICKS] Loaded {} signed skins.", out.size());
            return Collections.unmodifiableList(out);
        } catch (Exception ex) {
            LOGGER.error("[JUST NICKS] Failed to load skins.json!", ex);
            return Collections.emptyList();
        }
    }
}
