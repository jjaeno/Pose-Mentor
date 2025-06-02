package com.posementor.posementorbackend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;

/**
 * PoseJsonCompressor
 * ──────────────────
 *  • 필요 없는 관절(keypoint) 삭제
 *  • 좌표 x, y  → 소수점 1자리로 반올림
 *  • score     → 소수점 1자리로 반올림 (프레임 전체·관절별 모두)
 *
 *  사용:
 *      String slim = PoseJsonCompressor.compress(rawJson,
 *                      PoseJsonCompressor.ExerciseType.GOLF);
 */
public final class PoseJsonCompressor {

    /* ────────────────────────────────────────────────────────────── *
     *  1) 운동 종목 정의
     * ────────────────────────────────────────────────────────────── */
    public enum ExerciseType {
        FITNESS, GOLF, BOWLING, BASEBALL, BILLIARDS, BASKETBALL
    }

    /* ────────────────────────────────────────────────────────────── *
     *  2) 공통으로 남길 12개 핵심 관절
     * ────────────────────────────────────────────────────────────── */
    private static final Set<String> CORE_PARTS = Set.of(
            "leftShoulder",  "rightShoulder",
            "leftElbow",     "rightElbow",
            "leftWrist",     "rightWrist",
            "leftHip",       "rightHip",
            "leftKnee",      "rightKnee",
            "leftAnkle",     "rightAnkle"
    );

    /* ────────────────────────────────────────────────────────────── *
     *  3) 종목별 관절 매핑 (필요 시 개별 튜닝 가능)
     * ────────────────────────────────────────────────────────────── */
    private static final Map<ExerciseType, Set<String>> RELEVANT_PARTS = Map.of(
            ExerciseType.FITNESS,    CORE_PARTS,
            ExerciseType.GOLF,       CORE_PARTS,
            ExerciseType.BOWLING,    CORE_PARTS,
            ExerciseType.BASEBALL,   CORE_PARTS,
            ExerciseType.BILLIARDS,  CORE_PARTS,
            ExerciseType.BASKETBALL, CORE_PARTS
    );

    /* ────────────────────────────────────────────────────────────── */
    private PoseJsonCompressor() {}   // 정적 유틸리티

    /* ────────────────────────────────────────────────────────────── *
     *  Public API
     * ────────────────────────────────────────────────────────────── */

    /**
     * @param rawJson   Pose API 응답(JSON 문자열 - 배열/객체 모두 OK)
     * @param exercise  운동 종목(enum)
     * @return          불필요 관절 제거 + 좌표·score 1자리 반올림 JSON
     */
    public static String compress(String rawJson, ExerciseType exercise)
            throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(rawJson);
        ArrayNode frames = mapper.createArrayNode();

        if (root.isArray())            // 다중 프레임
            root.forEach(node -> frames.add(rewriteFrame(node, exercise, mapper)));
        else                           // 단일 프레임
            frames.add(rewriteFrame(root, exercise, mapper));

        return mapper.writeValueAsString(frames);
    }

    /* ────────────────────────────────────────────────────────────── *
     *  내부 로직
     * ────────────────────────────────────────────────────────────── */

    /** frameNode 1개를 변환해 반환 */
    private static ObjectNode rewriteFrame(JsonNode frameNode,
                                           ExerciseType ex,
                                           ObjectMapper mapper) {

        Set<String> keep = RELEVANT_PARTS.getOrDefault(ex, CORE_PARTS);
        ObjectNode newFrame = frameNode.deepCopy();

        /* (A) 프레임 전체 score 반올림 */
        if (newFrame.has("score")) {
            newFrame.put("score", round1(newFrame.get("score").asDouble()));
        }

        /* (B) keypoints 배열 재구성 */
        ArrayNode newKeypoints = mapper.createArrayNode();

        frameNode.withArray("keypoints").forEach(kpOrig -> {
            String part = kpOrig.get("part").asText();
            if (keep.contains(part)) {
                ObjectNode kp = kpOrig.deepCopy();

                /* (B-1) 관절 score 반올림 */
                kp.put("score", round1(kp.get("score").asDouble()));

                /* (B-2) 좌표 반올림 */
                ObjectNode pos = (ObjectNode) kp.get("position");
                pos.put("x", round1(pos.get("x").asDouble()));
                pos.put("y", round1(pos.get("y").asDouble()));

                newKeypoints.add(kp);
            }
        });

        newFrame.set("keypoints", newKeypoints);
        return newFrame;
    }

    /** 소수 첫째 자리로 반올림 */
    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
