package com.posementor.posementorbackend.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.posementor.posementorbackend.service.PoseService;
import com.posementor.posementorbackend.service.FrameExtractorService;

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

    @PostConstruct
    public void check() {
        // poseService 주입이 제대로 되었는지 확인 로그
        System.out.println("PoseAPIController 안에서 poseService 주입 상태: " + poseService);
    }

    @PostConstruct
    public void init() {
        System.out.println("PoseAPIController 로드 완료!");
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
    public ResponseEntity<List<String>> analyzeVideo(@RequestParam("file") MultipartFile file) {
        //업로드 파일이 없으면 오류 응답
        if (file.isEmpty()) {
            List<String> error = new ArrayList<>();
            error.add("⚠️ 업로드된 파일이 없습니다.");
            return ResponseEntity.badRequest().body(error);
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

            //영상에서 프레임 추출(0.25초 간격) -> frameExtractorService.java 이용
            String frameDir = System.getProperty("user.dir") + "/frames/" + System.currentTimeMillis(); // 프레임 저장 디렉토리
            List<File> frames = frameExtractorService.extractFramesEveryHalfSecond(savedFile, frameDir);

            //대표 프레임 3장 선택하고 pose api에 전송 -> PoseService.java 이용
            List<String> results = poseService.analyzeSelectedFrames(frames);

            //클라이언트에 분석 결과(JSON 리스트) 반환
            return ResponseEntity.ok().body(results);

        } catch (Exception e) {
            //예외 발생 시 에러 메시지를 리스트로 감싸 반환
            List<String> error = new ArrayList<>();
            error.add("파일 처리 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
