package com.posementor.posementorbackend.service;

import jakarta.annotation.PostConstruct;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Service // 이 클래스는 스프링의 서비스 컴포넌트로 등록됨 (비즈니스 로직 담당)
public class PoseService {

    // application.properties에서 RapidAPI 관련 설정값들을 주입받음
    @Value("${poseapi.rapidapi.url}")
    private String poseApiUrl;

    @Value("${poseapi.rapidapi.key}")
    private String apiKey;

    @Value("${poseapi.rapidapi.host}")
    private String apiHost;

    // 서비스가 시작될 때 한 번 실행됨 (디버깅용 로그 출력)
    @PostConstruct
    public void init() {
        System.out.println("✅ PoseService 로드됨");
        System.out.println("🌐 API URL: [" + poseApiUrl + "]");
    }

    /**
     * 대표 프레임 3장을 선택해서 Pose Recognition API로 전송하고 결과(JSON)를 받아오는 메서드
     *
     * @param allFrames 전체 프레임 이미지 리스트
     * @return 각 대표 프레임의 포즈 분석 결과(JSON 문자열) 리스트
     */
    public List<String> analyzeSelectedFrames(List<File> allFrames) throws Exception {
        int size = allFrames.size();

        // 프레임 수가 3개 미만이면 분석 불가능 → 예외 발생
        if (size < 3) throw new IllegalArgumentException("프레임 수가 3개 이상이어야 합니다.");

        // 대표 프레임 3개 선정 (처음, 중간, 마지막 프레임)
        List<File> selectedFrames = List.of(
                allFrames.get(0),               // 첫 프레임 (예: 어드레스 자세)
                allFrames.get(size / 2),        // 중간 프레임 (예: 임팩트 또는 스윙)
                allFrames.get(size - 1)         // 마지막 프레임 (예: 피니시 자세)
        );

        // HTTP 요청을 위한 클라이언트 객체 생성
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // 각 대표 프레임을 RapidAPI에 업로드하고 응답을 JSON 문자열로 받음
            return selectedFrames.stream().map(frame -> {
                try {
                    // HTTP POST 요청 생성
                    HttpPost uploadRequest = new HttpPost(poseApiUrl);

                    // RapidAPI 인증 헤더 추가
                    uploadRequest.setHeader("x-rapidapi-host", apiHost);
                    uploadRequest.setHeader("x-rapidapi-key", apiKey);

                    // multipart/form-data 형식으로 이미지 파일 첨부
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.addBinaryBody(
                            "image", // RapidAPI 측에서 요구하는 파라미터 이름 (고정)
                            Files.readAllBytes(frame.toPath()), // 이미지 파일 바이트 배열
                            ContentType.DEFAULT_BINARY,         // 일반 바이너리 타입
                            frame.getName()                     // 원본 파일명
                    );

                    // 요청에 multipart 엔티티 설정
                    uploadRequest.setEntity(builder.build());

                    // HTTP 요청 실행 및 응답 수신
                    HttpResponse response = httpClient.execute(uploadRequest);

                    // 응답 본문(JSON 문자열)을 UTF-8로 파싱하여 반환
                    return EntityUtils.toString(response.getEntity(), "UTF-8");

                } catch (Exception e) {
                    // 오류 발생 시 JSON 형식으로 에러 메시지 반환
                    return "{\"error\":\"" + e.getMessage() + "\"}";
                }
            }).toList(); // 결과를 List<String> 형태로 수집
        }
    }
}
