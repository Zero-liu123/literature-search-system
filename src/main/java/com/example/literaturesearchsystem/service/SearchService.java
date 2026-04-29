package com.example.literaturesearchsystem.service;

import com.example.literaturesearchsystem.dto.LiteratureSearchDTO;
import com.example.literaturesearchsystem.vo.SearchResultVO;
import java.util.List;

public interface SearchService {

    /**
     * 全文检索
     */
    SearchResultVO search(LiteratureSearchDTO searchDTO);

    /**
     * 搜索建议/自动补全
     */
    List<String> suggest(String prefix);

    /**
     * 同步单条文献到 ES
     */
    void syncToEs(Long literatureId);

    /**
     * 批量同步所有文献到 ES
     */
    void syncAllToEs();

    /**
     * 从 ES 中删除单条文献
     * @param literatureId 文献ID
     */
    void deleteFromEs(Long literatureId);

    /**
     * 从 ES 中批量删除文献
     * @param literatureIds 文献ID列表
     */
    void batchDeleteFromEs(List<Long> literatureIds);
}