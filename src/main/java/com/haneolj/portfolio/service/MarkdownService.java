package com.haneolj.portfolio.service;

import com.haneolj.portfolio.util.StringUtils;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final StringUtils stringUtils;
    private final GitService gitService;
    private final TexService texService;

    @Value("${obsidian.repo.study-path}")
    private String studyPath;

    // Obsidian 링크 패턴 ([[링크]])
    private static final Pattern OBSIDIAN_LINK_PATTERN = Pattern.compile("\\[\\[([^]]+)]]");

    // 일반 마크다운 링크 패턴 ([텍스트](링크.md))
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+\\.md)\\)");

    @Autowired
    public MarkdownService(GitService gitService, StringUtils stringUtils, TexService texService) {
        this.stringUtils = stringUtils;
        this.gitService = gitService;
        this.texService = texService;

        // 확장 기능 추가 (테이블, 체크박스 등)
        List<Extension> extensions = Arrays.asList(
                TablesExtension.create(),
                TaskListItemsExtension.create()
        );

        this.parser = Parser.builder()
                .extensions(extensions)
                .build();

        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .attributeProviderFactory(context -> new CustomAttributeProvider())
                .build();
    }


    // 마크다운 파일 읽기
    @Cacheable(value = "markdownHtmlCache", key = "#filePath.toString()")
    public String readMarkdownFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("파일이 존재하지 않습니다: " + filePath);
        }

        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    // 마크다운 파일을 HTML 파일로 변경
    @Cacheable(value = "markdownHtmlCache", key = "'html-' + #markdown.hashCode()")
    public String convertToHtml(String markdown) {
        try {
            // TeX 표현식 임시 보호
            markdown = texService.protectTexExpressions(markdown);

            // CommonMark로 HTML 변환 전에 Obsidian 링크 처리
            markdown = processObsidianLinks(markdown);
            markdown = processMarkdownLinks(markdown);

            // CommonMark를 사용하여 HTML로 변환
            Node document = parser.parse(markdown);
            String html = renderer.render(document);

            // TeX 표현식 복원
            html = texService.restoreTexExpressions(html);

            return html;
        } catch (Exception e) {
            log.error("마크다운을 HTML로 변환 중 오류 발생: {}", e.getMessage(), e);
            return "<div class='alert alert-danger'>마크다운 변환 중 오류가 발생했습니다: " + e.getMessage() + "</div>"
                    + "<pre>" + markdown + "</pre>";
        }
    }

    // 옵시디언의 Link 형식 처리
    private String processObsidianLinks(String markdown) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = OBSIDIAN_LINK_PATTERN.matcher(markdown);

        while (matcher.find()) {
            String linkContent = matcher.group(1);
            String replacement = createWikiLinkReplacement(linkContent);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }


    // [텍스트](링크) 형식의 링크 처리
    private String processMarkdownLinks(String markdown) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = MARKDOWN_LINK_PATTERN.matcher(markdown);

        while (matcher.find()) {
            String linkText = matcher.group(1);
            String linkTarget = matcher.group(2);
            String replacement = "[" + linkText + "](" + getFileUrl(linkTarget.replace(".md", "")) + ")";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    // 링크 변경
    private String createWikiLinkReplacement(String linkContent) {
        // 기본 링크 텍스트와 대상
        String linkText = linkContent;
        String linkTarget = linkContent;

        // | 문자로 분리된 별칭 처리
        if (linkContent.contains("|")) {
            String[] parts = linkContent.split("\\|", 2);
            linkTarget = parts[0].trim();
            linkText = parts[1].trim();
        }

        String url;

        // # 기호로 시작하는 헤더 링크
        if (linkTarget.contains("#")) {
            String[] parts = linkTarget.split("#", 2);
            String fileName = parts[0].trim();
            String headerTarget = parts[1].trim();

            // 같은 페이지 내 헤더 링크인 경우
            if (fileName.isEmpty()) {
                url = "#" + headerToId(headerTarget);
            } else {
                // 다른 파일의 헤더 링크 - 여기서 파일 경로와 헤더를 분리해서 처리
                String fileUrl = getFileUrl(fileName);
                url = fileUrl + "#" + headerToId(headerTarget);
            }
        }
        // ^ 기호로 시작하는 블록 링크
        else if (linkTarget.contains("^")) {
            String[] parts = linkTarget.split("\\^", 2);
            String fileName = parts[0].trim();
            String blockRef = parts[1].trim();

            // 블록 참조 URL 설정
            if (fileName.isEmpty()) {
                url = "#" + blockRef;
            } else {
                url = getFileUrl(fileName) + "#^" + blockRef;
            }
        }
        // 일반 파일 링크
        else {
            url = getFileUrl(linkTarget);
        }

        // 클래스 속성 없이 반환
        return "[" + linkText + "](" + url + ")";
    }

    // 헤더를 ID로 변환
    private String headerToId(String headerText) {
        return headerText.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^\\w\\-]", "")
                .replaceAll("-+", "-");
    }

    // 파일 이름으로 URL 생성
    private String getFileUrl(String fileName) {
        try {
            // 현재 상대 경로에서 실제 파일을 찾아야 함
            String repoPath = gitService.ensureRepository();
            Path studyRootPath = Paths.get(repoPath, studyPath);

            // 1. 전체 폴더 구조에서 해당 파일명 찾기
            final String fileNameWithExtension = fileName + ".md";
            Optional<Path> foundPath = Files.walk(studyRootPath)
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.getFileName().toString().equals(fileNameWithExtension))
                    .findFirst();

            if (foundPath.isPresent()) {
                // 2. 찾은 전체 경로를 인코딩
                String fullPath = foundPath.get().toString();
                return "/study/view/" + stringUtils.encodeBase64Url(fullPath);
            }

            // 못 찾은 경우 기본 경로 사용
            return "/study/view/" + stringUtils.encodeBase64Url(fileName + ".md");
        } catch (Exception e) {
            log.error("파일 URL 생성 중 오류: {}", e.getMessage());
            return "/study/view/" + stringUtils.encodeBase64Url(fileName + ".md");
        }
    }

    // 파일의 마지막 수정 일자 찾기
    public String getLastModifiedDate(Path filePath) {
        try {
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(filePath).toInstant(),
                    ZoneId.systemDefault());

            return lastModified.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        } catch (IOException e) {
            log.warn("파일 수정 시간을 가져올 수 없습니다: {}", e.getMessage());
            return "Unknown";
        }
    }

    public String getFileCreationDate(Path filePath) {
        try {
            LocalDateTime createdAt = gitService.getFileCreationDate(filePath);
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        } catch (Exception e) {
            log.warn("파일 생성 시간을 가져올 수 없습니다: {}", e.getMessage());
            return "Unknown";
        }
    }

    // 체크박스, Callout 등 스타일링
    static class CustomAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            // 테이블에 Bootstrap 클래스 추가
            if (node instanceof TableBlock) {
                attributes.put("class", "table table-bordered");
            }

            // 체크박스 처리
            if (tagName.equals("input") && attributes.containsKey("type")
                    && attributes.get("type").equals("checkbox")) {
                // 체크박스 비활성화 (읽기 전용)
                attributes.put("disabled", "disabled");
            }
        }
    }
}