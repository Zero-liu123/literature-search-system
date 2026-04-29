package com.example.literaturesearchsystem.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContributionVO {

    private Long id;
    private String title;
    private String authors;
    private Integer publishYear;
    private String journal;
    private Integer status;
    private String statusDesc;
    private String statusBadgeClass;
    private String reviewRemark;
    private LocalDateTime createTime;
}