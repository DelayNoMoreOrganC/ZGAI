package com.lawfirm.repository;

import com.lawfirm.entity.NpaPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NpaPackageRepository extends JpaRepository<NpaPackage, Long> {

    Page<NpaPackage> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Optional<NpaPackage> findByIdAndDeletedFalse(Long id);

    List<NpaPackage> findByStatusAndDeletedFalse(String status);

    List<NpaPackage> findByBankNameContainingAndDeletedFalse(String bankName);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(String status);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM NpaPackage p WHERE p.deleted = false")
    java.math.BigDecimal sumTotalAmountByDeletedFalse();

    @Query("SELECT COALESCE(SUM(p.recoveredAmount), 0) FROM NpaPackage p WHERE p.deleted = false AND p.status IN ('IN_PROGRESS', 'SETTLED')")
    java.math.BigDecimal sumRecoveredAmountByActivePackages();
}
