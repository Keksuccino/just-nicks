package de.keksuccino.justnicks.nick;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.justnicks.JustNicks;
import de.keksuccino.justnicks.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists nicknames so they can be restored after server restarts.
 * Uses either the local SQLite file or an optional remote HTTP backend, depending on user options.
 */
public final class PersistentNickStore {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final File DB_FILE = new File(JustNicks.MOD_DIR, "persistent_nicks.db");
    private static final String CONNECTION_STRING = "jdbc:sqlite:" + DB_FILE.getAbsolutePath().replace("\\", "/");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static volatile boolean schemaReady = false;

    private PersistentNickStore() {
    }

    private static void ensureSchema() {
        if (schemaReady) {
            return;
        }
        synchronized (PersistentNickStore.class) {
            if (schemaReady) {
                return;
            }
            try {
                if (!DB_FILE.getParentFile().exists()) {
                    DB_FILE.getParentFile().mkdirs();
                }
                try (Connection connection = DriverManager.getConnection(CONNECTION_STRING);
                     Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS persistent_nicks (" +
                            "uuid TEXT PRIMARY KEY, " +
                            "real_name TEXT NOT NULL, " +
                            "nickname TEXT NOT NULL, " +
                            "skin_uuid TEXT, " +
                            "skin_name TEXT, " +
                            "skin_value TEXT, " +
                            "skin_signature TEXT" +
                            ")");
                }
                schemaReady = true;
            } catch (SQLException ex) {
                LOGGER.error("[JUST NICKS] Failed to initialize persistent nick database at {}", DB_FILE.getAbsolutePath(), ex);
            }
        }
    }

    public static void save(@NotNull UUID uuid, @NotNull String realName, @NotNull String nickname, @Nullable SignedSkin skin) {
        RemoteConfig remote = resolveRemoteConfig();
        if (remote != null) {
            RemoteResult<Void> result = saveRemote(remote, uuid, realName, nickname, skin);
            if (!result.failed()) {
                return; // synced remotely, done
            }
            LOGGER.warn("[JUST NICKS] Falling back to local nick persistence after remote save failed for {}", uuid);
        }
        saveLocal(uuid, realName, nickname, skin);
    }

    public static Optional<StoredNick> load(@NotNull UUID uuid) {
        RemoteConfig remote = resolveRemoteConfig();
        if (remote != null) {
            RemoteResult<Optional<StoredNick>> result = loadRemote(remote, uuid);
            if (!result.failed()) {
                return result.value() == null ? Optional.empty() : result.value();
            }
            LOGGER.warn("[JUST NICKS] Falling back to local nick persistence after remote load failed for {}", uuid);
        }
        return loadLocal(uuid);
    }

    public static void delete(@NotNull UUID uuid) {
        RemoteConfig remote = resolveRemoteConfig();
        if (remote != null) {
            RemoteResult<Void> result = deleteRemote(remote, uuid);
            if (!result.failed()) {
                return;
            }
            LOGGER.warn("[JUST NICKS] Falling back to local nick persistence after remote delete failed for {}", uuid);
        }
        deleteLocal(uuid);
    }

