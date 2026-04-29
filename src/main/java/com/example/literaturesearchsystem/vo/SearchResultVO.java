package com.example.literaturesearchsystem.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SearchResultVO {

    private List<SearchHitVO> records;  // 搜索结果列表

    private Long total;                  // 总数

    private Integer page;                // 当前页

    private Integer size;                // 每页大小

    private Map<String, List<AggregationVO>> aggregations;  // 聚合统计

    @Data
    public static class SearchHitVO {
        private Long id;
        private String title;
        private String authors;
        private String abstractText;
        private Integer publishYear;
        private String journal;
        private String keywords;
        private String doi;
        private String category;
        private Integer viewCount;
        private Float score;
        private Map<String, List<String>> highlights;  // 高亮内容
    }

    @Data
    public static class AggregationVO {
        private String key;
        private Long count;
    }
}