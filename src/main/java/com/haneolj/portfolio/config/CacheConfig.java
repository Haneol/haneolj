package com.haneolj.portfolio.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "markdownHtmlCache", "studyStructureCache", "fileContentCache", "graphDataCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000));
        return cacheManager;
    }
}