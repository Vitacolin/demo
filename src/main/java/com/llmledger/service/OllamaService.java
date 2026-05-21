package com.llmledger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmledger.dto.TransactionCreateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    @Value("${app.ollama.api-url}")
    private String ollamaApiUrl;

    @Value("${app.ollama.default-model}")
    private String defaultModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TransactionCreateRequest> parseTransactions(String text, String modelName) {
        String prompt = "你是一个专业的智能记账助手。请从输入中提取所有记账条目，严格输出 JSON 对象，格式为{\"transactions\":[{\"amount\":数字,\"description\":\"描述\",\"type\":\"income或expense\",\"category\":\"分类\"}]}。输入: \"" + text + "\"";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName == null || modelName.isBlank() ? defaultModel : modelName);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("format", "json");

            String res = RestClient.create().post()
                    .uri(ollamaApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(res);
            String raw = root.path("response").asText("").trim();
            if (raw.startsWith("```json")) raw = raw.substring(7).trim();
            if (raw.startsWith("```")) raw = raw.substring(3).trim();
            if (raw.endsWith("```")) raw = raw.substring(0, raw.length() - 3).trim();

            JsonNode parsed = objectMapper.readTree(raw);
            JsonNode txNode = parsed.path("transactions");
            if (!txNode.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(txNode, new TypeReference<List<TransactionCreateRequest>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public String generateAdvice(String prompt, String modelName) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName == null || modelName.isBlank() ? defaultModel : modelName);
            body.put("prompt", prompt);
            body.put("stream", false);

            String res = RestClient.create().post()
                    .uri(ollamaApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(res);
            return root.path("response").asText("暂时无法生成结果").trim();
        } catch (Exception e) {
            return "诊断失败，请检查本地 Ollama 服务是否启动。错误信息：" + e.getMessage();
        }
    }
}
