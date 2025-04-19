package TGBUG.easyAICore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AIService 定义与 AI 交互并管理对话历史的统一接口。
 */
public interface AIService {
    /**
     * 发送一次带上下文的对话请求给 AI，并自动记录历史。
     *
     * @param chatUuid    聊天标识（玩家名或 "console"）
     * @param prompt      本次用户输入
     * @param maxTokens   本次请求最大 token
     * @param temperature 本次请求温度
     * @return AI 回复文本
     */
    CompletableFuture<String> chat(String chatUuid, String prompt, Integer maxTokens, Double temperature);

    /** 获取指定 chatUuid 的所有历史消息（按时间升序，交替 user/assistant） */
    List<Map<String,String>> getHistory(String chatUuid);

    /** 记录一次对话（prompt + response），返回是否成功 */
    boolean logChat(String chatUuid, String prompt, String response);

    /** 删除指定 chatUuid 的所有历史，返回是否成功 */
    boolean deleteHistory(String chatUuid);
}
