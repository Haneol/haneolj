package com.haneolj.portfolio.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class StringUtils {

    public String encodeBase64Url(String input) {
        if (input == null) {
            return "";
        }
        return Base64.getUrlEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    public String decodeBase64Url(String encoded) {
        if (encoded == null) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}