package com.example.literaturesearchsystem.controller;

import com.example.literaturesearchsystem.common.Result;
import com.example.literaturesearchsystem.dto.LiteratureCorrectionDTO;
import com.example.literaturesearchsystem.dto.LiteratureUploadDTO;
import com.example.literaturesearchsystem.entity.Literature;
import com.example.literaturesearchsystem.service.ContributorService;
import com.example.literaturesearchsystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/contributor")
@RequiredArgsConstructor
public class ContributorController {

    private final ContributorService contributorService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    private Long getCurrentUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserId(token);
    }

    private boolean isContributor() {
        Long userId = getCurrentUserId();
        return userId != null;
    }

    @PostMapping("/literature")
    public Result<Literature> uploadLiterature(@RequestBody LiteratureUploadDTO uploadDTO) {
        Long userId = getCurrentUserId();
        if (!isContributor()) {
            return Result.forbidden();
        }

        try {
            Literature literature = contributorService.uploadLiterature(uploadDTO, userId);
            return Result.successWithMsg("文献上传成功，等待审核", literature);
        } catch (Exception e) {
            log.error("上传文献失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my-contributions")
    public Result<Object> getMyContributions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = getCurrentUserId();
        if (!isContributor()) {
            return Result.forbidden();
        }

        try {
            var pageResult = contributorService.getMyContributions(userId, page, size, status);
            return Result.successWithPage(pageResult.getRecords(), pageResult.getTotal());
        } catch (Exception e) {
            log.error("获取贡献列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/my-contributions/{id}/resubmit")
    public Result<Literature> resubmitLiterature(@PathVariable Long id,
                                                 @RequestBody LiteratureUploadDTO uploadDTO) {
        Long userId = getCurrentUserId();
        if (!isContributor()) {
            return Result.forbidden();
        }

        try {
            Literature literature = contributorService.resubmitLiterature(id, uploadDTO, userId);
            return Result.successWithMsg("重新提交成功", literature);
        } catch (Exception e) {
            log.error("重新提交失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/correction")
    public Result<Long> submitCorrection(@RequestBody LiteratureCorrectionDTO correctionDTO) {
        Long userId = getCurrentUserId();
        if (!isContributor()) {
            return Result.forbidden();
        }

        try {
            Long correctionId = contributorService.submitCorrection(correctionDTO, userId);
            return Result.successWithMsg("修正建议已提交，等待审核", correctionId);
        } catch (Exception e) {
            log.error("提交修正建议失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my-corrections")
    public Result<Object> getMyCorrections(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = getCurrentUserId();
        if (!isContributor()) {
            return Result.forbidden();
        }

        try {
            var pageResult = contributorService.getMyCorrections(userId, page, size, status);
            return Result.successWithPage(pageResult.getRecords(), pageResult.getTotal());
        } catch (Exception e) {
            log.error("获取修正记录失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<Object> getContributionStats() {
        Long userId = getCurrentUserId();
        if (!isContributor()) {
            return Result.forbidden();
        }

        try {
            Object stats = contributorService.getContributionStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取贡献统计失败", e);
            return Result.error(e.getMessage());
        }
    }
}