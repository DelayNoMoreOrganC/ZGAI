package com.lawfirm.repository;

import com.lawfirm.entity.NpaDisposalResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface NpaDisposalResultRepository extends JpaRepository<NpaDisposalResult, Long> {

    List<NpaDisposalResult> findByAssetIdAndDeletedFalse(Long assetId);

    Optional<NpaDisposalResult> findByIdAndDeletedFalse(Long id);

    @Query("SELECT COALESCE(SUM(r.actualRecovery), 0) FROM NpaDisposalResult r WHERE r.assetId = ?1 AND r.deleted = false AND r.status = 'COMPLETED'")
    BigDecimal sumActualRecoveryByAssetId(Long assetId);

    @Query("SELECT COALESCE(SUM(r.costAmount), 0) FROM NpaDisposalResult r WHERE r.assetId = ?1 AND r.deleted = false")
    BigDecimal sumCostByAssetId(Long assetId);

    List<NpaDisposalResult> findByStatusAndDeletedFalse(String status);
}
