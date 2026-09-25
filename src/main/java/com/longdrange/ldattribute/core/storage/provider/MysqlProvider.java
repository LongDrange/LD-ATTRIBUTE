package com.longdrange.ldattribute.core.storage.provider;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import com.longdrange.ldattribute.core.storage.StorageProvider;
import com.longdrange.ldattribute.core.storage.StorageType;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.UUID;

public class MysqlProvider implements StorageProvider {

    private final LDAttribute plugin;
    private String url;
    private String user;
    private String password;
    private String table;

    public MysqlProvider(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public void init() throws Exception {
        FileConfiguration c = plugin.getCoreManager().getCoreConfig();

        String host = c.getString("storage.mysql.host", "127.0.0.1");
        int port = c.getInt("storage.mysql.port", 3306);
        String db = c.getString("storage.mysql.database", "ldattribute");
        this.user = c.getString("storage.mysql.username", "root");
        this.password = c.getString("storage.mysql.password", "");
        String prefix = c.getString("storage.mysql.table-prefix", "ldcore_");
        boolean useSSL = c.getBoolean("storage.mysql.useSSL", false);
        int timeout = c.getInt("storage.mysql.connect-timeout", 5000);
        this.table = prefix + "player_data";

        this.url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=" + useSSL
                + "&characterEncoding=utf8"
                + "&useUnicode=true"
                + "&connectTimeout=" + timeout
                + "&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true";

        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                "  `uuid`   VARCHAR(36) NOT NULL," +
                "  `module` VARCHAR(64) NOT NULL," +
                "  `data`   LONGTEXT," +
                "  PRIMARY KEY (`uuid`, `module`)" +
                ") DEFAULT CHARSET=utf8mb4"
            );
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override public void shutdown() { }

    @Override
    public PlayerModuleData load(UUID uuid, String module) {
        PlayerModuleData data = new PlayerModuleData(uuid, module);
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT `data` FROM `" + table + "` WHERE `uuid`=? AND `module`=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) data.deserialize(rs.getString("data"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Core/MySQL] 读取失败: " + e.getMessage());
        }
        data.markClean();
        return data;
    }

    @Override
    public void save(UUID uuid, String module, PlayerModuleData data) {
        String sql = "INSERT INTO `" + table + "` (`uuid`, `module`, `data`) VALUES (?,?,?) "
                   + "ON DUPLICATE KEY UPDATE `data`=VALUES(`data`)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            ps.setString(3, data.serialize());
            ps.executeUpdate();
            data.markClean();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Core/MySQL] 保存失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(UUID uuid, String module) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM `" + table + "` WHERE `uuid`=? AND `module`=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            ps.executeUpdate();
        } catch (SQLException ignored) { }
    }

    @Override
    public boolean exists(UUID uuid, String module) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT 1 FROM `" + table + "` WHERE `uuid`=? AND `module`=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, module);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    @Override public StorageType getType() { return StorageType.MYSQL; }
}