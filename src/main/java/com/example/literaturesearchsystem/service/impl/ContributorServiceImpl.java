package com.example.literaturesearchsystem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.literaturesearchsystem.common.LiteratureStatusEnum;
import com.example.literaturesearchsystem.dto.LiteratureCorrectionDTO;
import com.example.literaturesearchsystem.dto.LiteratureUploadDTO;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.entity.LiteratureCorrection;
import com.example.literaturesearchsystem.mapper.LiteratureCorrectionMapper;
import com.example.literaturesearchsystem.mapper.LiteratureMapper;
import com.example.literaturesearchsystem.service.ContributorService;
import com.example.literaturesearchsystem.vo.ContributionVO;
import com.example.literaturesearchsystem.vo.CorrectionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributorServiceImpl implements ContributorService {

    private final LiteratureMapper literatureMapper;
    private final LiteratureCorrectionMapper correctionMapper;

    @Override
    @Transactional
    public Literature uploadLiterature(LiteratureUploadDTO uploadDTO, Long userId) {
        Literature literature = new Literature();
        BeanUtil.copyProperties(uploadDTO, literature);
        literature.setStatus(LiteratureStatusEnum.PENDING.getCode());
        literature.setContributorId(userId);
        literature.setViewCount(0);
        literature.setFavoriteCount(0);
        literature.setCreateTime(LocalDateTime.now());
        literature.setUpdateTime(LocalDateTime.now());

        literatureMapper.insert(literature);
        log.info("文献上传成功，ID: {}, 贡献者ID: {}", literature.getId(), userId);

        return literature;
    }

    @Override
    public Page<ContributionVO> getMyContributions(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Literature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Literature::getContributorId, userId);
        if (status != null) {
            wrapper.eq(Literature::getStatus, status);
        }
        wrapper.orderByDesc(Literature::getCreateTime);

        Page<Literature> literaturePage = literatureMapper.selectPage(new Page<>(page, size), wrapper);

        Page<ContributionVO> voPage = new Page<>(page, size, literaturePage.getTotal());
        List<ContributionVO> voList = literaturePage.getRecords().stream().map(lit -> {
            ContributionVO vo = new ContributionVO();
            BeanUtil.copyProperties(lit, vo);
            vo.setStatusDesc(LiteratureStatusEnum.getDescByCode(lit.getStatus()));
            vo.setStatusBadgeClass(LiteratureStatusEnum.getBadgeClass(lit.getStatus()));
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    @Transactional
    public Literature resubmitLiterature(Long literatureId, LiteratureUploadDTO uploadDTO, Long userId) {
        Literature literature = literatureMapper.selectById(literatureId);
        if (literature == null) {
            throw new RuntimeException("文献不存在");
        }
        if (!literature.getContributorId().equals(userId)) {
            throw new RuntimeException("只能重新提交自己的文献");
        }
        if (!literature.getStatus().equals(LiteratureStatusEnum.REJECTED.getCode())) {
            throw new RuntimeException("只有被驳回的文献才能重新提交");
        }

        BeanUtil.copyProperties(uploadDTO, literature);
        literature.setStatus(LiteratureStatusEnum.PENDING.getCode());
        literature.setReviewRemark(null);
        literature.setReviewerId(null);
        literature.setReviewTime(null);
        literature.setUpdateTime(LocalDateTime.now());

        literatureMapper.updateById(literature);
        log.info("文献重新提交成功，ID: {}", literatureId);

        return literature;
    }

    @Override
    @Transactional
    public Long submitCorrection(LiteratureCorrectionDTO correctionDTO, Long userId) {
        Literature original = literatureMapper.selectById(correctionDTO.getLiteratureId());
        if (original == null) {
            throw new RuntimeException("文献不存在");
        }
        if (!original.getStatus().equals(LiteratureStatusEnum.APPROVED.getCode())) {
            throw new RuntimeException("只能对已发布的文献提出修正建议");
        }

        List<LiteratureCorrection> pendingList = correctionMapper.selectPendingByLiteratureId(correctionDTO.getLiteratureId());
        if (!pendingList.isEmpty()) {
            throw new RuntimeException("该文献已有待审核的修正请求，请耐心等待");
        }

        Map<String, Object> correctionMap = new HashMap<>();
        if (StrUtil.isNotBlank(correctionDTO.getTitle())) {
            correctionMap.put("title", correctionDTO.getTitle());
        }
        if (StrUtil.isNotBlank(correctionDTO.getAuthors())) {
            correctionMap.put("authors", correctionDTO.getAuthors());
        }
        if (StrUtil.isNotBlank(correctionDTO.getAbstractText())) {
            correctionMap.put("abstractText", correctionDTO.getAbstractText());
        }
        if (correctionDTO.getPublishYear() != null) {
            correctionMap.put("publishYear", correctionDTO.getPublishYear());
        }
        if (StrUtil.isNotBlank(correctionDTO.getJournal())) {
            correctionMap.put("journal", correctionDTO.getJournal());
        }
        if (StrUtil.isNotBlank(correctionDTO.getKeywords())) {
            correctionMap.put("keywords", correctionDTO.getKeywords());
        }
        if (StrUtil.isNotBlank(correctionDTO.getDoi())) {
            correctionMap.put("doi", correctionDTO.getDoi());
        }
        if (StrUtil.isNotBlank(correctionDTO.getCategory())) {
            correctionMap.put("category", correctionDTO.getCategory());
        }

        if (correctionMap.isEmpty()) {
            throw new RuntimeException("请至少填写一个需要修正的字段");
        }

        LiteratureCorrection correction = new LiteratureCorrection();
        correction.setLiteratureId(correctionDTO.getLiteratureId());
        correction.setContributorId(userId);
        correction.setCorrectionData(JSONUtil.toJsonStr(correctionMap));
        correction.setStatus(0);
        correction.setCreateTime(LocalDateTime.now());

        correctionMapper.insert(correction);
        log.info("提交修正建议成功，ID: {}", correction.getId());

        return correction.getId();
    }

    @Override
    public Page<CorrectionVO> getMyCorrections(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<LiteratureCorrection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiteratureCorrection::getContributorId, userId);
        if (status != null) {
            wrapper.eq(LiteratureCorrection::getStatus, status);
        }
        wrapper.orderByDesc(LiteratureCorrection::getCreateTime);

        Page<LiteratureCorrection> correctionPage = correctionMapper.selectPage(new Page<>(page, size), wrapper);

        Page<CorrectionVO> voPage = new Page<>(page, size, correctionPage.getTotal());
        List<CorrectionVO> voList = correctionPage.getRecords().stream().map(corr -> {
            CorrectionVO vo = new CorrectionVO();
            BeanUtil.copyProperties(corr, vo);
            Literature literature = literatureMapper.selectById(corr.getLiteratureId());
            if (literature != null) {
                vo.setLiteratureTitle(literature.getTitle());
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public Object getContributionStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<Literature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Literature::getContributorId, userId);
        Integer totalCount = literatureMapper.selectCount(wrapper).intValue();

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Literature::getContributorId, userId).eq(Literature::getStatus, LiteratureStatusEnum.PENDING.getCode());
        Integer pendingCount = literatureMapper.selectCount(wrapper).intValue();

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Literature::getContributorId, userId).eq(Literature::getStatus, LiteratureStatusEnum.APPROVED.getCode());
        Integer approvedCount = literatureMapper.selectCount(wrapper).intValue();

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Literature::getContributorId, userId).eq(Literature::getStatus, LiteratureStatusEnum.REJECTED.getCode());
        Integer rejectedCount = literatureMapper.selectCount(wrapper).intValue();

        stats.put("totalCount", totalCount);
        stats.put("pendingCount", pendingCount);
        stats.put("approvedCount", approvedCount);
        stats.put("rejectedCount", rejectedCount);

        return stats;
    }
}