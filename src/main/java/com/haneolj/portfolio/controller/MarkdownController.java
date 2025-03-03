package com.haneolj.portfolio.controller;

import com.haneolj.portfolio.service.MarkdownService;
import com.haneolj.portfolio.service.StudyService;
import com.haneolj.portfolio.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Controller
@RequestMapping("/study")
@RequiredArgsConstructor
public class MarkdownController {

    @Value("${app.version}")
    private String appVersion;

    private final MarkdownService markdownService;
    private final StudyService studyService;
    private final StringUtils stringUtils;

    @GetMapping("/view/{encodedPath}")
    public String viewMarkdown(@PathVariable String encodedPath, Model model) {

        model.addAttribute("version", appVersion);

        try {
            // Base64로 인코딩된 경로를 디코딩
            String decodedPath = stringUtils.decodeBase64Url(encodedPath);
            log.info("디코딩된 파일 경로: {}", decodedPath);

            return renderMarkdownFile(decodedPath, model);
        } catch (Exception e) {
            log.error("마크다운 처리 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("error", "오류가 발생했습니다: " + e.getMessage());
            return "error/generic";
        }
    }

    private String renderMarkdownFile(String filePath, Model model) throws IOException {
        Path path = Paths.get(filePath);

        // 파일 확장자 제거하여 제목으로 사용
        String title = path.getFileName().toString();
        if (title.endsWith(".md")) {
            title = title.substring(0, title.length() - 3);
        }

        // 번호 제거 ("1. Study" -> "Study")
        title = title.replaceAll("^\\d+\\.\\s*", "");

        // 마크다운 내용 가져오기
        String markdownContent = markdownService.readMarkdownFile(path);

        // HTML로 변환
        String htmlContent = markdownService.convertToHtml(markdownContent);

        // 모델에 데이터 추가
        model.addAttribute("contentTitle", title);
        model.addAttribute("contentBody", htmlContent);
        model.addAttribute("contentLastModified", markdownService.getLastModifiedDate(path));
        model.addAttribute("contentCreatedAt", markdownService.getFileCreationDate(path));
        model.addAttribute("studyRoot", studyService.getStudyStructure());
        model.addAttribute("currentFilePath", filePath);

        log.info("모델 속성 - contentTitle: {}", title);
        log.info("모델 속성 - contentLastModified: {}", markdownService.getLastModifiedDate(path));
        log.info("모델 속성 - contentBody 길이: {}", htmlContent.length());

        return "study/markdown-view";
    }
}