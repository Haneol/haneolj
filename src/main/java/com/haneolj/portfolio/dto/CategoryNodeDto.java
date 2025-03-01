package com.haneolj.portfolio.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CategoryNodeDto {
    private final String name;
    private final String path;
    private final boolean directory;
    private final List<CategoryNodeDto> children = new ArrayList<>();
    @Setter
    private List<String> links = new ArrayList<>();
    @Setter
    private LocalDateTime lastModified;

    public CategoryNodeDto(String name, String path, boolean directory) {
        this.name = name;
        this.path = path;
        this.directory = directory;
    }

    public void addChild(CategoryNodeDto child) {
        children.add(child);
    }

    public String getLastModifiedFormatted() {
        if (lastModified == null) {
            return "";
        }
        return lastModified.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }
}
