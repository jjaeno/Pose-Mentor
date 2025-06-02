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
import com.posementor.posementorbackend.util.PoseJsonCompressor;;;
import java.util.*;

/**
 * GPTService 클래스는 OpenAI의 ChatGPT API를 호출하여
 * 관절 좌표(keypoints) 데이터를 기반으로 자세 피드백을 생성하는 역할을 함함
 */
@Service
public class GPTService {
    String promptHeader = """
You are a professional posture analysis AI.

You will receive joint coordinate data from a workout pose. Analyze this data using simple math (differences in x and y values) to detect any imbalance, misalignment, or asymmetry in the user's posture.

Rules:
- If the left shoulder y-value is more than 5 pixels higher or lower than the right shoulder, mention shoulder tilt.
- If the left and right knee x-values differ by more than 20 pixels, mention knee misalignment.
- If the wrist y-values are significantly lower than the shoulders (more than 30 pixels), mention that the arms are too low.
- If the ankle y-values differ by more than 10 pixels, mention foot imbalance.

Provide feedback based only on the actual coordinates.

Return your analysis in Korean. Keep the feedback short and specific (2~3 sentences).

Now analyze the following:
""";

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
    public String getPoseFeedback(List<String> keypointsJsonList, String exerciseType) throws Exception {
        // 요청 본문 구성 (ChatGPT API 명세에 맞게 작성)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");  // 사용할 GPT 모델 지정
        //관절 좌표 요약약
        List<String> kp = PoseJsonCompressor.compress(keypointsJsonList);

        // GPT에 보낼 프롬프트 구성 -> 추후에 사용자가 입력한 운동 종류를 프롬포트에 동적으로 할당해 더욱 정교한 피드백 생성성
        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(promptHeader);
        fullPrompt.append("\n운동 종류: ").append(exerciseType);
        fullPrompt.append("\n관절 좌표들: ");
        for(String json : kp) { 
            fullPrompt.append("\n").append(json);
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", fullPrompt.toString()));

        requestBody.put("messages", messages);

        //자바 객체를 JSON 문자열로 변환
        String json = objectMapper.writeValueAsString(requestBody);

        //HttpClient 객체 생성
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            //POST 요청 객체 생성
            HttpPost httpPost = new HttpPost(apiUrl);

            //헤더 설정 (Authorization, Content-Type 등)
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            //요청 본문(body) 설정
            httpPost.setEntity(new StringEntity(json, "UTF-8"));

            //요청 실행 → 응답 수신
            HttpResponse response = httpClient.execute(httpPost);

            //응답 내용을 문자열로 변환
            String responseBody = EntityUtils.toString(response.getEntity());

            //응답 JSON 파싱
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            //choices[0].message.content 를 꺼내서 반환
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
            System.out.println("GPT 응답 원문 : " + responseBody);
        }
        
        // 오류 없이도 GPT 응답이 비었을 경우
        return "No feedback generated.";
    }
}
