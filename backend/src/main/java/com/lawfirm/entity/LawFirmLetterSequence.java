package com.lawfirm.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "law_firm_letter_sequence", uniqueConstraints = {
        @UniqueConstraint(name = "uk_law_firm_letter_sequence", columnNames = {"letter_year", "letter_type_code"})
})
public class LawFirmLetterSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "letter_year", nullable = false)
    private Integer letterYear;

    @Column(name = "letter_type_code", nullable = false, length = 10)
    private String letterTypeCode;

    @Column(name = "last_serial", nullable = false)
    private Integer lastSerial;

    @Column(name = "initialized_by", nullable = false)
    private Long initializedBy;

    @Column(name = "initialized_at", nullable = false)
    private LocalDateTime initializedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;
}
