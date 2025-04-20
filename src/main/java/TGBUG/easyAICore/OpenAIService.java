package TGBUG.easyAICore;

import com.google.gson.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class OpenAIService implements AIService {
    private final HttpClient httpClient;
    private final String apiEndpoint, apiKey, model;
    private final Duration timeout;
    private final ChatHistoryDatabase db;
    private final Gson gson = new Gson();
    private final String systemMessage;

    public OpenAIService(JavaPlugin plugin,
                         String apiKey,
                         String apiEndpoint,
                         String model,
                         int timeoutSeconds,
                         String systemMessage
                        ) {
        this.apiKey      = apiKey;
        this.apiEndpoint = apiEndpoint;
        this.model       = model;
        this.timeout     = Duration.ofSeconds(timeoutSeconds);
        this.httpClient  = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        this.db = new ChatHistoryDatabase(plugin);
        this.systemMessage = systemMessage;
    }

    @Override
    public CompletableFuture<String> chat(String chatUuid,
                                          String prompt,
                                          Integer maxTokens,
                                          Double temperature) {
        // 1. 拼装消息列表
        List<Map<String,String>> history = db.getHistory(chatUuid);
        List<Map<String,String>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content",systemMessage));
        messages.addAll(history);
        messages.add(Map.of("role","user","content",prompt));

        // 2. 构造请求体
        Map<String,Object> body = Map.of(
                "model",       model,
                "messages",    messages,
                "max_tokens",  maxTokens,
                "temperature", temperature
        );
        String json = gson.toJson(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint))
                .timeout(timeout)
                .header("Content-Type","application/json")
                .header("Authorization","Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // 3. 发送并处理
        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode()!=200) {
                        throw new RuntimeException("OpenAI 错误: "+ resp.statusCode()+" "+resp.body());
                    }
                    JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
                    JsonArray ch = root.getAsJsonArray("choices");
                    String answer = ch.get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString().trim();
                    // 4. 记录历史
                    db.logChat(chatUuid, prompt, answer);
                    return answer;
                });
    }

    @Override
    public List<Map<String,String>> getHistory(String chatUuid) {
        return db.getHistory(chatUuid);
    }

    @Override
    public boolean logChat(String chatUuid, String prompt, String response) {
        return db.logChat(chatUuid, prompt, response);
    }

    @Override
    public boolean deleteHistory(String chatUuid) {
        return db.deleteChatHistory(chatUuid);
    }
}
