package com.example.literaturesearchsystem.dto;

import lombok.Data;

@Data
public class LiteratureVO {
    private Long id;
    private String title;
    private String author;
    private String abstractText;
    private String keywords;
    private String journal;
    private Integer year;
    private String doi;
    private Double score;

    private Integer viewCount;      // 浏览量
    private String category;
}