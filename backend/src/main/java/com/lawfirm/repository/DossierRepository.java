package com.lawfirm.repository;

import com.lawfirm.entity.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
    List<Dossier> findByCaseIdOrderBySortOrder(Long caseId);
}
