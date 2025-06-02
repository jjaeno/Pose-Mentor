package com.posementor.posementorbackend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;

/**
 * PoseJsonCompressor (리스트<JSON 문자열> 받아서 프레임별로 정제 후 리스트<String> 반환)
 * ───────────────────────────────────────────────
 *  • 불필요 관절 제거 (CORE_PARTS만 유지)
 *  • 각 좌표(x, y) 및 score 값을 소수점 1자리로 반올림
 *  • 프레임별 JSON 객체를 String으로 반환하여 List<String> 형태로 제공
 */
public final class PoseJsonCompressor {

    // 유지할 핵심 관절 목록 (12개)
    private static final Set<String> CORE_PARTS = Set.of(
            "leftShoulder",  "rightShoulder",
            "leftElbow",     "rightElbow",
            "leftWrist",     "rightWrist",
            "leftHip",       "rightHip",
            "leftKnee",      "rightKnee",
            "leftAnkle",     "rightAnkle"
    );

    private PoseJsonCompressor() {} // static-only class

    /**
     * @param rawJsonList 프레임별 원본 JSON 문자열 리스트
     * @return 각 프레임에 대해 정제된 JSON 문자열 리스트
     * @throws JsonProcessingException 파싱 실패 시
     */
    public static List<String> compress(List<String> rawJsonList) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<String> result = new ArrayList<>();

        for (String rawJson : rawJsonList) {
            JsonNode node = mapper.readTree(rawJson);
            ObjectNode rewritten = rewriteFrame(node, mapper);
            result.add(mapper.writeValueAsString(rewritten));
        }

        return result;
    }

    /**
     * 프레임 1개의 keypoints 및 score 필드를 정제한 결과 반환
     */
    private static ObjectNode rewriteFrame(JsonNode frameNode, ObjectMapper mapper) {
        ObjectNode newFrame = frameNode.deepCopy();

        // (1) 전체 프레임 score 반올림
        if (newFrame.has("score")) {
            newFrame.put("score", round1(newFrame.get("score").asDouble()));
        }

        // (2) keypoints 필터링 + 정제
        ArrayNode newKeypoints = mapper.createArrayNode();
        frameNode.withArray("keypoints").forEach(kpOrig -> {
            String part = kpOrig.get("part").asText();
            if (CORE_PARTS.contains(part)) {
                ObjectNode kp = kpOrig.deepCopy();

                // score 반올림
                kp.put("score", round1(kp.get("score").asDouble()));

                // 좌표 반올림
                ObjectNode pos = (ObjectNode) kp.get("position");
                pos.put("x", round1(pos.get("x").asDouble()));
                pos.put("y", round1(pos.get("y").asDouble()));

                newKeypoints.add(kp);
            }
        });

        newFrame.set("keypoints", newKeypoints);
        return newFrame;
    }

    /**
     * 소수점 첫째 자리로 반올림
     */
    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
