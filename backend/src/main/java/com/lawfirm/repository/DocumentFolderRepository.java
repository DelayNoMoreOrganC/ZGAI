package com.lawfirm.repository;

import com.lawfirm.entity.DocumentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

    List<DocumentFolder> findByFolderTypeAndActiveTrueAndDeletedFalseOrderBySortOrderAsc(String folderType);

    List<DocumentFolder> findByCaseIdAndDeletedFalseOrderBySortOrderAsc(Long caseId);

    Optional<DocumentFolder> findByCaseIdAndFolderPathAndDeletedFalse(Long caseId, String folderPath);

    boolean existsByFolderTypeAndFolderCodeAndDeletedFalse(String folderType, String folderCode);
}
