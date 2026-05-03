package com.lawfirm.repository;

import com.lawfirm.entity.NpaDueDiligence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NpaDueDiligenceRepository extends JpaRepository<NpaDueDiligence, Long> {

    List<NpaDueDiligence> findByAssetIdAndDeletedFalse(Long assetId);

    Optional<NpaDueDiligence> findByIdAndDeletedFalse(Long id);

    long countByAssetIdAndDeletedFalse(Long assetId);

    List<NpaDueDiligence> findByInvestigatorAndDeletedFalse(String investigator);
}
