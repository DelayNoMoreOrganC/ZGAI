package com.lawfirm.repository;

import com.lawfirm.entity.OfficeSupplies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 办公用品Repository
 */
@Repository
public interface OfficeSuppliesRepository extends JpaRepository<OfficeSupplies, Long>,
        JpaSpecificationExecutor<OfficeSupplies> {

    /**
     * 按类别查询
     */
    List<OfficeSupplies> findByCategoryAndDeletedFalseOrderByStockQuantityAsc(String category);

    /**
     * 查询库存不足的物品
     */
    List<OfficeSupplies> findByStockQuantityLessThanAndDeletedFalse(Integer minStock);

    /**
     * 按名称模糊查询
     */
    List<OfficeSupplies> findByNameContainingIgnoreCaseAndDeletedFalse(String name);

    /**
     * 查询所有可用物品
     */
    List<OfficeSupplies> findByDeletedFalseOrderByCategoryAscNameAsc();
}
