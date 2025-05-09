package com.posementor.posementorbackend.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service // Spring이 관리하는 서비스 클래스
public class FrameExtractorService {

    /**
     * 동영상에서 0.25초 간격으로 프레임을 추출하여 이미지 파일로 저장함.
     *
     * @param videoFile     분석 대상 동영상 파일 (.mp4 등)
     * @param outputDirPath 추출된 프레임 이미지를 저장할 디렉토리 경로
     * @return 추출된 이미지 파일들의 리스트
     */
    public List<File> extractFramesEveryHalfSecond(File videoFile, String outputDirPath) throws Exception {
        List<File> frameFiles = new ArrayList<>(); // 반환할 이미지 파일 리스트

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start(); // 영상 파일 열기

            double fps = grabber.getFrameRate(); // 영상의 초당 프레임 수 (frames per second)

            // FPS가 0 이하인 경우, 영상 자체가 잘못되었을 수 있음
            if (fps <= 0) {
                throw new IllegalArgumentException("⚠️ FPS 정보가 올바르지 않습니다. 영상이 손상됐거나 잘못된 형식일 수 있습니다.");
            }

            // 추출 간격 설정: 0.25초마다 1장 추출 → interval = fps * 0.25
            int intervalFrames = (int) (fps * 0.25); // 0.25초 간격 프레임 수
            if (intervalFrames == 0) intervalFrames = 1; // 보호코드: 최소 1장씩 추출하도록 보정

            Frame frame; // 추출할 단일 프레임 객체
            Java2DFrameConverter converter = new Java2DFrameConverter(); // 프레임 → 이미지 변환기
            int currentFrameIndex = 0;     // 전체 프레임 인덱스
            int extractedCount = 0;        // 저장된 프레임 수

            // 출력 디렉토리 생성 (없으면 새로 만듦)
            File outputDir = new File(outputDirPath);
            if (!outputDir.exists()) outputDir.mkdirs();

            // 영상에서 프레임 하나씩 반복 추출
            while ((frame = grabber.grabImage()) != null) {
                // 현재 프레임 인덱스가 추출 간격에 해당할 때만 저장
                if (currentFrameIndex % intervalFrames == 0) {
                    BufferedImage bufferedImage = converter.convert(frame); // 프레임 → 이미지 변환
                    File outputFile = new File(outputDir, "frame_" + extractedCount + ".jpg"); // 저장 파일명 생성
                    ImageIO.write(bufferedImage, "jpg", outputFile); // 이미지 저장
                    frameFiles.add(outputFile); // 리스트에 추가
                    extractedCount++;
                }
                currentFrameIndex++; // 다음 프레임으로 이동
            }

            grabber.stop(); // 영상 읽기 종료
        }

        return frameFiles; // 추출된 이미지 파일 리스트 반환
    }
}
