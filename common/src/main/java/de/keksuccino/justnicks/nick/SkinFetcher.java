package de.keksuccino.justnicks.nick;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Lightweight runtime fetcher that resolves a username to a signed skin via Mojang's APIs.
 */
public final class SkinFetcher {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private SkinFetcher() {
    }

    public static Optional<SignedSkin> fetchByUsername(String username) {
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        try {
            HttpRequest profileRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(trimmed, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(7))
                    .GET()
                    .build();
            HttpResponse<String> profileResponse = CLIENT.send(profileRequest, HttpResponse.BodyHandlers.ofString());
            if (profileResponse.statusCode() != 200) {
                return Optional.empty();
            }

            JsonObject profileJson = JsonParser.parseString(profileResponse.body()).getAsJsonObject();
            String id = profileJson.get("id").getAsString();
            String resolvedName = profileJson.get("name").getAsString();

            HttpRequest skinRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false"))
                    .timeout(Duration.ofSeconds(7))
                    .GET()
                    .build();
            HttpResponse<String> skinResponse = CLIENT.send(skinRequest, HttpResponse.BodyHandlers.ofString());
            if (skinResponse.statusCode() != 200) {
                return Optional.empty();
            }

            JsonObject skinJson = JsonParser.parseString(skinResponse.body()).getAsJsonObject();
            JsonArray properties = skinJson.getAsJsonArray("properties");
            if (properties == null) {
                return Optional.empty();
            }

            for (JsonElement element : properties) {
                if (!element.isJsonObject()) continue;
                JsonObject property = element.getAsJsonObject();
                if (!"textures".equals(property.get("name").getAsString())) continue;
                if (!property.has("value") || !property.has("signature")) continue;
                return Optional.of(new SignedSkin(id, resolvedName, property.get("value").getAsString(), property.get("signature").getAsString()));
            }
        } catch (Exception ex) {
            LOGGER.warn("[JUST NICKS] Failed to fetch skin for '{}': {}", trimmed, ex.toString());
        }

        return Optional.empty();
    }
}
