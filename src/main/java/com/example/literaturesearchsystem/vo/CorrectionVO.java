package com.example.literaturesearchsystem.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CorrectionVO {

    private Long id;
    private Long literatureId;
    private String literatureTitle;
    private String correctionData;
    private Integer status;
    private String statusDesc;
    private String reviewRemark;
    private LocalDateTime createTime;
}