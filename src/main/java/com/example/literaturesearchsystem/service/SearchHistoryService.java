package com.example.literaturesearchsystem.service;

import com.example.literaturesearchsystem.entity.SearchHistory;
import java.util.List;

public interface SearchHistoryService {

    /**
     * 保存搜索记录
     */
    void saveHistory(Long userId, String keyword);

    /**
     * 获取用户搜索历史
     */
    List<SearchHistory> getUserHistory(Long userId);

    /**
     * 清空用户搜索历史
     */
    void clearHistory(Long userId);
}