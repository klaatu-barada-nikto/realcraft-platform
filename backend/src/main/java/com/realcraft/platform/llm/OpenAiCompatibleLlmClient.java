package com.realcraft.platform.llm;

import com.realcraft.platform.common.BusinessException;
import com.realcraft.platform.common.ErrorCode;
import com.realcraft.platform.config.AppProperties;
import com.realcraft.platform.llm.dto.ChatCompletionDtos.ChatCompletionRequest;
import com.realcraft.platform.llm.dto.ChatCompletionDtos.ChatCompletionResponse;
import com.realcraft.platform.llm.dto.ChatCompletionDtos.ImageContent;
import com.realcraft.platform.llm.dto.ChatCompletionDtos.ImageUrl;
import com.realcraft.platform.llm.dto.ChatCompletionDtos.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 基于 RestClient 的 OpenAI 兼容 Vision 调用实现。
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final AppProperties props;
    private final RestClient restClient;

    public OpenAiCompatibleLlmClient(AppProperties props, RestClient restClient) {
        this.props = props;
        this.restClient = restClient;
    }

    @Override
    public String generate(byte[][] images) {
        List<ImageContent> parts = new ArrayList<>(images.length);
        for (byte[] image : images) {
            String encoded = Base64.getEncoder().encodeToString(image);
            parts.add(new ImageContent("image_url", new ImageUrl("data:image/jpeg;base64," + encoded)));
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                props.getAiModel(),
                List.of(
                        new Message("system", PromptProvider.systemPrompt()),
                        new Message("user", parts)
                )
        );

        String url = trimTrailingSlash(props.getAiApiUrl()) + "/chat/completions";

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + props.getAiApiKey())
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new BusinessException(ErrorCode.UPSTREAM_ERROR);
            }
            return response.choices().get(0).message().content();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UPSTREAM_ERROR);
        }
    }

    private String trimTrailingSlash(String url) {
        String result = url;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}