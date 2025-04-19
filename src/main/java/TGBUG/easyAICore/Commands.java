package TGBUG.easyAICore;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

import static org.apache.commons.lang.StringUtils.isNumeric;

public class Commands implements CommandExecutor {
    private final AIService ai;
    private final String version;
    private final Integer maxTokens, historyPerPage;
    private final Double temperature;
    private final String waitingMsg, responsePrefix;

    public Commands(AIService ai,
                    String version,
                    Integer maxTokens,
                    Double temperature,
                    Integer historyPerPage,
                    String waitingMsg,
                    String responsePrefix) {
        this.ai = ai;
        this.version = version;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.historyPerPage = historyPerPage;
        this.waitingMsg = waitingMsg;
        this.responsePrefix = responsePrefix;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String l, String[] args) {
        String name = cmd.getName().toLowerCase();
        String uuid = (s instanceof Player) ? s.getName() : "console";

        if (name.equals("aichat") || name.equals("ai")) {
            if (!s.hasPermission("easyaicore.command.aichat")) {
                send(s,"§c无权限");
                return true;
            }
            if (args.length==0) {
                send(s,"用法: /ai <提示>");
                return true;
            }
            String prompt = String.join(" ", args);
            send(s, waitingMsg);

            ai.chat(uuid, prompt, maxTokens, temperature)
                    .thenAccept(ans -> send(s, responsePrefix + ans))
                    .exceptionally(ex -> { send(s,"§cAI 错误: "+ex.getMessage()); return null; });
            return true;
        }

        if (name.equals("easyaicore") || name.equals("eac")) {
            if (!s.hasPermission("easyaicore.command.easyaicore")) {
                send(s,"§c无权限");
                return true;
            }
            if (args.length==0) {
                send(s,"§bEasyAICore §f版本: "+version);
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "help":
                    send(s,"§b/eac reload §7重载配置");
                    send(s,"§b/eac history [page|reset] §7查/清历史");
                    break;
                case "reload":
                    if (!s.hasPermission("easyaicore.command.easyaicore.reload")) {
                        send(s,"§c无权限");
                    } else {
                        EasyAICore.getInstance().loadServicesAndCommands();
                        send(s,"§f插件已热重载");
                    }
                    break;
                case "history":
                    if (!s.hasPermission("easyaicore.command.easyaicore.history")) {
                        send(s,"§c无权限");
                    } else if (args.length==2 && args[1].equalsIgnoreCase("reset")) {
                        if (ai.deleteHistory(uuid)) send(s,"§f历史已清空");
                        else                    send(s,"§c清空失败");
                    } else if (args.length==2 && isNumeric(args[1])) {
                        List<Map<String,String>> h = ai.getHistory(uuid);
                        if (h.isEmpty()) {
                            send(s,"§f无历史");
                        } else {
                            int page = Integer.parseInt(args[1]);
                            int totalConv = h.size()/2;
                            int start = (page-1)*historyPerPage;
                            if (start<0 || start>=totalConv) {
                                send(s,"§c页码错误");
                            } else {
                                int end = Math.min(page*historyPerPage, totalConv);
                                send(s,"§f历史对话 (第"+page+"页):");
                                for (int i=start*2;i<end*2;i+=2) {
                                    String q = h.get(i).get("content");
                                    String a = h.get(i+1).get("content");
                                    send(s, "§7"+q+" → "+a);
                                }
                                send(s,"§f/eac history reset §7重置历史");
                            }
                        }
                    } else {
                        send(s,"§f用法: /eac history [页码|reset]");
                    }
                    break;
                default:
                    send(s,"§c未知子命令");
            }
            return true;
        }

        return false;
    }

    private void send(CommandSender s, String msg) {
        s.sendMessage("§b[EasyAICore] " + msg);
    }
}
