package com.haneolj.portfolio.controller;

import com.haneolj.portfolio.service.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Formatter;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final StudyService studyService;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    @Value("${obsidian.repo.branch}")
    private String targetBranch;

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader("X-Hub-Signature-256") String signature) {

        // 시그니처 검증
        if (!verifySignature(payload.toString(), signature)) {
            log.warn("웹훅 시그니처 검증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // 이벤트 타입 확인 (push 이벤트만 처리)
        if (!"push".equals(event)) {
            log.info("Push 이벤트가 아니므로 무시: {}", event);
            return ResponseEntity.ok("Event ignored");
        }

        // 대상 브랜치 확인
        String ref = (String) payload.get("ref");
        if (ref == null || !ref.equals("refs/heads/" + targetBranch)) {
            log.info("대상 브랜치({})가 아니므로 무시: {}", targetBranch, ref);
            return ResponseEntity.ok("Branch ignored");
        }

        // 스터디 구조 새로고침
        log.info("GitHub 웹훅 수신: {} 브랜치에 Push 이벤트 발생, 스터디 구조 새로고침 시작", targetBranch);
        studyService.refreshStudyStructure();
        log.info("스터디 구조 새로고침 완료");

        return ResponseEntity.ok("Successfully processed");
    }

    // GitHub 웹훅 시그니처 검증
    private boolean verifySignature(String payload, String signature) {
        try {
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                log.warn("웹훅 시크릿이 설정되지 않았습니다");
                return false;
            }

            String calculatedSignature = "sha256=" + calculateHmacSha256(payload, webhookSecret);
            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("시그니처 검증 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    // HMAC-SHA256 해시 계산
    private String calculateHmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}