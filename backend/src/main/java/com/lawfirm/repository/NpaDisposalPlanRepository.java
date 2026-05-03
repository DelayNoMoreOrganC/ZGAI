package com.lawfirm.repository;

import com.lawfirm.entity.NpaDisposalPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NpaDisposalPlanRepository extends JpaRepository<NpaDisposalPlan, Long> {

    List<NpaDisposalPlan> findByAssetIdAndDeletedFalse(Long assetId);

    Optional<NpaDisposalPlan> findByIdAndDeletedFalse(Long id);

    long countByAssetIdAndDeletedFalse(Long assetId);

    List<NpaDisposalPlan> findByStatusAndDeletedFalse(String status);

    List<NpaDisposalPlan> findByResponsiblePersonAndDeletedFalse(String responsiblePerson);
}
