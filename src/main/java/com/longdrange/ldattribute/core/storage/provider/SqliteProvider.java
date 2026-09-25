package com.longdrange.ldattribute.core.storage.provider;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import com.longdrange.ldattribute.core.storage.StorageProvider;
import com.longdrange.ldattribute.core.storage.StorageType;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SqliteProvider implements StorageProvider {

    private final LDAttribute plugin;
    private String url;

    public SqliteProvider(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public void init() throws Exception {
        File dir = new File(plugin.getDataFolder(), "core-data");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("无法创建 core-data 目录");
        File db = new File(dir, "core.db");
        this.url = "jdbc:sqlite:" + db.getAbsolutePath();

        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS ldcore_player_data (" +
                "  uuid   TEXT NOT NULL," +
                "  module TEXT NOT NULL," +
                "  data   TEXT," +
                "  PRIMARY KEY (uuid, module)" +
                ")"
            );
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override public void shutdown() { }

    @Override
    public PlayerModuleData load(UUID uuid, String module) {
        PlayerModuleData data = new PlayerModuleData(uuid, module);
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT data FROM ldcore_player_data WHERE uuid=? AND module=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) data.deserialize(rs.getString("data"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Core/SQLite] 读取失败: " + e.getMessage());
        }
        data.markClean();
        return data;
    }

    @Override
    public void save(UUID uuid, String module, PlayerModuleData data) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT OR REPLACE INTO ldcore_player_data(uuid, module, data) VALUES(?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            ps.setString(3, data.serialize());
            ps.executeUpdate();
            data.markClean();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Core/SQLite] 保存失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(UUID uuid, String module) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM ldcore_player_data WHERE uuid=? AND module=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            ps.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public boolean exists(UUID uuid, String module) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT 1 FROM ldcore_player_data WHERE uuid=? AND module=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    @Override public StorageType getType() { return StorageType.SQLITE; }
}