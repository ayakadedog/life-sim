package com.example.demo.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeepSeek API 服务
 * 负责与 DeepSeek 大模型进行交互，处理 API 调用、重试机制和 JSON 响应解析
 */
@Service
public class DeepSeekService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private final HttpClient httpClient;
    private static final int MAX_RETRIES = 3;

    public DeepSeekService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 调用 DeepSeek API
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return API 响应内容
     */
    public String call(String systemPrompt, String userPrompt) {
        return callWithRetry(systemPrompt, userPrompt, null);
    }
    
    /**
     * 调用 DeepSeek API 并期望返回 JSON 格式
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return JSON 格式的响应内容
     */
    public String callExpectingJson(String systemPrompt, String userPrompt) {
        return callWithRetry(systemPrompt, userPrompt, (content) -> {
            try {
                String clean = cleanContent(content);
                if (clean.startsWith("[")) {
                    new JSONArray(clean);
                } else if (clean.startsWith("{")) {
                    new JSONObject(clean);
                } else {
                    return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * 带重试机制的 API 调用
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param validator 响应验证器
     * @return API 响应内容
     */
    private String callWithRetry(String systemPrompt, String userPrompt, java.util.function.Predicate<String> validator) {
        int attempts = 0;
        String lastError = "";
        
        while (attempts < MAX_RETRIES) {
            attempts++;
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "deepseek-chat");
                requestBody.put("stream", false);
                // Optional: Enable JSON mode if API supports it (DeepSeek V3 does)
                // requestBody.put("response_format", new JSONObject().put("type", "json_object")); 

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
                messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
                // Add a nudge for retry if it's not the first attempt
                if (attempts > 1) {
                     messages.put(new JSONObject().put("role", "user").put("content", "Previous response was invalid. Please strictly follow the JSON format requirements."));
                }
                requestBody.put("messages", messages);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    String content = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                    
                    String cleaned = cleanContent(content);
                    
                    // Validate if validator is present
                    if (validator != null && !validator.test(cleaned)) {
                        System.out.println("DeepSeekService: JSON validation failed. Retrying...");
                        lastError = "JSON validation failed";
                        TimeUnit.SECONDS.sleep((long) Math.pow(2, attempts));
                        continue;
                    }
                    
                    return cleaned;
                } else if (response.statusCode() == 429 || response.statusCode() >= 500) {
                     System.out.println("DeepSeekService: Error " + response.statusCode() + ". Retrying...");
                     lastError = "HTTP " + response.statusCode();
                     TimeUnit.SECONDS.sleep((long) Math.pow(2, attempts)); 
                } else {
                    return "Error calling DeepSeek API: " + response.statusCode() + " - " + response.body();
                }
            } catch (Exception e) {
                System.out.println("DeepSeekService: Exception " + e.getMessage() + ". Retrying...");
                lastError = e.getMessage();
                try {
                    TimeUnit.SECONDS.sleep((long) Math.pow(2, attempts));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "Interrupted during retry";
                }
            }
        }
        return "Failed after " + MAX_RETRIES + " attempts. Last error: " + lastError;
    }
    
    /**
     * 清理响应内容，去除 Markdown 代码块标记
     * @param content 原始内容
     * @return 清理后的内容
     */
    private String cleanContent(String content) {
        if (content == null) return "";
        Pattern pattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content.trim();
    }
}
