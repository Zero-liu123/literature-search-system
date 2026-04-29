package com.example.literaturesearchsystem.dto;

import lombok.Data;

@Data
public class LiteratureUploadDTO {

    private String title;

    private String authors;

    private String abstractText;

    private Integer publishYear;

    private String journal;

    private String keywords;

    private String doi;

    private String category;
}