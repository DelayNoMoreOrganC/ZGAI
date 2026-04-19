package com.lawfirm.repository;

import com.lawfirm.entity.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 固定资产Repository
 */
@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long>,
        JpaSpecificationExecutor<FixedAsset> {

    /**
     * 按类别查询
     */
    List<FixedAsset> findByAssetCategoryAndDeletedFalseOrderByPurchaseDateDesc(String assetCategory);

    /**
     * 按使用状态查询
     */
    List<FixedAsset> findByUsageStatusAndDeletedFalse(String usageStatus);

    /**
     * 按部门查询
     */
    List<FixedAsset> findByDepartmentAndDeletedFalse(String department);

    /**
     * 按资产名称模糊查询
     */
    List<FixedAsset> findByAssetNameContainingIgnoreCaseAndDeletedFalse(String assetName);

    /**
     * 查询所有可用资产
     */
    List<FixedAsset> findByDeletedFalseOrderByAssetCategoryAscAssetNameAsc();
}
