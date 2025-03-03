package com.haneolj.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haneolj.portfolio.service.StudyService;
import com.haneolj.portfolio.service.WebhookService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final StudyService studyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    @Value("${obsidian.repo.branch}")
    private String targetBranch;

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestBody byte[] payload,
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        // 시그니처 검증
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            if (signature == null || !verifySignature(payload, signature)) {
                log.warn("웹훅 시그니처 검증 실패");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
        } else {
            log.warn("웹훅 시크릿이 설정되지 않았습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret not configured");
        }

        // 이벤트 타입 확인 (push 이벤트만 처리)
        if (!"push".equals(event)) {
            log.info("Push 이벤트가 아니므로 무시: {}", event);
            return ResponseEntity.ok("Event ignored");
        }

        // 대상 브랜치 확인
        String ref = extractRefFromPayload(payload);
        if (ref == null || !ref.equals("refs/heads/" + targetBranch)) {
            log.info("대상 브랜치({})가 아니므로 무시: {}", targetBranch, ref);
            return ResponseEntity.ok("Branch ignored");
        }

        // 변경된 파일 목록 추출
        List<String> changedFiles = webhookService.extractChangedFilesFromPayload(payload);
        log.info("변경된 파일 목록: {}", changedFiles);

        // 스터디 구조 새로고침
        if (!changedFiles.isEmpty()) {
            // 변경된 파일이 있다면 선택적 처리
            webhookService.processChangedFiles(changedFiles);
        } else {
            // 변경된 파일 정보가 없는 경우 전체 새로고침 (기존 방식)
            log.info("변경된 파일 정보 없음, 전체 새로고침 시작");
            studyService.refreshStudyStructure();
        }

        return ResponseEntity.ok("Successfully processed");
    }

    // GitHub 웹훅 시그니처 검증
    private boolean verifySignature(byte[] payload, String signature) {
        try {
            String calculatedSignature = "sha256=" + calculateHmacSha256(payload, webhookSecret);
            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("시그니처 검증 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    // HMAC-SHA256 해시 계산
    private String calculateHmacSha256(byte[] data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data);

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Jackson을 사용하여 페이로드에서 'ref' 값 추출
    private String extractRefFromPayload(byte[] payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            return jsonNode.path("ref").asText(null);
        } catch (Exception e) {
            log.error("페이로드 파싱 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }
}