package com.posementor.posementorbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * GPTService 클래스는 OpenAI의 ChatGPT API를 호출하여
 * 관절 좌표(keypoints) 데이터를 기반으로 자세 피드백을 생성하는 역할을 합니다.
 */
@Service
public class GPTService {

    // application.properties에서 설정값 주입 받음
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    // Jackson의 ObjectMapper는 자바 객체 ↔ JSON 변환을 도와주는 유틸리티
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 관절 좌표 JSON 데이터를 GPT에게 보내고,
     * 분석 피드백을 받아 문자열로 반환하는 메서드
     *
     * @param keypointsJson Pose Estimation API로부터 받은 관절 좌표 데이터(JSON 문자열)
     * @return GPT가 생성한 자세 피드백 문자열
     * @throws Exception API 요청 중 문제가 발생한 경우
     */
    public String getPoseFeedback(String keypointsJson) throws Exception {
        // 1. 요청 본문 구성 (ChatGPT API 명세에 맞게 작성)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");  // 사용할 GPT 모델 지정

        // GPT에 보낼 메시지 목록 구성
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are a fitness coach who provides posture feedback."));
        messages.add(Map.of("role", "user", "content", "Give feedback on this pose keypoints:\n" + keypointsJson));

        requestBody.put("messages", messages);

        // 2. 자바 객체를 JSON 문자열로 변환
        String json = objectMapper.writeValueAsString(requestBody);

        // 3. HttpClient 객체 생성
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // 4. POST 요청 객체 생성
            HttpPost httpPost = new HttpPost(apiUrl);

            // 5. 헤더 설정 (Authorization, Content-Type 등)
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            // 6. 요청 본문(body) 설정
            httpPost.setEntity(new StringEntity(json));

            // 7. 요청 실행 → 응답 수신
            HttpResponse response = httpClient.execute(httpPost);

            // 8. 응답 내용을 문자열로 변환
            String responseBody = EntityUtils.toString(response.getEntity());

            // 9. 응답 JSON 파싱
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            // 10. choices[0].message.content 를 꺼내서 반환
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        // 오류 없이도 GPT 응답이 비었을 경우
        return "No feedback generated.";
    }
}
