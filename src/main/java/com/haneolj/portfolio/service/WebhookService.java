package com.haneolj.portfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final GitService gitService;
    private final StudyService studyService;
    private final CacheService cacheService;

    @Value("${obsidian.repo.study-path}")
    private String studyPath;

    /**
     * GitHub 웹훅 페이로드에서 변경된 파일 목록을 추출합니다.
     */
    public List<String> extractChangedFilesFromPayload(byte[] payload) {
        Set<String> changedFiles = new HashSet<>();
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            // commits에서 변경된 파일 목록 수집
            JsonNode commits = jsonNode.path("commits");
            if (commits.isArray()) {
                for (JsonNode commit : commits) {
                    // 추가된 파일
                    addFilesFromJsonArray(changedFiles, commit.path("added"));

                    // 수정된 파일
                    addFilesFromJsonArray(changedFiles, commit.path("modified"));

                    // 삭제된 파일
                    addFilesFromJsonArray(changedFiles, commit.path("removed"));
                }
            }
        } catch (Exception e) {
            log.error("페이로드에서 변경된 파일 목록 추출 중 오류 발생: {}", e.getMessage(), e);
        }
        return new ArrayList<>(changedFiles);
    }

    private void addFilesFromJsonArray(Set<String> fileList, JsonNode jsonArray) {
        if (jsonArray.isArray()) {
            for (JsonNode file : jsonArray) {
                fileList.add(file.asText());
            }
        }
    }

    /**
     * 변경된 파일 목록을 기반으로 필요한 캐시만 갱신합니다.
     */
    public void processChangedFiles(List<String> changedFiles) {
        log.info("변경된 파일 처리 시작 (총 {}개)", changedFiles.size());

        // Git 저장소 경로 가져오기
        String repoPath = gitService.ensureRepository();

        // 스터디 관련 파일만 필터링
        List<Path> studyFiles = filterStudyFiles(changedFiles, repoPath);

        if (studyFiles.isEmpty()) {
            log.info("스터디 관련 변경 파일 없음, 처리 종료");
            return;
        }

        // 변경 파일 중에 디렉토리 구조 변경이 있는지 확인
        boolean hasStructureChanges = checkForStructureChanges(changedFiles);

        if (hasStructureChanges) {
            // 구조 변경이 있으면 전체 새로고침
            log.info("디렉토리 구조 변경 감지, 전체 새로고침 수행");
            studyService.refreshStudyStructure();
        } else {
            // 개별 파일만 새로고침
            log.info("개별 파일만 새로고침 ({}개)", studyFiles.size());

            // 각 파일에 대해 캐시 제거
            for (Path filePath : studyFiles) {
                cacheService.evictFileCache(filePath.toString());

                // 파일이 존재하면 노드 업데이트, 없으면 노드 제거
                if (Files.exists(filePath)) {
                    studyService.updateFileNode(filePath);
                } else {
                    studyService.removeFileNode(filePath.toString());
                }
            }
        }

        log.info("변경된 파일 처리 완료");
    }

    private List<Path> filterStudyFiles(List<String> changedFiles, String repoPath) {
        List<Path> studyFiles = new ArrayList<>();
        String studyPathNormalized = studyPath.replace('\\', '/');

        for (String file : changedFiles) {
            String normalizedPath = file.replace('\\', '/');

            // 스터디 경로 내 마크다운 파일만 처리
            if (normalizedPath.startsWith(studyPathNormalized) && normalizedPath.endsWith(".md")) {
                Path fullPath = Paths.get(repoPath, normalizedPath);
                studyFiles.add(fullPath);
            }
        }

        return studyFiles;
    }

    private boolean checkForStructureChanges(List<String> changedFiles) {
        // 디렉토리 구조 변경으로 간주할 조건:
        // 1. .md가 아닌 파일이 변경됨 (설정 파일 등)
        // 2. 파일 삭제/추가 (수정은 구조 변경으로 간주하지 않음)

        for (String file : changedFiles) {
            // 마크다운 파일이 아닌 경우
            if (!file.endsWith(".md")) {
                return true;
            }

            // 여기에 필요하면 다른 조건 추가
        }

        return false;
    }
}