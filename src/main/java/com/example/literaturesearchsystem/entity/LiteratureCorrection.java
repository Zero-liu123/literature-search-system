package com.example.literaturesearchsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("literature_correction")
public class LiteratureCorrection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long literatureId;

    private Long contributorId;

    private String correctionData;  // JSON格式存储修正内容

    private Integer status;  // 0-待审核 1-已通过 2-已驳回

    private String reviewRemark;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}