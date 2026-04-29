package com.example.literaturesearchsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Literature {

    private Long id;

    // 投稿人
    private Long contributorId;

    // 标题
    private String title;

    // 作者
    private String authors;

    // 摘要
    private String abstractText;

    // 年份
    private Integer publishYear;

    private String journal;

    private String keywords;

    private String doi;

    private String category;

    // ⭐ 添加fileUrl字段
    private String fileUrl;
    // 状态（审核状态）
    private Integer status;

    // 浏览量
    private Integer viewCount;

    // 收藏数
    private Integer favoriteCount;

    // 审核备注
    private String reviewRemark;

    // 审核人
    private Long reviewerId;

    // 审核时间
    private LocalDateTime reviewTime;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
