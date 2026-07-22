package com.lawfirm.repository;

import com.lawfirm.entity.LegacyMaterialSearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LegacyMaterialSearchResultRepository extends JpaRepository<LegacyMaterialSearchResult, Long> {
}
