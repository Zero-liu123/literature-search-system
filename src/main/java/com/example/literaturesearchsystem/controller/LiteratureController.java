package com.example.literaturesearchsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.literaturesearchsystem.common.Result;
import com.example.literaturesearchsystem.dto.LiteratureSearchDTO;
import com.example.literaturesearchsystem.dto.LiteratureVO;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.entity.SearchHistory;
import com.example.literaturesearchsystem.mapper.LiteratureMapper;
import com.example.literaturesearchsystem.mapper.SearchHistoryMapper;
import com.example.literaturesearchsystem.service.FavoriteService;
import com.example.literaturesearchsystem.service.LiteratureService;
import com.example.literaturesearchsystem.service.SearchHistoryService;
import com.example.literaturesearchsystem.service.SearchService;
import com.example.literaturesearchsystem.util.JwtUtil;
import com.example.literaturesearchsystem.util.PdfParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/literature")
@RequiredArgsConstructor
public class LiteratureController {

    private final LiteratureService literatureService;
    private final SearchService searchService;
    private final PdfParserUtil pdfParserUtil;
    private final FavoriteService favoriteService;
    private final JwtUtil jwtUtil;
    private final SearchHistoryService searchHistoryService;
    private final SearchHistoryMapper searchHistoryMapper;
    private final LiteratureMapper literatureMapper;

    /**
     * 搜索文献（重要：前端调用的接口）
     */
    @PostMapping("/search")
    public Result<?> search(@RequestBody LiteratureSearchDTO searchDTO) {
        log.info("搜索请求: keyword={}, author={}, year={}-{}, page={}, size={}",
                searchDTO.getKeyword(),
                searchDTO.getAuthor(),
                searchDTO.getStartYear(), searchDTO.getEndYear(),
                searchDTO.getPage(), searchDTO.getSize());

        // ⭐ 改这里：走 ES
        var result = searchService.search(searchDTO);

        return Result.success(result);
    }

