package com.lawfirm.service;

import com.lawfirm.dto.NpaDisposalPlanDTO;
import com.lawfirm.dto.NpaDisposalResultDTO;
import com.lawfirm.entity.NpaAsset;
import com.lawfirm.entity.NpaDisposalPlan;
import com.lawfirm.entity.NpaDisposalResult;
import com.lawfirm.repository.NpaAssetRepository;
import com.lawfirm.repository.NpaDisposalPlanRepository;
import com.lawfirm.repository.NpaDisposalResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 不良资产处置服务（方案 + 结果）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpaDisposalService {

    private final NpaDisposalPlanRepository planRepository;
    private final NpaDisposalResultRepository resultRepository;
    private final NpaAssetRepository npaAssetRepository;
    private final NpaAssetService npaAssetService;

    // ========== 处置方案 ==========

    public List<NpaDisposalPlanDTO> listPlansByAsset(Long assetId) {
        return planRepository.findByAssetIdAndDeletedFalse(assetId)
                .stream().map(this::toPlanDTO).collect(Collectors.toList());
    }

    public NpaDisposalPlanDTO getPlan(Long id) {
        NpaDisposalPlan plan = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("处置方案不存在: " + id));
        return toPlanDTO(plan);
    }

    @Transactional
    public NpaDisposalPlanDTO createPlan(NpaDisposalPlanDTO dto) {
        npaAssetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new RuntimeException("债权不存在: " + dto.getAssetId()));

        NpaDisposalPlan plan = new NpaDisposalPlan();
        BeanUtils.copyProperties(dto, plan, "id");
        if (plan.getStatus() == null) plan.setStatus("PENDING_REVIEW");
        plan = planRepository.save(plan);

        // 更新债权状态
        npaAssetRepository.findById(dto.getAssetId()).ifPresent(asset -> {
            asset.setStatus("IN_PROGRESS");
            npaAssetRepository.save(asset);
        });

        return toPlanDTO(plan);
    }

    @Transactional
    public NpaDisposalPlanDTO updatePlan(Long id, NpaDisposalPlanDTO dto) {
        NpaDisposalPlan plan = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("处置方案不存在: " + id));
        BeanUtils.copyProperties(dto, plan, "id", "assetId");
        plan = planRepository.save(plan);
        return toPlanDTO(plan);
    }

    @Transactional
    public NpaDisposalPlanDTO approvePlan(Long id, String comment, boolean approved) {
        NpaDisposalPlan plan = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("处置方案不存在: " + id));
        plan.setStatus(approved ? "APPROVED" : "REJECTED");
        plan.setApprovalComment(comment);
        plan = planRepository.save(plan);
        return toPlanDTO(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        NpaDisposalPlan plan = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("处置方案不存在: " + id));
        plan.setDeleted(true);
        planRepository.save(plan);
    }

    // ========== 处置结果 ==========

    public List<NpaDisposalResultDTO> listResultsByAsset(Long assetId) {
        return resultRepository.findByAssetIdAndDeletedFalse(assetId)
                .stream().map(this::toResultDTO).collect(Collectors.toList());
    }

    public NpaDisposalResultDTO getResult(Long id) {
        NpaDisposalResult result = resultRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("处置结果不存在: " + id));
        return toResultDTO(result);
    }

    @Transactional
    public NpaDisposalResultDTO createResult(NpaDisposalResultDTO dto) {
        npaAssetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new RuntimeException("债权不存在: " + dto.getAssetId()));

        NpaDisposalResult result = new NpaDisposalResult();
        BeanUtils.copyProperties(dto, result, "id");

        // 自动计算回收率
        result.setNetRecovery(result.getActualRecovery().subtract(
                result.getCostAmount() != null ? result.getCostAmount() : BigDecimal.ZERO));

        NpaAsset asset = npaAssetRepository.findById(dto.getAssetId()).get();
        if (asset.getTotalAmount() != null && asset.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = result.getActualRecovery()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(asset.getTotalAmount(), 2, RoundingMode.HALF_UP);
            result.setRecoveryRate(rate);
        }

        result = resultRepository.save(result);

        // 回写债权实体的回收数据
        BigDecimal totalRecovered = resultRepository.sumActualRecoveryByAssetId(dto.getAssetId());
        npaAssetService.recordRecovery(dto.getAssetId(), totalRecovered);

        return toResultDTO(result);
    }

    /**
     * 获取全局处置统计
     */
    public java.util.Map<String, Object> getDisposalStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        List<NpaDisposalPlan> allPlans = planRepository.findAll();
        long pendingReview = allPlans.stream().filter(p -> "PENDING_REVIEW".equals(p.getStatus())).count();
        long approved = allPlans.stream().filter(p -> "APPROVED".equals(p.getStatus())).count();
        long executing = allPlans.stream().filter(p -> "IN_PROGRESS".equals(p.getStatus())).count();
        long completed = allPlans.stream().filter(p -> "COMPLETED".equals(p.getStatus())).count();

        stats.put("pendingReview", pendingReview);
        stats.put("approved", approved);
        stats.put("executing", executing);
        stats.put("completed", completed);
        stats.put("totalPlans", allPlans.size());

        return stats;
    }

    // ========== DTO 转换 ==========

    private NpaDisposalPlanDTO toPlanDTO(NpaDisposalPlan plan) {
        NpaDisposalPlanDTO dto = new NpaDisposalPlanDTO();
        BeanUtils.copyProperties(plan, dto);
        npaAssetRepository.findById(plan.getAssetId()).ifPresent(asset ->
                dto.setDebtorName(asset.getDebtorName()));
        return dto;
    }

    private NpaDisposalResultDTO toResultDTO(NpaDisposalResult result) {
        NpaDisposalResultDTO dto = new NpaDisposalResultDTO();
        BeanUtils.copyProperties(result, dto);
        npaAssetRepository.findById(result.getAssetId()).ifPresent(asset ->
                dto.setDebtorName(asset.getDebtorName()));
        return dto;
    }
}
