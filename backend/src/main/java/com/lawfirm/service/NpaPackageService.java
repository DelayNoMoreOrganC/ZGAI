package com.lawfirm.service;

import com.lawfirm.dto.NpaPackageDTO;
import com.lawfirm.entity.NpaPackage;
import com.lawfirm.repository.NpaPackageRepository;
import com.lawfirm.repository.NpaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产包管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpaPackageService {

    private final NpaPackageRepository npaPackageRepository;
    private final NpaAssetRepository npaAssetRepository;

    /**
     * 分页查询资产包
     */
    public Page<NpaPackageDTO> listPackages(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return npaPackageRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(this::toDTO);
    }

    /**
     * 获取资产包详情
     */
    public NpaPackageDTO getPackage(Long id) {
        NpaPackage pkg = npaPackageRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("资产包不存在: " + id));
        return toDTO(pkg);
    }

    /**
     * 创建资产包
     */
    @Transactional
    public NpaPackageDTO createPackage(NpaPackageDTO dto) {
        NpaPackage pkg = new NpaPackage();
        BeanUtils.copyProperties(dto, pkg, "id", "recoveredAmount", "costAmount");
        if (pkg.getRecoveredAmount() == null) pkg.setRecoveredAmount(BigDecimal.ZERO);
        if (pkg.getCostAmount() == null) pkg.setCostAmount(BigDecimal.ZERO);
        if (pkg.getStatus() == null) pkg.setStatus("PENDING");
        pkg = npaPackageRepository.save(pkg);
        return toDTO(pkg);
    }

    /**
     * 更新资产包
     */
    @Transactional
    public NpaPackageDTO updatePackage(Long id, NpaPackageDTO dto) {
        NpaPackage pkg = npaPackageRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("资产包不存在: " + id));
        BeanUtils.copyProperties(dto, pkg, "id", "packageCode", "createdBy", "recoveredAmount", "costAmount");
        pkg = npaPackageRepository.save(pkg);
        return toDTO(pkg);
    }

    /**
     * 删除资产包（逻辑删除）
     */
    @Transactional
    public void deletePackage(Long id) {
        NpaPackage pkg = npaPackageRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("资产包不存在: " + id));
        pkg.setDeleted(true);
        npaPackageRepository.save(pkg);
    }

    /**
     * 获取全局统计概览
     */
    public java.util.Map<String, Object> getOverviewStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        long totalPackages = npaPackageRepository.countByDeletedFalse();
        long totalAssets = npaAssetRepository.countByDeletedFalse();
        BigDecimal totalAmount = npaPackageRepository.sumTotalAmountByDeletedFalse();
        BigDecimal totalRecovered = npaPackageRepository.sumRecoveredAmountByActivePackages();

        stats.put("totalPackages", totalPackages);
        stats.put("totalAssets", totalAssets);
        stats.put("totalAmount", totalAmount);
        stats.put("totalRecovered", totalRecovered);
        stats.put("recoveryRate", totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalRecovered.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 按状态统计
        long pending = npaPackageRepository.countByStatusAndDeletedFalse("PENDING");
        long inProgress = npaPackageRepository.countByStatusAndDeletedFalse("IN_PROGRESS");
        long settled = npaPackageRepository.countByStatusAndDeletedFalse("SETTLED");
        stats.put("pendingCount", pending);
        stats.put("inProgressCount", inProgress);
        stats.put("settledCount", settled);

        return stats;
    }

    /**
     * 转为 DTO（含计算字段）
     */
    private NpaPackageDTO toDTO(NpaPackage pkg) {
        NpaPackageDTO dto = new NpaPackageDTO();
        BeanUtils.copyProperties(pkg, dto);

        // 计算回收率
        if (pkg.getTotalAmount() != null && pkg.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                && pkg.getRecoveredAmount() != null) {
            BigDecimal rate = pkg.getRecoveredAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(pkg.getTotalAmount(), 2, RoundingMode.HALF_UP);
            dto.setRecoveryRate(rate);
        } else {
            dto.setRecoveryRate(BigDecimal.ZERO);
        }

        // 计算处置天数
        if (pkg.getAcquisitionDate() != null) {
            LocalDate end = pkg.getDeadlineDate() != null ? pkg.getDeadlineDate() : LocalDate.now();
            dto.setDisposalDays(ChronoUnit.DAYS.between(pkg.getAcquisitionDate(), end));
        }

        return dto;
    }
}