    /**
     * 新增文献（支持文件上传）- 带 token 验证
     */
    @PostMapping("/add")
    public Result<Literature> addLiterature(
            @RequestPart("literature") Literature literature,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            // 获取当前用户ID
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserId(token);
            if (userId == null) {
                return Result.error("请先登录");
            }
            literature.setContributorId(userId);  // 设置贡献者ID

            Literature result = literatureService.addLiterature(literature, file);
            return Result.successWithMsg("文献添加成功，等待审核", result);
        } catch (Exception e) {
            log.error("添加文献失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新文献（管理员或贡献者可更新自己的文献）
     */
    @PutMapping("/{id}")
    public Result<Void> updateLiterature(
            @PathVariable Long id,
            @RequestBody Literature literature,
            @RequestHeader("Authorization") String token) {
        // 验证登录
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 获取原文献
        Literature existing = literatureMapper.selectById(id);
        if (existing == null) {
            return Result.error("文献不存在");
        }

        // 权限验证：管理员 或 文献贡献者（且是自己上传的）
        Integer role = jwtUtil.getRole(token);
        boolean isAdmin = role != null && role == 2;  // ✅ 使用整数比较
        boolean isOwner = existing.getContributorId() != null && existing.getContributorId().equals(userId);

        if (!isAdmin && !isOwner) {
            return Result.error("权限不足，只能编辑自己上传的文献");
        }

        // 更新字段
        literature.setId(id);
        literature.setUpdateTime(java.time.LocalDateTime.now());
        literatureMapper.updateById(literature);

        // 同步更新 Elasticsearch
        searchService.syncToEs(id);

        return Result.success(null);
    }

    /**
     * 获取文献详情
     */
    @GetMapping("/{id}")
    public Result<LiteratureVO> getById(@PathVariable Long id) {
        return Result.success(literatureService.getById(id));
    }

    /**
     * 删除文献
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        literatureService.deleteById(id);
        searchService.deleteFromEs(id);
        return Result.success(null);
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteByIds(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            literatureService.deleteById(id);
            searchService.deleteFromEs(id);
        }
        return Result.success(null);
    }

    /**
     * 同步到ES
     */
    @PostMapping("/sync")
    public Result<Void> syncToEs() {
        literatureService.syncToEs();
        return Result.success(null);
    }

    /**
     * 解析 PDF 文件，提取元数据
     */
    @PostMapping("/parse-pdf")
    public Result<Map<String, String>> parsePdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("请选择文件");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return Result.error("只支持 PDF 文件");
            }

            Map<String, String> parsedData = pdfParserUtil.parsePdf(file);

            if (parsedData.containsKey("error")) {
                return Result.error("PDF解析失败: " + parsedData.get("error"));
            }

            return Result.success(parsedData);
        } catch (Exception e) {
            log.error("PDF解析失败", e);
            return Result.error("PDF解析失败: " + e.getMessage());
        }
    }

    /**
     * 从token获取用户ID
     */
    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserId(token);
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/favorite/{id}/status")
    public Result<Boolean> isFavorited(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return Result.success(false);
        }
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.success(false);
        }
        boolean result = favoriteService.isFavorited(userId, id);
        return Result.success(result);
    }

    /**
     * 保存搜索历史（在搜索时调用）
     */
    @PostMapping("/search-history/save")
    public Result<Void> saveSearchHistory(
            @RequestBody Map<String, String> params,
            @RequestHeader(value = "Authorization", required = false) String token) {
        String keyword = params.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(null);
        }

        Long userId = null;
        if (token != null && !token.isEmpty()) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            userId = jwtUtil.getUserId(token);
        }

        if (userId != null) {
            searchHistoryService.saveHistory(userId, keyword);
        }
        return Result.success(null);
    }

    /**
     * 获取用户搜索历史
     */
    @GetMapping("/search-history")
    public Result<List<SearchHistory>> getSearchHistory(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<SearchHistory> histories = searchHistoryService.getUserHistory(userId);
        return Result.success(histories);
    }

    /**
     * 清空搜索历史
     */
    @DeleteMapping("/search-history")
    public Result<Void> clearSearchHistory(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        searchHistoryService.clearHistory(userId);
        return Result.success(null);
    }

    /**
     * 删除单条搜索历史
     */
    @DeleteMapping("/search-history/{id}")
    public Result<Void> deleteSearchHistory(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }
        searchHistoryMapper.delete(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getId, id)
                .eq(SearchHistory::getUserId, userId));
        return Result.success(null);
    }

    /**
     * 获取待审核文献列表（仅管理员）
     */
    @GetMapping("/pending")
    public Result<List<Literature>> getPendingLiterature(@RequestHeader("Authorization") String token) {
        // 验证管理员权限
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer role = jwtUtil.getRole(token);
        if (role == null || role != 2) {  // ✅ 使用整数比较
            return Result.error("权限不足");
        }

        LambdaQueryWrapper<Literature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Literature::getStatus, 0); // 待审核
        wrapper.orderByDesc(Literature::getCreateTime);
        List<Literature> list = literatureMapper.selectList(wrapper);
        return Result.success(list);
    }

    /**
     * 获取已审核文献列表（仅管理员）
     */
    @GetMapping("/reviewed")
    public Result<List<Literature>> getReviewedLiterature(@RequestHeader("Authorization") String token) {
        // 验证管理员权限
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Integer role = jwtUtil.getRole(token);
        if (role == null || role != 2) {  // ✅ 使用整数比较
            return Result.error("权限不足");
        }

        LambdaQueryWrapper<Literature> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Literature::getStatus, 1, 2); // 已通过(1)或已驳回(2)
        wrapper.orderByDesc(Literature::getReviewTime);
        List<Literature> list = literatureMapper.selectList(wrapper);
        return Result.success(list);
    }

    /**
     * 获取我的贡献（文献经略专员查看自己上传的文献）
     */
    @GetMapping("/my-contributions")
    public Result<List<Literature>> getMyContributions(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.error("请先登录");
        }

        LambdaQueryWrapper<Literature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Literature::getContributorId, userId);
        wrapper.orderByDesc(Literature::getCreateTime);
        List<Literature> list = literatureMapper.selectList(wrapper);
        return Result.success(list);
    }

    /**
     * 增加浏览量
     */
    @PutMapping("/{id}/view")
    public Result<Void> incrementViewCount(@PathVariable Long id) {
        literatureMapper.incrementViewCount(id);
        return Result.success(null);
    }
/**
 * 审核文献
 */
@PutMapping("/review/{id}")
public Result<Void> reviewLiterature(
        @PathVariable Long id,
        @RequestParam Integer status,
        @RequestParam(required = false) String remark,
        @RequestHeader("Authorization") String token) {
    // 验证管理员权限
    if (token.startsWith("Bearer ")) {
        token = token.substring(7);
    }
    Integer role = jwtUtil.getRole(token);
    if (role == null || role != 2) {
        return Result.error("权限不足");
    }

    // 获取审核人ID
    Long reviewerId = jwtUtil.getUserId(token);
    if (reviewerId == null) {
        return Result.error("请先登录");
    }

    // 获取文献
    Literature literature = literatureMapper.selectById(id);
    if (literature == null) {
        return Result.error("文献不存在");
    }

    // 更新状态
    literature.setStatus(status);
    if (remark != null) {
        literature.setReviewRemark(remark);
    }
    literature.setReviewerId(reviewerId);
    literature.setReviewTime(java.time.LocalDateTime.now());
    literatureMapper.updateById(literature);

    // 暂时注释掉 Elasticsearch 同步，测试基本功能
    // if (status == 1) {
    //     searchService.syncToEs(id);
    // }

    return Result.success(null);
}
}
