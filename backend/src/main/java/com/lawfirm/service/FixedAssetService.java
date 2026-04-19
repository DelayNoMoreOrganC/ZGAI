package com.lawfirm.service;

import com.lawfirm.dto.FixedAssetDTO;
import com.lawfirm.entity.FixedAsset;
import com.lawfirm.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 固定资产服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository repository;

    /**
     * 创建固定资产
     */
    public FixedAssetDTO create(FixedAssetDTO dto) {
        FixedAsset entity = new FixedAsset();
        BeanUtils.copyProperties(dto, entity);

        // 自动计算当前价值（如果有购买日期和折旧年限）
        if (dto.getPurchaseDate() != null && dto.getDepreciationYears() != null && dto.getPurchasePrice() != null) {
            BigDecimal currentValue = calculateDepreciation(
                    dto.getPurchasePrice(),
                    dto.getPurchaseDate(),
                    dto.getDepreciationYears(),
                    dto.getDepreciationMethod()
            );
            entity.setCurrentValue(currentValue);
        } else if (dto.getPurchasePrice() != null) {
            entity.setCurrentValue(dto.getPurchasePrice());
        }

        FixedAsset saved = repository.save(entity);
        log.info("创建固定资产成功: id={}, name={}, category={}", saved.getId(),
                saved.getAssetName(), saved.getAssetCategory());

        return toVO(saved);
    }

    /**
     * 更新固定资产
     */
    public FixedAssetDTO update(Long id, FixedAssetDTO dto) {
        FixedAsset entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("固定资产不存在"));

        BeanUtils.copyProperties(dto, entity, "id");

        // 重新计算当前价值
        if (dto.getPurchaseDate() != null && dto.getDepreciationYears() != null && dto.getPurchasePrice() != null) {
            BigDecimal currentValue = calculateDepreciation(
                    dto.getPurchasePrice(),
                    dto.getPurchaseDate(),
                    dto.getDepreciationYears(),
                    dto.getDepreciationMethod()
            );
            entity.setCurrentValue(currentValue);
        }

        FixedAsset saved = repository.save(entity);
        log.info("更新固定资产成功: id={}", id);

        return toVO(saved);
    }

    /**
     * 删除固定资产（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        FixedAsset entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("固定资产不存在"));

        entity.setDeleted(true);
        repository.save(entity);
        log.info("删除固定资产成功: id={}", id);
    }

    /**
     * 根据ID查询
     */
    public FixedAssetDTO getById(Long id) {
        FixedAsset entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("固定资产不存在"));
        return toVO(entity);
    }

    /**
     * 查询所有固定资产
     */
    public List<FixedAssetDTO> getAll() {
        List<FixedAsset> entities = repository.findByDeletedFalseOrderByAssetCategoryAscAssetNameAsc();
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 按类别查询
     */
    public List<FixedAssetDTO> getByCategory(String category) {
        List<FixedAsset> entities = repository.findByAssetCategoryAndDeletedFalseOrderByPurchaseDateDesc(category);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 按使用状态查询
     */
    public List<FixedAssetDTO> getByUsageStatus(String status) {
        List<FixedAsset> entities = repository.findByUsageStatusAndDeletedFalse(status);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 按部门查询
     */
    public List<FixedAssetDTO> getByDepartment(String department) {
        List<FixedAsset> entities = repository.findByDepartmentAndDeletedFalse(department);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 报废资产
     */
    @Transactional
    public FixedAssetDTO scrap(Long id, String remarks) {
        FixedAsset entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("固定资产不存在"));

        entity.setUsageStatus("SCRAPPED");
        entity.setRemarks(remarks);

        FixedAsset saved = repository.save(entity);
        log.info("资产报废成功: id={}", id);

        return toVO(saved);
    }

    /**
     * 计算折旧
     * 使用直线法：年折旧额 = (原值 - 残值) / 折旧年限
     */
    private BigDecimal calculateDepreciation(BigDecimal purchasePrice,
                                              LocalDate purchaseDate,
                                              Integer years,
                                              String method) {
        if (purchasePrice == null || years == null || years == 0) {
            return purchasePrice;
        }

        // 计算已使用年数
        int yearsUsed = Period.between(purchaseDate, LocalDate.now()).getYears();
        if (yearsUsed < 0) {
            return purchasePrice;
        }

        if ("STRAIGHT_LINE".equals(method)) {
            // 直线法：年折旧率 = 1 / 折旧年限
            BigDecimal depreciationRate = BigDecimal.ONE.divide(BigDecimal.valueOf(years), 4, RoundingMode.HALF_UP);
            BigDecimal annualDepreciation = purchasePrice.multiply(depreciationRate);

            // 累计折旧
            BigDecimal accumulatedDepreciation = annualDepreciation.multiply(BigDecimal.valueOf(yearsUsed));

            // 当前价值 = 原值 - 累计折旧
            BigDecimal currentValue = purchasePrice.subtract(accumulatedDepreciation);

            // 不能为负
            return currentValue.max(BigDecimal.ZERO);
        }

        return purchasePrice;
    }

    private FixedAssetDTO toVO(FixedAsset entity) {
        FixedAssetDTO vo = new FixedAssetDTO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
