package de.keksuccino.justnicks.nick;

import de.keksuccino.justnicks.JustNicks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists nicknames so they can be restored after server restarts.
 */
public final class PersistentNickStore {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final File DB_FILE = new File(JustNicks.MOD_DIR, "persistent_nicks.db");
    private static final String CONNECTION_STRING = "jdbc:sqlite:" + DB_FILE.getAbsolutePath().replace("\\", "/");
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
            LOGGER.error("[JUST NICKS] Failed to persist nickname for {}", nickname, ex);
        }
    }

    public static Optional<StoredNick> load(@NotNull UUID uuid) {
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
            LOGGER.error("[JUST NICKS] Failed to load persistent nick for {}", uuid, ex);
            return Optional.empty();
        }
    }

    public static void delete(@NotNull UUID uuid) {
        ensureSchema();
        if (!schemaReady) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM persistent_nicks WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("[JUST NICKS] Failed to delete persistent nick for {}", uuid, ex);
        }
    }

    public record StoredNick(@NotNull String realName, @NotNull String nickname, @Nullable SignedSkin skin) {
    }
}
