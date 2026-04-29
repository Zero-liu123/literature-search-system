package com.example.literaturesearchsystem.controller;

import com.example.literaturesearchsystem.common.Result;
import com.example.literaturesearchsystem.entity.SearchHistory;
import com.example.literaturesearchsystem.mapper.SearchHistoryMapper;
import com.example.literaturesearchsystem.service.SearchService;
import com.example.literaturesearchsystem.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SearchHistoryMapper searchHistoryMapper;
    private final JwtUtil jwtUtil;

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("服务正常运行");
    }

    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String prefix) {
        try {
            List<String> suggestions = searchService.suggest(prefix);
            return Result.success(suggestions);
        } catch (Exception e) {
            log.error("搜索建议失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/sync-all")
    public Result<Void> syncAllToEs() {
        try {
            searchService.syncAllToEs();
            return Result.success(null);
        } catch (Exception e) {
            log.error("批量同步失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/es/{literatureId}")
    public Result<Void> deleteFromEs(@PathVariable Long literatureId) {
        try {
            searchService.deleteFromEs(literatureId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("从ES删除文献失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/es/batch")
    public Result<Void> batchDeleteFromEs(@RequestBody List<Long> literatureIds) {
        try {
            searchService.batchDeleteFromEs(literatureIds);
            return Result.success(null);
        } catch (Exception e) {
            log.error("从ES批量删除文献失败", e);
            return Result.error(e.getMessage());
        }
    }

    // ========== 搜索历史相关接口 ==========

    /**
     * 保存搜索历史
     */
    @PostMapping("/history/save")
    public Result<Void> saveSearchHistory(@RequestBody Map<String, String> params,
                                          @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        String keyword = params.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("关键词不能为空");
        }

        try {
            // 删除相同关键词的旧记录
            searchHistoryMapper.deleteByUserIdAndKeyword(userId, keyword);

            // 插入新记录
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(keyword);
            searchHistoryMapper.insert(history);

            // 只保留最近20条
            searchHistoryMapper.keepLatest(userId, 20);

            return Result.success(null);
        } catch (Exception e) {
            log.error("保存搜索历史失败", e);
            return Result.error("保存失败");
        }
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    public Result<List<SearchHistory>> getSearchHistory(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            List<SearchHistory> historyList = searchHistoryMapper.selectByUserId(userId);
            return Result.success(historyList);
        } catch (Exception e) {
            log.error("获取搜索历史失败", e);
            return Result.error("获取失败");
        }
    }

    private Long getUserIdFromToken(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            return jwtUtil.getUserId(token);
        } catch (Exception e) {
            log.error("解析token失败", e);
            return null;
        }
    }
}