package com.lawfirm.repository;

import com.lawfirm.entity.ClientSubjectRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientSubjectRelationRepository extends JpaRepository<ClientSubjectRelation, Long> {

    List<ClientSubjectRelation> findBySourceClientIdAndDeletedFalseOrderByCreatedAtDesc(Long sourceClientId);

    List<ClientSubjectRelation> findByTargetClientIdAndDeletedFalseOrderByCreatedAtDesc(Long targetClientId);

    List<ClientSubjectRelation> findByDeletedFalse();
}
