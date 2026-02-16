package me.typical.lixiplugin.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.typical.lixiplugin.LXPlugin;
import me.typical.lixiplugin.config.types.MainConfig;
import me.typical.lixiplugin.economy.LixiCurrency;
import me.typical.lixiplugin.util.MessageUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** HikariCP database (SQLite/MariaDB). */
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
                    status VARCHAR(10) NOT NULL DEFAULT 'UNUSED',
                    currency_type VARCHAR(10) NOT NULL DEFAULT 'VAULT'
                )
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createEnvelopesTable)) {
            stmt.execute();
            migrateAddCurrencyType(conn);
            MessageUtil.info("Database tables created successfully");
        } catch (SQLException e) {
            MessageUtil.error("Failed to create database tables");
            e.printStackTrace();
        }
    }

    private void migrateAddCurrencyType(Connection conn) {
        try {
            conn.prepareStatement("ALTER TABLE envelopes ADD COLUMN currency_type VARCHAR(10) NOT NULL DEFAULT 'VAULT'").executeUpdate();
            MessageUtil.info("Database migrated: added currency_type column");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                MessageUtil.warn("Migration note: " + e.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            MessageUtil.info("Database connection pool closed");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> insertEnvelope(UUID id, double amount, UUID creator, LixiCurrency currencyType) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO envelopes (uuid, amount, creator_uuid, status, currency_type) VALUES (?, ?, ?, 'UNUSED', ?)";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                stmt.setDouble(2, amount);
                stmt.setString(3, creator.toString());
                stmt.setString(4, currencyType.name());
                stmt.executeUpdate();
            } catch (SQLException e) {
                MessageUtil.error("Failed to insert envelope: " + id);
                e.printStackTrace();
            }
        });
    }

    /** Atomic claim via UPDATE WHERE status='UNUSED'. */
    public CompletableFuture<Optional<ClaimResult>> claimEnvelope(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String updateSql = "UPDATE envelopes SET status = 'CLAIMED' WHERE uuid = ? AND status = 'UNUSED'";
            String selectSql = "SELECT amount, currency_type FROM envelopes WHERE uuid = ?";

            try (Connection conn = getConnection()) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, id.toString());
                    if (updateStmt.executeUpdate() == 0) {
                        return Optional.empty();
                    }
                }

                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, id.toString());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            double amount = rs.getDouble("amount");
                            String currencyStr = "VAULT";
                            try {
                                currencyStr = rs.getString("currency_type");
                            } catch (SQLException ignored) {
                            }
                            LixiCurrency currency = LixiCurrency.valueOf(currencyStr != null ? currencyStr : "VAULT");
                            return Optional.of(new ClaimResult(amount, currency));
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

    public record ClaimResult(double amount, LixiCurrency currency) {    }

    public CompletableFuture<Optional<EnvelopeData>> getEnvelope(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT amount, creator_uuid, status, currency_type FROM envelopes WHERE uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String currencyStr = "VAULT";
                        try {
                            currencyStr = rs.getString("currency_type");
                        } catch (SQLException ignored) {
                        }
                        LixiCurrency currency = LixiCurrency.valueOf(currencyStr != null ? currencyStr : "VAULT");
                        return Optional.of(new EnvelopeData(
                                rs.getDouble("amount"),
                                UUID.fromString(rs.getString("creator_uuid")),
                                rs.getString("status"),
                                currency
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

    public record EnvelopeData(double amount, UUID creator, String status, LixiCurrency currency) {
        public EnvelopeData(double amount, UUID creator, String status) {
            this(amount, creator, status, LixiCurrency.VAULT);
        }
    }
}
