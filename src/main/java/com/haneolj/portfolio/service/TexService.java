package com.haneolj.portfolio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TexService {

    // TeX 표현식 저장용 맵, <토큰, 수식>
    private final Map<String, String> texTokenMap = new HashMap<>();
    private int texTokenCounter = 0;

    // 마크다운에서 TeX 표현식을 찾아 보호 토큰으로 대체
    public String protectTexExpressions(String markdown) {
        texTokenMap.clear();
        texTokenCounter = 0;

        // 디스플레이 수식 먼저 처리 ($$...$$)
        markdown = protectDisplayMath(markdown);

        // 인라인 수식 처리 ($...$)
        markdown = protectInlineMath(markdown);

        return markdown;
    }

    // HTML에서 TeX 토큰을 원래 표현식으로 복원
    public String restoreTexExpressions(String html) {
        String result = html;

        // 키 길이순으로 정렬하여 긴 토큰부터 처리 (부분 일치 방지)
        List<String> sortedKeys = new ArrayList<>(texTokenMap.keySet());
        sortedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String key : sortedKeys) {
            result = result.replace(key, texTokenMap.get(key));
        }

        return result;
    }

    // 블럭 수식 보호 ($$...$$)
    private String protectDisplayMath(String text) {
        StringBuilder result = new StringBuilder();
        Pattern regex = Pattern.compile("\\$\\$(.*?)\\$\\$", Pattern.DOTALL);
        Matcher matcher = regex.matcher(text);

        while (matcher.find()) {
            String token = "TEX_DISPLAY_" + texTokenCounter++ + "_TOKEN";
            String texExpr = matcher.group(0);
            texTokenMap.put(token, texExpr);
            matcher.appendReplacement(result, Matcher.quoteReplacement(token));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    // 인라인 수식 보호 ($...$)
    private String protectInlineMath(String text) {
        StringBuilder result = new StringBuilder();
        Pattern regex = Pattern.compile("(?<![\\\\$])\\$(.*?)(?<!\\\\)\\$(?!\\$)", Pattern.DOTALL);
        Matcher matcher = regex.matcher(text);

        while (matcher.find()) {
            // 빈 수식은 건너뛰기
            if (matcher.group(1).trim().isEmpty()) {
                continue;
            }

            String token = "TEX_INLINE_" + texTokenCounter++ + "_TOKEN";
            String texExpr = matcher.group(0);
            texTokenMap.put(token, texExpr);
            matcher.appendReplacement(result, Matcher.quoteReplacement(token));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}