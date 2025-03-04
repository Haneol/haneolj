package com.haneolj.portfolio.service;

import com.haneolj.portfolio.dto.CategoryNodeDto;
import com.haneolj.portfolio.dto.GraphDataDto;
import com.haneolj.portfolio.dto.GraphLinkDto;
import com.haneolj.portfolio.dto.GraphNodeDto;
import com.haneolj.portfolio.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final StudyService studyService;
    private final StringUtils stringUtils;

    // 마크다운 파일 내부 링크 정보를 바탕으로 그래프 데이터를 생성 & 캐싱
    @Cacheable(value = "graphDataCache", unless = "#result == null")
    public GraphDataDto getGraphData() {
        log.info("마크다운 파일의 그래프 데이터 생성 중...");

        // 스터디 구조 가져오기
        CategoryNodeDto studyRoot = studyService.getStudyStructure();
        if (studyRoot == null) {
            log.warn("스터디 구조를 가져올 수 없습니다.");
            return null;
        }

        GraphDataDto graphData = new GraphDataDto();
        Map<String, String> pathToIdMap = new HashMap<>(); // <파일 경로, 노드 ID>
        Map<String, String> nameToIdMap = new HashMap<>(); // <파일 이름, 노드 ID>
        Set<String> processedLinks = new HashSet<>(); // 중복 링크 방지

        // 노드, 링크 수집
        collectNodes(studyRoot, graphData, pathToIdMap, nameToIdMap);
        collectLinks(studyRoot, graphData, pathToIdMap, nameToIdMap, processedLinks);

        log.info("그래프 데이터 생성 완료: 노드 {} 개, 링크 {} 개",
                graphData.getNodes().size(), graphData.getLinks().size());

        return graphData;
    }

    // 캐시된 그래프 데이터 갱신
    @CacheEvict(value = "graphDataCache", allEntries = true)
    public void refreshGraphData() {
        log.info("그래프 데이터 캐시 초기화");
    }

    // 모든 마크다운 파일 노드 수집
    private void collectNodes(CategoryNodeDto node, GraphDataDto graphData,
            Map<String, String> pathToIdMap, Map<String, String> nameToIdMap) {
        if (!node.isDirectory()) {
            // 파일 노드
            String nodePath = node.getPath();
            String nodeId = "node-" + graphData.getNodes().size();
            String encodedPath = stringUtils.encodeBase64Url(nodePath);

            // 노드 추가
            GraphNodeDto graphNode = new GraphNodeDto();
            graphNode.setId(nodeId);
            graphNode.setName(node.getName());
            graphNode.setEncodedPath(encodedPath);

            graphData.getNodes().add(graphNode);

            // 매핑 저장
            pathToIdMap.put(nodePath, nodeId);
            nameToIdMap.put(node.getName(), nodeId);
        } else {
            // 디렉토리에서 재귀적으로 자식 노드 탐색
            for (CategoryNodeDto child : node.getChildren()) {
                collectNodes(child, graphData, pathToIdMap, nameToIdMap);
            }
        }
    }

    // 마크다운 파일 간의 링크 수집
    private void collectLinks(CategoryNodeDto node, GraphDataDto graphData,
            Map<String, String> pathToIdMap, Map<String, String> nameToIdMap,
            Set<String> processedLinks) {
        if (!node.isDirectory()) {
            // 파일 내부 링크 정보 처리
            String sourceId = pathToIdMap.get(node.getPath());

            if (sourceId != null && !node.getLinks().isEmpty()) {
                for (String targetName : node.getLinks()) {
                    // 링크 대상 찾기
                    String targetId = nameToIdMap.get(targetName);

                    if (targetId != null) {
                        // 링크 중복 방지
                        String linkKey = sourceId + "-" + targetId;
                        String reverseLinkKey = targetId + "-" + sourceId;

                        if (!processedLinks.contains(linkKey) && !processedLinks.contains(reverseLinkKey)) {
                            graphData.getLinks().add(new GraphLinkDto(sourceId, targetId, 1.0));
                            processedLinks.add(linkKey);
                        }
                    }
                }
            }
        } else {
            // 디렉토리에서 재귀적으로 자식 노드 탐색
            for (CategoryNodeDto child : node.getChildren()) {
                collectLinks(child, graphData, pathToIdMap, nameToIdMap, processedLinks);
            }
        }
    }
}