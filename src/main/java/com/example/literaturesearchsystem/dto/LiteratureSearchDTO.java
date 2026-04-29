package com.example.literaturesearchsystem.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class LiteratureSearchDTO {
    private String keyword;
    private String author;
    private Integer startYear;
    private Integer endYear;
    private Integer page;
    private Integer size;

    // ========== 新增三个筛选字段 ==========
    private String journal;      // 期刊筛选
    private String category;     // 分类筛选
    private String keywords;     // 关键词筛选
    // ====================================

    public String getKeyword() {
        if (!StringUtils.hasText(keyword) || "null".equals(keyword)) {
            return null;
        }
        return keyword;
    }

    public String getAuthor() {
        if (!StringUtils.hasText(author) || "null".equals(author)) {
            return null;
        }
        return author;
    }

    public Integer getPage() {
        return page == null ? 1 : page;
    }

    public Integer getSize() {
        return size == null ? 10 : size;
    }

    // ========== 新增三个筛选字段的 getter ==========
    public String getJournal() {
        if (!StringUtils.hasText(journal) || "null".equals(journal)) {
            return null;
        }
        return journal;
    }

    public String getCategory() {
        if (!StringUtils.hasText(category) || "null".equals(category)) {
            return null;
        }
        return category;
    }

    public String getKeywords() {
        if (!StringUtils.hasText(keywords) || "null".equals(keywords)) {
            return null;
        }
        return keywords;
    }
    // ============================================
}