package com.example.literaturesearchsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.literaturesearchsystem.dto.LiteratureCorrectionDTO;
import com.example.literaturesearchsystem.dto.LiteratureUploadDTO;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.vo.ContributionVO;
import com.example.literaturesearchsystem.vo.CorrectionVO;

public interface ContributorService {

    Literature uploadLiterature(LiteratureUploadDTO uploadDTO, Long userId);

    Page<ContributionVO> getMyContributions(Long userId, Integer page, Integer size, Integer status);

    Literature resubmitLiterature(Long literatureId, LiteratureUploadDTO uploadDTO, Long userId);

    Long submitCorrection(LiteratureCorrectionDTO correctionDTO, Long userId);

    Page<CorrectionVO> getMyCorrections(Long userId, Integer page, Integer size, Integer status);

    Object getContributionStats(Long userId);
}