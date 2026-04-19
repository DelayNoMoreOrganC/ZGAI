package com.lawfirm.repository;

import com.lawfirm.entity.PropertyPreservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyPreservationRepository extends JpaRepository<PropertyPreservation, Long> {
    List<PropertyPreservation> findByCaseIdAndDeletedFalseOrderByPreservationDateDesc(Long caseId);
}
