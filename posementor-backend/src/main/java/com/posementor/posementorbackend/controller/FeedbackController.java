package com.posementor.posementorbackend.controller;

import com.posementor.posementorbackend.service.GPTService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 이 컨트롤러는 클라이언트로부터 관절 좌표 데이터를 받아서
 * GPTService를 통해 자세 피드백을 생성하고 반환하는 역할을 합니다.
 */
@RestController // 이 클래스는 REST API 요청을 처리하는 컨트롤러임을 나타냄
@RequestMapping("/api/feedback") // 이 컨트롤러의 기본 URL 경로: /api/feedback
public class FeedbackController {

    // GPTService를 주입받음 (Spring이 자동으로 생성자 주입)
    private final GPTService gptService;

    public FeedbackController(GPTService gptService) {
        this.gptService = gptService;
    }

    /**
     * POST 방식으로 JSON 형식의 keypoints 데이터를 받아 GPT에 전달하고,
     * 생성된 자세 피드백을 반환하는 API
     *
     * @param keypointsJson 프론트엔드나 외부에서 전달된 관절 좌표 JSON (문자열 형식)
     * @return GPT가 생성한 자세 피드백 (문자열)
     */
    @PostMapping // POST /api/feedback 요청을 처리함
    public ResponseEntity<String> generateFeedback(@RequestBody String keypointsJson) {
        try {
            // GPTService를 호출하여 자세 피드백 생성
            String feedback = gptService.getPoseFeedback(keypointsJson);

            // 성공 응답 (HTTP 200 OK)
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            // 예외 발생 시 HTTP 500 응답 반환
            return ResponseEntity.status(500).body("❌ 피드백 생성 실패: " + e.getMessage());
        }
    }
}
