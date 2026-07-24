package com.lawfirm.repository;

import com.lawfirm.entity.LawFirmLetterSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface LawFirmLetterSequenceRepository extends JpaRepository<LawFirmLetterSequence, Long> {
    Optional<LawFirmLetterSequence> findByLetterYearAndLetterTypeCode(Integer year, String typeCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LawFirmLetterSequence s where s.letterYear = :year and s.letterTypeCode = :typeCode")
    Optional<LawFirmLetterSequence> findForUpdate(@Param("year") Integer year, @Param("typeCode") String typeCode);
}
