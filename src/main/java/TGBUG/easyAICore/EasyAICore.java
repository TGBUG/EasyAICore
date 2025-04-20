package TGBUG.easyAICore;

import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class EasyAICore extends JavaPlugin {
    // 单例引用
    @Getter private static EasyAICore instance;

    @Getter private AIService aiService;

    private ChatHistoryDatabase dbManager;

    @Override
    public void onEnable() {
        instance = this;
        this.loadServicesAndCommands();
        // 初始化数据库
        this.dbManager = new ChatHistoryDatabase(this);
        getLogger().info("ChatHistoryDatabase 已初始化");
    }

    @Override
    public void onDisable() {
        // 关闭数据库
        if (dbManager != null) {
            dbManager.close();
        }
        getLogger().info("EasyAICore 已禁用，数据库连接已关闭");
    }

    /**
     * 加载或重载 AIService、命令及 TabCompleter。
     */
    public void loadServicesAndCommands() {
        // 1. 载入配置文件 (会将 resources/config.yml 覆盖到插件目录)
        saveDefaultConfig();
        reloadConfig();

        boolean registerAiCmd = getConfig().getBoolean("register-ai-command", true);
        int rateLimitCount = getConfig().getInt("rate-limit-count", 5);
        int rateLimitInterval = getConfig().getInt("rate-limit-interval", 1);

        aiService = new OpenAIService(
                this,
                getConfig().getString("api-key"),
                getConfig().getString("api-endpoint"),
                getConfig().getString("model"),
                getConfig().getInt("timeout-seconds"),
                getConfig().getString("system-message")
        );

        // 2. 构建新的 Commands 执行器（拿到最新 config 参数）
        CommandExecutor commandExecutor = new Commands(
                aiService,
                getDescription().getVersion(),
                getConfig().getInt("max-tokens"),
                getConfig().getDouble("temperature"),
                getConfig().getInt("history-per-page"),
                getConfig().getString("waiting-message"),
                getConfig().getString("AI-response-prefix"),
                registerAiCmd,
                rateLimitCount,
                rateLimitInterval,
                getConfig().getString("exceed-limit-message")
        );
        TabCompleter tabCompleter = new TabComplete();

        // 3. 重新注册命令
        if (registerAiCmd) {
            getCommand("aichat").setExecutor(commandExecutor);
        }
        getCommand("easyaicore").setExecutor(commandExecutor);
        getCommand("easyaicore").setTabCompleter(tabCompleter);

        getLogger().info("EasyAICore 已加载");
    }
}
