package com.realcraft.platform.llm.dto;

import java.util.List;

/**
 * OpenAI 兼容 Chat Completions 的请求/响应 DTO。
 */
public final class ChatCompletionDtos {

    private ChatCompletionDtos() {
    }

    public record ChatCompletionRequest(String model, List<Message> messages) {
    }

    public record Message(String role, Object content) {
    }

    public record ImageContent(String type, ImageUrl imageUrl) {
    }

    public record ImageUrl(String url) {
    }

    public record ChatCompletionResponse(List<Choice> choices) {
    }

    public record Choice(ChatMessage message) {
    }

    public record ChatMessage(String content) {
    }
}