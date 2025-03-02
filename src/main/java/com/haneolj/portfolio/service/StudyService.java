package com.haneolj.portfolio.service;

import com.haneolj.portfolio.dto.CategoryNodeDto;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {

    private final GitService gitService;
    private final MarkdownService markdownService;

    @Value("${obsidian.repo.study-path}")
    private String studyPath;

    private static final Pattern OBSIDIAN_LINK_PATTERN =
            Pattern.compile("\\[\\[([^]]+)]]|\\[[^]]*]\\(([^)]+\\.md)\\)");

    private LocalDateTime lastUpdate;
    private CategoryNodeDto studyRoot;


    // Study 디렉토리 구조 반환
    @Cacheable(value = "studyStructureCache", unless = "#result == null")
    public CategoryNodeDto getStudyStructure() {
        // Refresh가 필요한지 체크
        if (studyRoot == null) {
            try {
                refreshStudyStructure();
            } catch (Exception e) {
                log.error("스터디 구조를 새로고침하는 중 오류 발생: {}", e.getMessage(), e);
                // 오류 발생 시 null 반환 (view에서 처리)
                return null;
            }
        }
        return studyRoot;
    }

    // 마지막 Timestamp 가져오기
    public String getLastUpdateDate() {
        if (lastUpdate == null) {
            return "Unknown";
        }
        return lastUpdate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    // Study 디렉토리 구조 Refresh
    @CacheEvict(value = {"studyStructureCache", "markdownHtmlCache", "fileContentCache"}, allEntries = true)
    public synchronized void refreshStudyStructure() {
        log.info("스터디 구조 새로고침 시작 (캐시 초기화)");
        String repoPath = gitService.ensureRepository();
        log.info("저장소 경로: {}", repoPath);

        Path studyDirectoryPath = Paths.get(repoPath, studyPath);
        log.info("스터디 디렉토리 경로: {}", studyDirectoryPath);

        try {
            if (!Files.exists(studyDirectoryPath)) {
                log.error("스터디 디렉토리가 존재하지 않습니다: {}", studyDirectoryPath);
                throw new RuntimeException("스터디 디렉토리를 찾을 수 없습니다");
            }

            studyRoot = new CategoryNodeDto("Study", studyDirectoryPath.toString(), true);
            List<Path> allMarkdownFiles = new ArrayList<>();

            processDirectory(studyRoot, studyDirectoryPath, allMarkdownFiles);
            lastUpdate = LocalDateTime.now();

            // 비동기적으로 모든 마크다운 파일을 사전 캐싱
            log.info("총 {} 개의 마크다운 파일을 사전 캐싱합니다.", allMarkdownFiles.size());
            CompletableFuture.runAsync(() -> precacheMarkdownFiles(allMarkdownFiles));

            log.info("스터디 구조 새로고침 완료");
        } catch (IOException e) {
            log.error("스터디 구조 새로고침 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("스터디 구조 새로고침 실패", e);
        }
    }


    // 디렉토리 구조 검증 및 디버깅
    private void validateAndLogStructure(CategoryNodeDto node, int depth) {
        String indent = "  ".repeat(depth);
        log.info("{}노드: {} (디렉토리: {}, 자식 수: {})",
                indent,
                node.getName(),
                node.isDirectory(),
                node.getChildren().size());

        if (node.isDirectory() && !node.getChildren().isEmpty()) {
            log.info("{}자식 노드 목록:", indent);
            for (CategoryNodeDto child : node.getChildren()) {
                log.info("{}  - {}{}",
                        indent,
                        child.getName(),
                        child.isDirectory() ? " (디렉토리)" : " (파일)");
            }

            // 재귀적으로 모든 자식 노드 검증
            for (CategoryNodeDto child : node.getChildren()) {
                validateAndLogStructure(child, depth + 1);
            }
        }
    }

    // 모든 마크다운 파일 사전 캐싱
    private void precacheMarkdownFiles(List<Path> markdownFiles) {
        int total = markdownFiles.size();
        int processed = 0;
        int success = 0;
        int failed = 0;

        log.info("마크다운 파일 사전 캐싱 시작 (총 {} 파일)", total);

        for (Path file : markdownFiles) {
            try {
                // 파일 내용 읽기
                String markdownContent = markdownService.readMarkdownFile(file);

                // HTML로 변환하여 캐싱
                markdownService.convertToHtml(markdownContent);

                success++;
            } catch (Exception e) {
                log.warn("파일 사전 캐싱 실패: {}, 오류: {}", file, e.getMessage());
                failed++;
            }

            processed++;

            // 진행 상태 로깅 (10% 단위)
            if (processed % Math.max(1, total / 10) == 0 || processed == total) {
                int percentage = (int) (((double) processed / total) * 100);
                log.info("사전 캐싱 진행 중: {}% 완료 ({}/{}), 성공: {}, 실패: {}",
                        percentage, processed, total, success, failed);
            }
        }

        log.info("마크다운 파일 사전 캐싱 완료. 총 {} 파일 중 {} 성공, {} 실패",
                total, success, failed);
    }

    // 디렉토리 처리
    // 처리된 디렉토리는 디렉토리 구조에 추가
    private void processDirectory(CategoryNodeDto parentNode, Path directoryPath, List<Path> allMarkdownFiles) throws IOException {
        if (!Files.exists(directoryPath)) {
            log.warn("디렉토리가 존재하지 않습니다: {}", directoryPath);
            return;
        }

        if (!Files.isDirectory(directoryPath)) {
            log.warn("경로가 디렉토리가 아닙니다: {}", directoryPath);
            return;
        }

        try {
            // 디렉토리 내용을 스트림으로 읽고 정렬
            List<Path> sortedEntries = Files.list(directoryPath)
                    .sorted((a, b) -> {
                        boolean aDir = Files.isDirectory(a);
                        boolean bDir = Files.isDirectory(b);

                        if (aDir && !bDir) return -1;
                        if (!aDir && bDir) return 1;

                        return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                    })
                    .toList();

            for (Path entry : sortedEntries) {
                String name = entry.getFileName().toString();

                // 숨김 파일과 디렉토리 건너뛰기
                if (name.startsWith(".")) {
                    log.debug("숨김 항목 건너뛰기: {}", name);
                    continue;
                }

                boolean isDirectory = Files.isDirectory(entry);

                // 번호 제거 ("1. Study" -> "Study")
                String displayName = name.replaceAll("^\\d+\\.\\s*", "");

                if (isDirectory) {
                    CategoryNodeDto childNode = new CategoryNodeDto(displayName, entry.toString(), true);
                    parentNode.addChild(childNode);
                    processDirectory(childNode, entry, allMarkdownFiles);
                } else if (name.endsWith(".md")) {
                    // 마크다운 파일을 목록에 추가
                    allMarkdownFiles.add(entry);

                    // 표시용 확장자 제거
                    displayName = displayName.substring(0, displayName.length() - 3);

                    CategoryNodeDto fileNode = new CategoryNodeDto(displayName, entry.toString(), false);

                    try {
                        // 파일 수정 시간 가져오기
                        LocalDateTime lastModified = LocalDateTime.ofInstant(
                                Files.getLastModifiedTime(entry).toInstant(),
                                ZoneId.systemDefault());
                        fileNode.setLastModified(lastModified);

                        // 마크다운 파일 파싱하여 링크 추출
                        String content = Files.readString(entry);
                        List<String> links = extractLinks(content);
                        fileNode.setLinks(links);
                    } catch (IOException e) {
                        log.warn("파일 정보 읽기 오류: {}: {}", entry, e.getMessage());
                    }

                    parentNode.addChild(fileNode);
                } else {
                    log.debug("마크다운이 아닌 파일 건너뛰기: {}", name);
                }
            }
        } catch (IOException e) {
            log.error("디렉토리 처리 중 오류 발생: {}: {}", directoryPath, e.getMessage());
            throw e;
        }
    }

    // 옵시디언 링크 추출
    private List<String> extractLinks(String content) {
        List<String> links = new ArrayList<>();
        Matcher matcher = OBSIDIAN_LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            String link = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (link != null) {
                links.add(link);
            }
        }

        return links;
    }

}