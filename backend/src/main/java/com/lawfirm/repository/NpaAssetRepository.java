package com.lawfirm.repository;

import com.lawfirm.entity.NpaAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface NpaAssetRepository extends JpaRepository<NpaAsset, Long> {

    Page<NpaAsset> findByPackageIdAndDeletedFalse(Long packageId, Pageable pageable);

    List<NpaAsset> findByPackageIdAndDeletedFalse(Long packageId);

    long countByPackageIdAndDeletedFalse(Long packageId);

    Page<NpaAsset> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(String status);

    List<NpaAsset> findByStatusAndDeletedFalse(String status);

    List<NpaAsset> findByDebtorNameContainingAndDeletedFalse(String debtorName);

    /** 统计某一资产包的总债权额 */
    @Query("SELECT COALESCE(SUM(a.totalAmount), 0) FROM NpaAsset a WHERE a.packageId = ?1 AND a.deleted = false")
    BigDecimal sumTotalAmountByPackageId(Long packageId);

    /** 统计某一资产包的已回收金额 */
    @Query("SELECT COALESCE(SUM(a.recoveredAmount), 0) FROM NpaAsset a WHERE a.packageId = ?1 AND a.deleted = false")
    BigDecimal sumRecoveredAmountByPackageId(Long packageId);

    /** 统计某资产包处于某状态的债权数 */
    long countByPackageIdAndStatusAndDeletedFalse(Long packageId, String status);
}
