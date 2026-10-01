package com.example.aiapp.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.ResponseFormatJsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thin wrapper around the OpenAI chat completions API, covering the three call
 * shapes the QA graph and EMS AI assistant need: a plain text answer, a JSON-only
 * answer (used for the classifier and the natural-language search endpoint), and
 * a tool-calling turn.
 */
@Component
public class ChatCompletionClient {

    private final OpenAIClient client;
    private final String model;

    public ChatCompletionClient(OpenAIClient client, @Value("${app.openai.chat-model}") String model) {
        this.client = client;
        this.model = model;
    }

    public String modelName() {
        return model;
    }

    public String complete(String systemPrompt, String userPrompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .build();
        return firstMessageText(client.chat().completions().create(params));
    }

    /** Same as {@link #complete}, but forces the response to be a single JSON object. */
    public String completeJson(String systemPrompt, String userPrompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .responseFormat(ResponseFormatJsonObject.builder().build())
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .build();
        return firstMessageText(client.chat().completions().create(params));
    }

    /**
     * Single-turn tool-calling completion: if the model requests one or more tool calls
     * they're all returned (the caller executes them); otherwise the model's own direct
     * text answer is returned. Mirrors qa_graph/nodes.py's tool_node, which never sends
     * tool results back for a second model turn either.
     */
    public ToolTurnResult completeWithTools(String systemPrompt, String userPrompt, List<ChatCompletionTool> tools) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt);
        tools.forEach(builder::addTool);

        ChatCompletion completion = client.chat().completions().create(builder.build());
        var message = completion.choices().get(0).message();

        List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().orElse(List.of());
        if (!toolCalls.isEmpty()) {
            List<ToolCall> calls = toolCalls.stream()
                    .map(tc -> tc.asFunction().function())
                    .map(fn -> new ToolCall(fn.name(), fn.arguments()))
                    .toList();
            return new ToolTurnResult(calls, null);
        }
        return new ToolTurnResult(List.of(), message.content().orElse(""));
    }

    private String firstMessageText(ChatCompletion completion) {
        return completion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse("");
    }

    public record ToolCall(String name, String argumentsJson) {
    }

    public record ToolTurnResult(List<ToolCall> toolCalls, String directAnswer) {
    }
}
