package TGBUG.easyAICore;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ChatHistoryDatabase {
    private final JavaPlugin plugin;
    private Connection connection;

    public ChatHistoryDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!setupDatabase()) {
            plugin.onDisable();
        }
    }

    private Boolean setupDatabase() {
        try {
            // 1. 在插件数据目录中定位或创建 db 文件
            File dbFile = new File(plugin.getDataFolder(), "chat_history.db");
            if (!dbFile.exists()) {
                plugin.getLogger().info("数据库文件不存在，创建 chat_history.db");              // :contentReference[oaicite:0]{index=0}
                dbFile.getParentFile().mkdirs();                                         // :contentReference[oaicite:1]{index=1}
                dbFile.createNewFile();                                                   // :contentReference[oaicite:2]{index=2}
            }

            // 2. 连接 SQLite，若文件不存在会自动创建（jdbc:sqlite:filename.db）       // :contentReference[oaicite:3]{index=3}
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            Class.forName("org.sqlite.JDBC");                                           // :contentReference[oaicite:4]{index=4}
            connection = DriverManager.getConnection(url);                               // :contentReference[oaicite:5]{index=5}

            // 3. 创建记录表（如果尚不存在）
            try (Statement stmt = connection.createStatement()) {                       // :contentReference[oaicite:6]{index=6}
                String sql =
                        "CREATE TABLE IF NOT EXISTS chat_history (" +
                                "  chat_uuid TEXT NOT NULL," +
                                "  prompt      TEXT NOT NULL," +
                                "  response    TEXT NOT NULL," +
                                "  timestamp   INTEGER NOT NULL," +
                                "  PRIMARY KEY(chat_uuid, timestamp)" +
                                ");";                                                                // :contentReference[oaicite:7]{index=7}
                stmt.execute(sql);                                                       // :contentReference[oaicite:8]{index=8}
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "无法初始化聊天记录数据库", e);
            return false;                                                            // :contentReference[oaicite:9]{index=9}
        }
        return true;
    }

    /**
     * 将一条对话记录插入到 chat_history 表
     */
    public boolean logChat(String chatUuid, String prompt, String response) {
        String sql = "INSERT INTO chat_history(chat_uuid,prompt,response,timestamp) VALUES (?,?,?,?);";
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chatUuid);
            ps.setString(2, prompt);
            ps.setString(3, response);
            ps.setLong(4, now);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "聊天记录写入失败", e);
            return false;
        }
    }

    /** 在插件禁用时关闭连接 */
    public void close() {
        if (connection != null) {
            try {
                connection.close();                                                   // :contentReference[oaicite:13]{index=13}
            } catch (SQLException ignored) {}
        }
    }

    /**
     * 获取指定 chatUuid 的所有对话记录（按时间升序，交替 user/assistant）。
     *
     * @param chatUuid 聊天标识（玩家名或 "console"）
     * @return 包含 role/content 键的 Map 列表，已按发送顺序排列
     */
    public List<Map<String, String>> getHistory(String chatUuid) {
        String sql = "SELECT prompt, response FROM chat_history " +
                "WHERE chat_uuid = ? " +
                "ORDER BY timestamp ASC;";                              // SELECT ALL 默认返回全部行，ORDER BY 保证正序&#8203;:contentReference[oaicite:0]{index=0}
        List<Map<String,String>> history = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {               // 使用 PreparedStatement 防注入并提升性能&#8203;:contentReference[oaicite:1]{index=1}
            ps.setString(1, chatUuid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(Map.of(
                            "role",    "user",
                            "content", rs.getString("prompt")
                    ));
                    history.add(Map.of(
                            "role",    "assistant",
                            "content", rs.getString("response")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载ChatUUID=" + chatUuid + "的聊天历史失败: " + e.getMessage());
            return null;                                                            // :contentReference[oaicite:2]{index=2}
        }
        return history;
    }

    /**
     * 删除指定 chatUuid 的所有聊天记录。
     *
     * @param chatUuid 聊天标识（玩家名或 "console"）
     * @return
     */
    public boolean deleteChatHistory(String chatUuid) {
        String sql = "DELETE FROM chat_history WHERE chat_uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chatUuid);
            int count = ps.executeUpdate();
            plugin.getLogger().info("已删除 " + count + " 条记录 for " + chatUuid);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "删除聊天记录失败: " + chatUuid, e);
            return false;
        }
    }

}
