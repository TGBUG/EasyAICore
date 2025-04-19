对接手册：

获取服务实例：

```java
AIService ai = Bukkit.getServicesManager().load(AIService.class);
```

对话:
```java
ai.chat(chatUuid, prompt, maxTokens, temperature)
        .thenAccept(ans -> send(s, responsePrefix + ans))
        .exceptionally(ex -> { send(s,"§cAI 错误: "+ex.getMessage()); return null; });
```

获取对话历史:
```java
List<Map<String, String>> history = ai.getHistory("chatUuid");
```

记录对话:
```java
boolean success = ai.logChat(
    "chatUuid",
    "How to make obsidian?", 
    "You need water and lava source blocks..."
);
```

删除历史对话:
```java
if (ai.deleteHistory("chatUuid")) {
    
}
```
