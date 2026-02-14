package me.typical.lixiplugin.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.util.MessageUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages database connections using HikariCP.
 * Supports both SQLite and MariaDB backends.
 */
public class DatabaseManager implements IService {

    private HikariDataSource dataSource;
    private final LXPlugin plugin = LXPlugin.getInstance();

    @Override
    public void setup() {
        MainConfig.DatabaseConfig dbConfig = plugin.getConfigManager()
                .getConfig(MainConfig.class)
                .getDatabase();

        HikariConfig config = new HikariConfig();

        if (dbConfig.getType().equalsIgnoreCase("MARIADB")) {
            setupMariaDB(config, dbConfig);
        } else {
            setupSQLite(config);
        }

        dataSource = new HikariDataSource(config);
        MessageUtil.info("Database connection pool established");

        // Create tables
        createTables();
    }

    private void setupSQLite(HikariConfig config) {
        File dbFile = new File(plugin.getDataFolder(), "lixi.db");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite only supports one writer
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        MessageUtil.info("Using SQLite database at: " + dbFile.getAbsolutePath());
    }

    private void setupMariaDB(HikariConfig config, MainConfig.DatabaseConfig dbConfig) {
        String jdbcUrl = String.format("jdbc:mariadb://%s:%d/%s",
                dbConfig.getHost(),
                dbConfig.getPort(),
                dbConfig.getDatabase());

        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setUsername(dbConfig.getUsername());
        config.setPassword(dbConfig.getPassword());
        config.setMaximumPoolSize(10);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        MessageUtil.info("Using MariaDB database at: " + dbConfig.getHost() + ":" + dbConfig.getPort());
    }

    private void createTables() {
        String createEnvelopesTable = """
                CREATE TABLE IF NOT EXISTS envelopes (
                    uuid VARCHAR(36) PRIMARY KEY,
                    amount DOUBLE NOT NULL,
                    creator_uuid VARCHAR(36) NOT NULL,
                    status VARCHAR(10) NOT NULL DEFAULT 'UNUSED'
                )
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createEnvelopesTable)) {
            stmt.execute();
            MessageUtil.info("Database tables created successfully");
        } catch (SQLException e) {
            MessageUtil.error("Failed to create database tables");
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            MessageUtil.info("Database connection pool closed");
        }
    }

    /**
     * Get a connection from the pool
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Insert a new envelope into the database
     *
     * @param id      The unique envelope ID
     * @param amount  The amount of money in the envelope
     * @param creator The UUID of the player who created the envelope
     * @return CompletableFuture that completes when insert is done
     */
    public CompletableFuture<Void> insertEnvelope(UUID id, double amount, UUID creator) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO envelopes (uuid, amount, creator_uuid, status) VALUES (?, ?, ?, 'UNUSED')";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                stmt.setDouble(2, amount);
                stmt.setString(3, creator.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                MessageUtil.error("Failed to insert envelope: " + id);
                e.printStackTrace();
            }
        });
    }

    /**
     * Attempt to claim an envelope atomically.
     * Uses UPDATE with WHERE status='UNUSED' to prevent double-claiming.
     *
     * @param id The envelope UUID
     * @return Optional containing the amount if successfully claimed, empty if already claimed or not found
     */
    public CompletableFuture<Optional<Double>> claimEnvelope(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String updateSql = "UPDATE envelopes SET status = 'CLAIMED' WHERE uuid = ? AND status = 'UNUSED'";
            String selectSql = "SELECT amount FROM envelopes WHERE uuid = ?";

            try (Connection conn = getConnection()) {
                // Try to atomically claim the envelope
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, id.toString());
                    int rowsAffected = updateStmt.executeUpdate();

                    // If no rows were affected, envelope was already claimed or doesn't exist
                    if (rowsAffected == 0) {
                        return Optional.empty();
                    }
                }

                // Successfully claimed, now fetch the amount
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, id.toString());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            return Optional.of(rs.getDouble("amount"));
                        }
                    }
                }
            } catch (SQLException e) {
                MessageUtil.error("Failed to claim envelope: " + id);
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }

    /**
     * Get envelope data by UUID
     *
     * @param id The envelope UUID
     * @return CompletableFuture with envelope data (amount, creator, status)
     */
    public CompletableFuture<Optional<EnvelopeData>> getEnvelope(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT amount, creator_uuid, status FROM envelopes WHERE uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new EnvelopeData(
                                rs.getDouble("amount"),
                                UUID.fromString(rs.getString("creator_uuid")),
                                rs.getString("status")
                        ));
                    }
                }
            } catch (SQLException e) {
                MessageUtil.error("Failed to get envelope: " + id);
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }

    /**
     * Data class for envelope information
     */
    public record EnvelopeData(double amount, UUID creator, String status) {
    }
}