    private static void saveLocal(@NotNull UUID uuid, @NotNull String realName, @NotNull String nickname, @Nullable SignedSkin skin) {
        ensureSchema();
        if (!schemaReady) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING);
             PreparedStatement statement = connection.prepareStatement("INSERT INTO persistent_nicks (uuid, real_name, nickname, skin_uuid, skin_name, skin_value, skin_signature) VALUES (?,?,?,?,?,?,?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET real_name=excluded.real_name, nickname=excluded.nickname, skin_uuid=excluded.skin_uuid, skin_name=excluded.skin_name, skin_value=excluded.skin_value, skin_signature=excluded.skin_signature")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, realName);
            statement.setString(3, nickname);
            if (skin != null) {
                statement.setString(4, skin.uuid());
                statement.setString(5, skin.name());
                statement.setString(6, skin.value());
                statement.setString(7, skin.signature());
            } else {
                statement.setNull(4, Types.VARCHAR);
                statement.setNull(5, Types.VARCHAR);
                statement.setNull(6, Types.VARCHAR);
                statement.setNull(7, Types.VARCHAR);
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("[JUST NICKS] Failed to persist nickname locally for {}", nickname, ex);
        }
    }

    private static Optional<StoredNick> loadLocal(@NotNull UUID uuid) {
        ensureSchema();
        if (!schemaReady) {
            return Optional.empty();
        }
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING);
             PreparedStatement statement = connection.prepareStatement("SELECT real_name, nickname, skin_uuid, skin_name, skin_value, skin_signature FROM persistent_nicks WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String realName = resultSet.getString("real_name");
                String nickname = resultSet.getString("nickname");

                String skinUuid = resultSet.getString("skin_uuid");
                String skinName = resultSet.getString("skin_name");
                String skinValue = resultSet.getString("skin_value");
                String skinSignature = resultSet.getString("skin_signature");

                SignedSkin skin = null;
                if (skinUuid != null && skinValue != null && skinSignature != null) {
                    skin = new SignedSkin(skinUuid, skinName != null ? skinName : skinUuid, skinValue, skinSignature);
                }

                return Optional.of(new StoredNick(realName, nickname, skin));
            }
        } catch (SQLException ex) {
            LOGGER.error("[JUST NICKS] Failed to load persistent nick locally for {}", uuid, ex);
            return Optional.empty();
        }
    }

    private static void deleteLocal(@NotNull UUID uuid) {
        ensureSchema();
        if (!schemaReady) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM persistent_nicks WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("[JUST NICKS] Failed to delete persistent nick locally for {}", uuid, ex);
        }
    }

    private static RemoteResult<Void> saveRemote(@NotNull RemoteConfig config, @NotNull UUID uuid, @NotNull String realName, @NotNull String nickname, @Nullable SignedSkin skin) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", uuid.toString());
            payload.addProperty("real_name", realName);
            payload.addProperty("nickname", nickname);
            if (skin != null) {
                payload.addProperty("skin_uuid", skin.uuid());
                payload.addProperty("skin_name", skin.name());
                payload.addProperty("skin_value", skin.value());
                payload.addProperty("skin_signature", skin.signature());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri(config, "/nicks/" + uuid))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + config.token())
                    .PUT(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (isSuccess(response.statusCode())) {
                return RemoteResult.success(null);
            }
            LOGGER.warn("[JUST NICKS] Remote nick save returned {} for {}", response.statusCode(), uuid);
        } catch (Exception ex) {
            LOGGER.error("[JUST NICKS] Failed to sync nickname to remote store for {}", uuid, ex);
        }
        return RemoteResult.failure();
    }

    private static RemoteResult<Optional<StoredNick>> loadRemote(@NotNull RemoteConfig config, @NotNull UUID uuid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri(config, "/nicks/" + uuid))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + config.token())
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 404) {
                return RemoteResult.success(Optional.empty());
            }
            if (isSuccess(status)) {
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                String realName = getAsString(body.get("real_name"));
                String nickname = getAsString(body.get("nickname"));
                if (realName == null || nickname == null) {
                    LOGGER.warn("[JUST NICKS] Remote nick payload missing fields for {}", uuid);
                    return RemoteResult.failure();
                }

                String skinUuid = getAsString(body.get("skin_uuid"));
                String skinName = getAsString(body.get("skin_name"));
                String skinValue = getAsString(body.get("skin_value"));
                String skinSignature = getAsString(body.get("skin_signature"));

                SignedSkin skin = null;
                if (skinUuid != null && skinValue != null && skinSignature != null) {
                    skin = new SignedSkin(skinUuid, skinName != null ? skinName : skinUuid, skinValue, skinSignature);
                }

                return RemoteResult.success(Optional.of(new StoredNick(realName, nickname, skin)));
            }
            LOGGER.warn("[JUST NICKS] Remote nick load returned {} for {}", status, uuid);
        } catch (Exception ex) {
            LOGGER.error("[JUST NICKS] Failed to fetch nickname from remote store for {}", uuid, ex);
        }
        return RemoteResult.failure();
    }

    private static RemoteResult<Void> deleteRemote(@NotNull RemoteConfig config, @NotNull UUID uuid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri(config, "/nicks/" + uuid))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + config.token())
                    .DELETE()
                    .build();

            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status == 404 || isSuccess(status)) {
                return RemoteResult.success(null);
            }
            LOGGER.warn("[JUST NICKS] Remote nick delete returned {} for {}", status, uuid);
        } catch (Exception ex) {
            LOGGER.error("[JUST NICKS] Failed to delete nickname from remote store for {}", uuid, ex);
        }
        return RemoteResult.failure();
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static URI buildUri(@NotNull RemoteConfig config, @NotNull String path) {
        String base = config.baseUrl();
        if (path.startsWith("/")) {
            return URI.create(base + path);
        }
        return URI.create(base + "/" + path);
    }

    private static RemoteConfig resolveRemoteConfig() {
        Options options = JustNicks.getOptions();
        String url = normalize(options.persistentNicksRemoteUrl.getValue());
        String token = normalize(options.persistentNicksRemoteToken.getValue());

        if (isPlaceholder(url, Options.PERSISTENT_NICKS_REMOTE_URL_PLACEHOLDER) || isPlaceholder(token, Options.PERSISTENT_NICKS_REMOTE_TOKEN_PLACEHOLDER)) {
            return null;
        }
        if (url.isEmpty() || token.isEmpty()) {
            return null;
        }
        return new RemoteConfig(trimTrailingSlash(url), token);
    }

    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isPlaceholder(@NotNull String value, @NotNull String placeholder) {
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.equalsIgnoreCase(placeholder);
    }

    private static String trimTrailingSlash(@NotNull String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    @Nullable
    private static String getAsString(@Nullable JsonElement element) {
        return element != null && !element.isJsonNull() ? element.getAsString() : null;
    }

    private record RemoteConfig(@NotNull String baseUrl, @NotNull String token) {
    }

    private record RemoteResult<T>(@Nullable T value, boolean failed) {
        private static <T> RemoteResult<T> success(@Nullable T value) {
            return new RemoteResult<>(value, false);
        }

        private static <T> RemoteResult<T> failure() {
            return new RemoteResult<>(null, true);
        }
    }

    public record StoredNick(@NotNull String realName, @NotNull String nickname, @Nullable SignedSkin skin) {
    }
}
