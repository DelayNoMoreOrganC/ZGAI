package com.lawfirm.service;

import com.lawfirm.dto.NpaAssetDTO;
import com.lawfirm.entity.NpaAsset;
import com.lawfirm.entity.NpaPackage;
import com.lawfirm.repository.NpaAssetRepository;
import com.lawfirm.repository.NpaPackageRepository;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 债权管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpaAssetService {

    private final NpaAssetRepository npaAssetRepository;
    private final NpaPackageRepository npaPackageRepository;

    /**
     * 分页查询某资产包下的债权
     */
    public Page<NpaAssetDTO> listAssetsByPackage(Long packageId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return npaAssetRepository.findByPackageIdAndDeletedFalse(packageId, pageable)
                .map(this::toDTO);
    }

    /**
     * 分页查询全部债权
     */
    public Page<NpaAssetDTO> listAllAssets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return npaAssetRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(this::toDTO);
    }

    /**
     * 获取债权详情
     */
    public NpaAssetDTO getAsset(Long id) {
        NpaAsset asset = npaAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("债权不存在: " + id));
        return toDTO(asset);
    }

    /**
     * 创建债权
     */
    @Transactional
    public NpaAssetDTO createAsset(NpaAssetDTO dto) {
        // 校验资产包存在
        npaPackageRepository.findByIdAndDeletedFalse(dto.getPackageId())
                .orElseThrow(() -> new RuntimeException("资产包不存在: " + dto.getPackageId()));

        NpaAsset asset = new NpaAsset();
        BeanUtils.copyProperties(dto, asset, "id", "recoveredAmount", "recoveryRate");
        if (asset.getRecoveredAmount() == null) asset.setRecoveredAmount(BigDecimal.ZERO);
        if (asset.getStatus() == null) asset.setStatus("PENDING");
        if (asset.getRiskLevel() == null) asset.setRiskLevel("MEDIUM");
        if (asset.getLawsuitStatus() == null) asset.setLawsuitStatus("NOT_SUED");

        // 自动计算总额 = 本金 + 利息
        if (asset.getTotalAmount() == null) {
            BigDecimal principal = asset.getPrincipalAmount() != null ? asset.getPrincipalAmount() : BigDecimal.ZERO;
            BigDecimal interest = asset.getInterestAmount() != null ? asset.getInterestAmount() : BigDecimal.ZERO;
            asset.setTotalAmount(principal.add(interest));
        }

        asset = npaAssetRepository.save(asset);

        // 更新资产包的债权笔数和总额
        updatePackageStats(dto.getPackageId());

        return toDTO(asset);
    }

    /**
     * 更新债权
     */
    @Transactional
    public NpaAssetDTO updateAsset(Long id, NpaAssetDTO dto) {
        NpaAsset asset = npaAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("债权不存在: " + id));
        BeanUtils.copyProperties(dto, asset, "id", "packageId", "recoveredAmount", "recoveryRate");

        if (asset.getTotalAmount() == null) {
            BigDecimal principal = asset.getPrincipalAmount() != null ? asset.getPrincipalAmount() : BigDecimal.ZERO;
            BigDecimal interest = asset.getInterestAmount() != null ? asset.getInterestAmount() : BigDecimal.ZERO;
            asset.setTotalAmount(principal.add(interest));
        }

        asset = npaAssetRepository.save(asset);
        return toDTO(asset);
    }

    /**
     * 录入回收金额，自动更新债权状态和回收率
     */
    @Transactional
    public NpaAssetDTO recordRecovery(Long id, BigDecimal recoveredAmount) {
        NpaAsset asset = npaAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("债权不存在: " + id));
        asset.setRecoveredAmount(recoveredAmount);

        if (asset.getTotalAmount() != null && asset.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = recoveredAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(asset.getTotalAmount(), 2, RoundingMode.HALF_UP);
            asset.setRecoveryRate(rate);

            // 全额回收则自动结清
            if (rate.compareTo(BigDecimal.valueOf(99.99)) >= 0) {
                asset.setStatus("RECOVERED");
            }
        }

        asset = npaAssetRepository.save(asset);

        // 更新所属资产包统计
        updatePackageStats(asset.getPackageId());

        return toDTO(asset);
    }

    /**
     * 批量导入债权（供 Excel 导入使用）
     */
    @Transactional
    public List<NpaAssetDTO> batchCreateAssets(List<NpaAssetDTO> dtos) {
        return dtos.stream().map(this::createAsset).collect(Collectors.toList());
    }

    /**
     * 删除债权（逻辑删除）
     */
    @Transactional
    public void deleteAsset(Long id) {
        NpaAsset asset = npaAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("债权不存在: " + id));
        asset.setDeleted(true);
        npaAssetRepository.save(asset);
        updatePackageStats(asset.getPackageId());
    }

    /**
     * 按债务人搜索
     */
    public List<NpaAssetDTO> searchByDebtor(String keyword) {
        return npaAssetRepository.findByDebtorNameContainingAndDeletedFalse(keyword)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 更新资产包统计（债权笔数 + 总金额 + 已回收金额）
     */
    private void updatePackageStats(Long packageId) {
        npaPackageRepository.findByIdAndDeletedFalse(packageId).ifPresent(pkg -> {
            long count = npaAssetRepository.countByPackageIdAndDeletedFalse(packageId);
            BigDecimal totalAmount = npaAssetRepository.sumTotalAmountByPackageId(packageId);
            BigDecimal recoveredAmount = npaAssetRepository.sumRecoveredAmountByPackageId(packageId);
            pkg.setAssetCount((int) count);
            pkg.setTotalAmount(totalAmount);
            pkg.setRecoveredAmount(recoveredAmount);
            npaPackageRepository.save(pkg);
        });
    }

    private NpaAssetDTO toDTO(NpaAsset asset) {
        NpaAssetDTO dto = new NpaAssetDTO();
        BeanUtils.copyProperties(asset, dto);

        // 补充资产包名称
        npaPackageRepository.findByIdAndDeletedFalse(asset.getPackageId())
                .ifPresent(pkg -> dto.setPackageName(pkg.getPackageName()));

        return dto;
    }
}
