package com.community.smartelderlybackend.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private static final String API_KEY = "sk-e4cbf5bbf68f4ff8a4e469ed1971a9e5";

    // DeepSeek 官方接口地址
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    /**
     * 发送提示词给大模型，获取诊断报告
     */
    public String getRiskPrediction(String prompt) {
        try {
            // 1. 组装发给大模型的 JSON 数据包 (遵循 OpenAI 格式标准)
            JSONObject body = new JSONObject();
            body.set("model", "deepseek-chat"); // 使用 deepseek-chat 模型

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.set("role", "user");
            message.set("content", prompt);
            messages.add(message);

            body.set("messages", messages);

            // 2. 使用 Hutool 发起 HTTP POST 请求
            String result = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .execute().body();

            // 3. 解析大模型返回的一大坨 JSON，精准提取那段文字
            JSONObject resJson = JSONUtil.parseObj(result);

            // 防御性判断：如果余额不足或报错，返回提示
            if (resJson.containsKey("error")) {
                return "AI 诊断失败：" + resJson.getJSONObject("error").getStr("message");
            }

            String aiReport = resJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content");

            return aiReport;

        } catch (Exception e) {
            e.printStackTrace();
            return "AI 诊断接口调用异常，请检查网络或 API Key 配置。";
        }
    }
}