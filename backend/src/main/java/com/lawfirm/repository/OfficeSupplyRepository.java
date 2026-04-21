package com.lawfirm.repository;

import com.lawfirm.entity.OfficeSupply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 办公用品Repository
 */
@Repository
public interface OfficeSupplyRepository extends JpaRepository<OfficeSupply, Long>, JpaSpecificationExecutor<OfficeSupply> {

    /**
     * 查找未删除的用品
     */
    Optional<OfficeSupply> findByIdAndDeletedFalse(Long id);

    /**
     * 查找未删除的用品列表（分页）
     */
    Page<OfficeSupply> findByDeletedFalse(Pageable pageable);
}
