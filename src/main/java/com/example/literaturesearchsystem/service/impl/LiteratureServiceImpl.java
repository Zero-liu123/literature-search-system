package com.example.literaturesearchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.literaturesearchsystem.dto.LiteratureSearchDTO;
import com.example.literaturesearchsystem.dto.LiteratureVO;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.mapper.LiteratureMapper;
import com.example.literaturesearchsystem.service.LiteratureService;
import com.example.literaturesearchsystem.service.SearchService;
import com.example.literaturesearchsystem.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiteratureServiceImpl implements LiteratureService {

    private final LiteratureMapper literatureMapper;
    private final SearchService searchService;
    private final FileUploadUtil fileUploadUtil;

    @Override
    public Map<String, Object> search(LiteratureSearchDTO searchDTO) {
        // 参数预处理
        String rawKeyword = searchDTO.getKeyword();
        String rawAuthor = searchDTO.getAuthor();
        String rawJournal = searchDTO.getJournal();
        String rawCategory = searchDTO.getCategory();
        String rawKeywords = searchDTO.getKeywords();

        String keyword = (rawKeyword != null && !"null".equals(rawKeyword) && !rawKeyword.trim().isEmpty()) ? rawKeyword.trim() : null;
        String author = (rawAuthor != null && !"null".equals(rawAuthor) && !rawAuthor.trim().isEmpty()) ? rawAuthor.trim() : null;
        String journal = (rawJournal != null && !"null".equals(rawJournal) && !rawJournal.trim().isEmpty()) ? rawJournal.trim() : null;
        String category = (rawCategory != null && !"null".equals(rawCategory) && !rawCategory.trim().isEmpty()) ? rawCategory.trim() : null;
        String keywordsFilter = (rawKeywords != null && !"null".equals(rawKeywords) && !rawKeywords.trim().isEmpty()) ? rawKeywords.trim() : null;

        int pageNum = searchDTO.getPage() != null ? searchDTO.getPage() : 1;
        int pageSize = searchDTO.getSize() != null ? searchDTO.getSize() : 10;

        LambdaQueryWrapper<Literature> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（标题、摘要、关键词）
        final String keywordFinal = keyword;
        if (StringUtils.hasText(keywordFinal)) {
            wrapper.and(w -> w
                    .like(Literature::getTitle, keywordFinal)
                    .or()
                    .like(Literature::getAbstractText, keywordFinal)
                    .or()
                    .like(Literature::getKeywords, keywordFinal)
            );
        }

        // 作者筛选
        final String authorFinal = author;
        if (StringUtils.hasText(authorFinal)) {
            wrapper.like(Literature::getAuthors, authorFinal);
        }

        // ========== 新增三个筛选条件 ==========
        // 期刊筛选
        final String journalFinal = journal;
        if (StringUtils.hasText(journalFinal)) {
            wrapper.like(Literature::getJournal, journalFinal);
        }

        // 分类筛选
        final String categoryFinal = category;
        if (StringUtils.hasText(categoryFinal)) {
            wrapper.eq(Literature::getCategory, categoryFinal);
        }

        // 关键词筛选（在文献的关键词字段中搜索）
        final String keywordsFilterFinal = keywordsFilter;
        if (StringUtils.hasText(keywordsFilterFinal)) {
            wrapper.like(Literature::getKeywords, keywordsFilterFinal);
        }
        // ====================================

        // 年份范围筛选
        Integer startYear = searchDTO.getStartYear();
        if (startYear != null) {
            wrapper.ge(Literature::getPublishYear, startYear);
        }

        Integer endYear = searchDTO.getEndYear();
        if (endYear != null) {
            wrapper.le(Literature::getPublishYear, endYear);
        }

        // 只查询已通过的文献（status=1）
        wrapper.eq(Literature::getStatus, 1);

        // 按更新时间倒序
        wrapper.orderByDesc(Literature::getUpdateTime);

        // 分页查询
        Page<Literature> page = new Page<>(pageNum, pageSize);
        Page<Literature> resultPage = literatureMapper.selectPage(page, wrapper);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("list", resultPage.getRecords());
        result.put("page", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        result.put("pages", resultPage.getPages());

        log.info("搜索完成，关键词: {}, 作者: {}, 期刊: {}, 分类: {}, 关键词筛选: {}, 共找到 {} 条记录",
                keywordFinal, authorFinal, journalFinal, categoryFinal, keywordsFilterFinal, resultPage.getTotal());

        return result;
    }

    @Override
    public Literature addLiterature(Literature literature, MultipartFile file) {
        // 设置默认值
        literature.setViewCount(0);
        literature.setFavoriteCount(0);
        literature.setStatus(0); // 改为 0 = 待审核（需要管理员审核）
        literature.setCreateTime(java.time.LocalDateTime.now());
        literature.setUpdateTime(java.time.LocalDateTime.now());

        // 如果有文件上传，处理文件保存逻辑
        if (file != null && !file.isEmpty()) {
            String fileUrl = fileUploadUtil.uploadFile(file);
            literature.setFileUrl(fileUrl);
            log.info("文件上传成功: {}", fileUrl);
        }

        literatureMapper.insert(literature);
        log.info("文献添加成功，ID: {}，状态: 待审核", literature.getId());
        return literature;
    }

    @Override
    public Literature updateLiterature(Long id, Literature literature, MultipartFile file) {
        // 先查询原文献
        Literature existingLiterature = literatureMapper.selectById(id);
        if (existingLiterature == null) {
            throw new RuntimeException("文献不存在");
        }

        // 设置更新时间
        literature.setId(id);
        literature.setUpdateTime(java.time.LocalDateTime.now());

        // 处理文件上传
        if (file != null && !file.isEmpty()) {
            // 删除旧文件
            if (existingLiterature.getFileUrl() != null && !existingLiterature.getFileUrl().isEmpty()) {
                fileUploadUtil.deleteFile(existingLiterature.getFileUrl());
                log.info("删除旧文件: {}", existingLiterature.getFileUrl());
            }
            // 上传新文件
            String fileUrl = fileUploadUtil.uploadFile(file);
            literature.setFileUrl(fileUrl);
            log.info("新文件上传成功: {}", fileUrl);
        } else {
            // 没有上传新文件，保留原文件路径
            literature.setFileUrl(existingLiterature.getFileUrl());
        }

        // 使用 LambdaUpdateWrapper 强制更新所有字段
        LambdaUpdateWrapper<Literature> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Literature::getId, id)
                .set(Literature::getTitle, literature.getTitle())
                .set(Literature::getAuthors, literature.getAuthors())
                .set(Literature::getAbstractText, literature.getAbstractText())
                .set(Literature::getPublishYear, literature.getPublishYear())
                .set(Literature::getJournal, literature.getJournal())
                .set(Literature::getKeywords, literature.getKeywords())
                .set(Literature::getDoi, literature.getDoi())
                .set(Literature::getCategory, literature.getCategory())
                .set(Literature::getFileUrl, literature.getFileUrl())
                .set(Literature::getUpdateTime, literature.getUpdateTime());

        literatureMapper.update(null, updateWrapper);

        log.info("更新文献成功，ID: {}, 作者: {}, 年份: {}", id, literature.getAuthors(), literature.getPublishYear());
        return literature;
    }

    @Override
    public LiteratureVO getById(Long id) {
        Literature literature = literatureMapper.selectById(id);
        if (literature == null) {
            return null;
        }
        LiteratureVO vo = new LiteratureVO();
        BeanUtils.copyProperties(literature, vo);
        vo.setAuthor(literature.getAuthors());
        vo.setYear(literature.getPublishYear());
        vo.setViewCount(literature.getViewCount());
        vo.setCategory(literature.getCategory());

        return vo;
    }
    @Override
    public void deleteById(Long id) {
        // 先查询文献获取文件路径
        Literature literature = literatureMapper.selectById(id);
        if (literature != null && literature.getFileUrl() != null && !literature.getFileUrl().isEmpty()) {
            // 删除关联的文件
            fileUploadUtil.deleteFile(literature.getFileUrl());
            log.info("删除文献关联文件: {}", literature.getFileUrl());
        }
        literatureMapper.deleteById(id);
        log.info("删除文献成功，ID: {}", id);
    }

    @Override
    public void syncToEs() {
        List<Literature> list = literatureMapper.selectList(null);
        for (Literature literature : list) {
            try {
                searchService.syncToEs(literature.getId());
            } catch (Exception e) {
                log.error("同步文献 {} 到ES失败: {}", literature.getId(), e.getMessage());
            }
        }
        log.info("同步完成，共同步 {} 条数据", list.size());
    }
}