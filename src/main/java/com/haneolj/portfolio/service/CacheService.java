package com.haneolj.portfolio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    // 특정 파일의 캐시만 제거
    @CacheEvict(value = {"markdownHtmlCache", "fileContentCache"}, key = "#filePath")
    public void evictFileCache(String filePath) {
        log.debug("파일 캐시 제거: {}", filePath);
    }

    // HTML 변환 캐시 제거
    @CacheEvict(value = "markdownHtmlCache", key = "'html-' + #markdown.hashCode()")
    public void evictHtmlCache(String markdown) {
        log.debug("HTML 캐시 제거: hash={}", markdown.hashCode());
    }

    // 스터디 구조 캐시 모두 제거
    @CacheEvict(value = "studyStructureCache", allEntries = true)
    public void evictStudyStructureCache() {
        log.debug("스터디 구조 캐시 전체 제거");
    }

    // 모든 캐시 제거
    @CacheEvict(value = {"studyStructureCache", "markdownHtmlCache", "fileContentCache"}, allEntries = true)
    public void evictAllCaches() {
        log.debug("모든 캐시 제거");
    }
}