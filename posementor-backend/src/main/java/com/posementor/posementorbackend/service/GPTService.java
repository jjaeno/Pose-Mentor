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
import com.posementor.posementorbackend.util.PoseJsonCompressor;

import jakarta.annotation.PostConstruct;

import java.util.*;

/**
 * GPTService 클래스는 OpenAI의 ChatGPT API를 호출하여
 * 관절 좌표(keypoints) 데이터를 기반으로 자세 피드백을 생성하는 역할을 함함
 */
@Service
public class GPTService {
    private static final String COMMON_STRING = """
            You are a Ph.D.-level sports medicine and rehabilitation specialist with 10 years of performance-coaching experience.

            ◎ Workflow
            1. **First**, you will receive *exerciseType* and the block of **“ideal posture checkpoints”** for that exercise.
            2. Next, you will receive the actual joint-coordinate data (JSON).
            3. Compare the coordinates with the checkpoints, diagnose any issues, and write corrective feedback.

            ◎ Feedback guidelines
            • Output **4~5 sentences in Korean**.
              ① Diagnose the problem → ② Provide specific corrective advice.
            • **Never reveal raw numbers** (pixels, coordinates, angles).
            • Use **1~2 discipline-appropriate technical terms** for professionalism.
            • Insert line breaks where appropriate to improve readability.

            ◎ Internal decision rules *(do NOT expose to user)*
            • Shoulder Δy > 5 → shoulder tilt
            • Knee Δx > 20 → knee misalignment
            • Wrist y 30 lower than shoulder → wrists too low
            • Ankle Δy > 10 → foot imbalance

            Below, the inputs will follow in this order:
            ① *exerciseType* & its ideal-posture checkpoints
            ② The actual joint-coordinate JSON.
            Begin your analysis now, and **return your feedback in Korean**.
            """;
    private static final Map<String, String> CANONICAL = Map.of(
            "GOLF", """
                    GOLF – Swing Address (Driver)
                    • Shoulders: perfectly level; chest opened slightly toward 1 o’clock (RH golfer).
                    • Spine & pelvis: hip hinge forward without lumbar hyper-extension; maintain natural spine curve.
                    • Knees: slight flexion, feet shoulder-width; equal gap left/right.
                    • Arms: maintain triangle with neutral wrists; keep space between clubhead and navel.
                    • Weight: 55 % on lead (left) foot, 45 % on trail (right) foot; light adductor tension in both legs.
                    """,
            "BASEBALL", """
                    BASEBALL – Right-handed Batting Stance
                    • Feet: shoulder-width; front foot 11 o’clock, back foot 1 o’clock.
                    • Knees: soft flexion for elasticity; 60 % weight on rear foot, 40 % on front.
                    • Hips & spine: hip-hinge forward, spine neutral.
                    • Hands/Bat: hands at shoulder height; bat head tilted slightly backward.
                    • Gaze: both eyes squarely on the pitcher.
                    """,
            "BOWLING", """
                    BOWLING – Release Position
                    • Shoulders/Arm: shoulders stay closed as the ball passes the ankle.
                    • Spine: upper and lower body form a straight line at 15–20° forward tilt.
                    • Lead foot: sliding heel up, weight on forefoot; knee softly flexed.
                    • Trail leg: extended diagonally on the floor for balance.
                    • Gaze: fixed on the release target (break-point).
                    """,
            "SOCCER",
            """
                    SOCCER – Instep Drive (Right-Footed)
                    • Feet: plant (left) foot turned ~90° to the target, 15–20 cm beside the ball; kicking foot drawn straight back with locked ankle.
                    • Knees: plant knee slightly flexed and stacked over the ball; kicking knee drives forward and up through impact.
                    • Hips & torso: explosive hip rotation through the strike; torso leans slightly over the ball to keep the shot low.
                    • Arms: opposite arm extended sideways for counter-balance; kicking-side arm relaxed behind the body.
                    • Gaze: eyes fixed on the ball’s center until contact, then lift toward the target.
                    """,
            "BILLIARDS", """
                    BILLIARDS – Cue-Stroke Address
                    • Feet: lead foot 45°, trail foot 90°, creating a stable triangle; slightly wider than shoulders.
                    • Knees & hips: lead knee softly flexed to lower the center of gravity.
                    • Spine: torso nearly parallel to the table; avoid lumbar hyper-flexion.
                    • Arms: bridge hand fixed; only the cueing elbow and forearm move freely.
                    • Gaze: align cue tip, object ball, and pocket on a straight line.
                    """,
            "BASKETBALL", """
                    BASKETBALL – Set Position for Straight-On Jump Shot
                    • Feet: shoulder-width; toes angled slightly toward the rim (11 o’clock–1 o’clock).
                    • Knees: light flexion for spring; knees vertically over toes.
                    • Hips & spine: slight hip hinge with torso leaning marginally toward the rim.
                    • Arms: ball in front of forehead; elbows vertical; wrists ready to snap.
                    • Gaze: fix on the rim or upper backboard until release.
                    """

    );

    // application.properties에서 설정값 주입 받음
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @PostConstruct
    public void init() {
        System.out.println("GPTService loaded");
    }

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
        System.out.println("GPT method call complete");
        // 요청 본문 구성 (ChatGPT API 명세에 맞게 작성)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4.1"); // 사용할 GPT 모델 지정
        // 관절 좌표 요약
        List<String> kp = PoseJsonCompressor.compress(keypointsJsonList);
        System.out.println("Compressor Result :" + kp);

        // GPT에 보낼 프롬프트 구성 -> 추후에 사용자가 입력한 운동 종류를 프롬포트에 동적으로 할당해 더욱 정교한 피드백 생성성
        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(COMMON_STRING);
        String cp = CANONICAL.getOrDefault(exerciseType.toUpperCase(), "");
        if (!cp.isEmpty())
            fullPrompt.append("\n").append(cp);
        fullPrompt.append("\nExercise Type: ").append(exerciseType);
        fullPrompt.append("\njoint keypoints: ");
        for (String json : kp) {
            fullPrompt.append("\n").append(json);
        }
        System.out.println("Full frompt : " + fullPrompt);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", fullPrompt.toString()));

        requestBody.put("messages", messages);

        // 자바 객체를 JSON 문자열로 변환
        String json = objectMapper.writeValueAsString(requestBody);

        // HttpClient 객체 생성
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // POST 요청 객체 생성
            HttpPost httpPost = new HttpPost(apiUrl);

            // 헤더 설정 (Authorization, Content-Type 등)
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            // 요청 본문(body) 설정
            httpPost.setEntity(new StringEntity(json, "UTF-8"));

            // 요청 실행 → 응답 수신
            HttpResponse response = httpClient.execute(httpPost);

            // 응답 내용을 문자열로 변환
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("GPT respone : " + responseBody);
            // 응답 JSON 파싱
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            // choices[0].message.content 를 꺼내서 반환
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

        } catch (Exception e) {
            System.out.println("GPT call Fail" + e.getMessage());
            e.printStackTrace();
        }

        // 오류 없이도 GPT 응답이 비었을 경우
        return "No feedback generated.";
    }
}
