package com.posementor.posementorbackend.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.posementor.posementorbackend.service.PoseService;
import com.posementor.posementorbackend.service.FrameExtractorService;
import com.posementor.posementorbackend.service.GPTService;
import com.posementor.posementorbackend.util.PoseJsonCompressor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.File;

@RestController
@RequestMapping("/api") // 기본 요청 경로가 /api
public class PoseAPIController {

    @Autowired
    private PoseService poseService; // 포즈 분석 서비스
    @Autowired
    private FrameExtractorService frameExtractorService; // 프레임 추출 서비스
    @Autowired
    private GPTService gptService;
    @PostConstruct
    public void check() {
        // poseService 주입이 제대로 되었는지 확인 로그
        System.out.println("poseService state in PoseAPIController " + poseService);
    }

    @PostConstruct
    public void init() {
        System.out.println("PoseAPIController load complete");
    }

    // API 연결 테스트용 엔드포인트 (GET /api/test)
    @GetMapping("/test")
    public String test() {
        return "서버 연결 OK!";
    }

    /**
     * [POST] /api/analyze
     * 사용자가 업로드한 동영상 파일을 받아:
     * 1. 로컬에 저장
     * 2. 프레임을 일정 간격으로 추출
     * 3. 대표 프레임 3장을 선택해 Pose API로 분석 요청
     * 4. 분석 결과(JSON 문자열)를 리스트로 반환
     */

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeVideo(@RequestParam("file") MultipartFile file, @RequestParam("exerciseType") String exerciseType) {
        //업로드 파일이 없으면 오류 응답
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("업로드된 파일이 없습니다.");
        }

        try {
            //업로드된 영상 파일을 서버에 저장
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists())
                dir.mkdirs(); // 저장 디렉토리가 없으면 생성

            //랜덤 파일명 생성 (UUID) → 중복 방지
            String fileName = UUID.randomUUID().toString() + ".mp4";
            File savedFile = new File(uploadDir + fileName);
            file.transferTo(savedFile); // MultipartFile을 실제 파일로 저장
            System.out.println("File create complete");

            //영상에서 프레임 추출(0.25초 간격) -> frameExtractorService.java 이용
            String frameDir = System.getProperty("user.dir") + "/frames/" + System.currentTimeMillis(); // 프레임 저장 디렉토리
            List<File> frames = frameExtractorService.extractFramesEveryHalfSecond(savedFile, frameDir);
            //frmaes가 존재하면 -> 로그 출력
            if (frames != null && !frames.isEmpty()) {
            System.out.println("Frame extract Success");
            }
            else System.out.println("Frame extract Fail");
            //대표 프레임 3장 선택하고 pose api에 전송 후 결과 받음음 -> PoseService.java 이용
            List<String> keypoints = poseService.analyzeSelectedFrames(frames);
            if (keypoints != null && !keypoints.isEmpty()) {
                System.out.println("Keypoints Create success");
            }
            else System.out.println("Keypoints Create Fail");
            String feedback = gptService.getPoseFeedback(keypoints, exerciseType);
            if (feedback != null && !feedback.isEmpty()) {
                System.out.println("feedback Create success");
            }
            else System.out.println("feedback Create Fail");
            //클라이언트에 분석 결과 반환
            return ResponseEntity.ok().body(feedback);

        } catch (Exception e) {
            //예외 발생 시 에러 메시지를 리스트로 감싸 반환
            return ResponseEntity.internalServerError().body("파일 처리 중 오류 발생생");
        }
    }
}
