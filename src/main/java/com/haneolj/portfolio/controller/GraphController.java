package com.haneolj.portfolio.controller;

import com.haneolj.portfolio.dto.GraphDataDto;
import com.haneolj.portfolio.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class GraphController {
    private final GraphService graphService;

    @GetMapping("/graph")
    public ResponseEntity<GraphDataDto> getGraphData() {
        try {
            GraphDataDto graphData = graphService.getGraphData();

            if (graphData == null || graphData.getNodes().isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(graphData);
        } catch (Exception e) {
            log.error("그래프 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}