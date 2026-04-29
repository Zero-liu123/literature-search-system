package com.example.literaturesearchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.literaturesearchsystem.entity.SearchHistory;
import com.example.literaturesearchsystem.mapper.SearchHistoryMapper;
import com.example.literaturesearchsystem.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryMapper searchHistoryMapper;

    @Override
    public void saveHistory(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        // 避免重复保存相同的搜索记录（删除旧的同关键词记录）
        LambdaQueryWrapper<SearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SearchHistory::getUserId, userId).eq(SearchHistory::getKeyword, keyword);
        searchHistoryMapper.delete(wrapper);

        // 保存新记录
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setSearchTime(LocalDateTime.now());
        searchHistoryMapper.insert(history);

        // 只保留最近20条，删除多余的
        List<SearchHistory> histories = searchHistoryMapper.selectByUserId(userId);
        if (histories.size() > 20) {
            for (int i = 20; i < histories.size(); i++) {
                searchHistoryMapper.deleteById(histories.get(i).getId());
            }
        }

        log.info("保存搜索历史: userId={}, keyword={}", userId, keyword);
    }

    @Override
    public List<SearchHistory> getUserHistory(Long userId) {
        return searchHistoryMapper.selectByUserId(userId);
    }

    @Override
    public void clearHistory(Long userId) {
        searchHistoryMapper.deleteByUserId(userId);
        log.info("清空搜索历史: userId={}", userId);
    }
}