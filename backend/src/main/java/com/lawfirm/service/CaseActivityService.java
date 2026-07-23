package com.lawfirm.service;

import com.lawfirm.dto.CaseActivityDTO;
import com.lawfirm.entity.CaseActivity;
import com.lawfirm.repository.CaseActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseActivityService {

    private final CaseActivityRepository repository;

    @Transactional
    public CaseActivityDTO create(Long caseId, String type, String title, String content,
                                  LocalDateTime occurredAt, String sourceType, Long sourceId,
                                  Long operatorId, String procedureStage, String metadataJson) {
        CaseActivity activity = new CaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(type);
        activity.setTitle(title);
        activity.setContent(content);
        activity.setOccurredAt(occurredAt == null ? LocalDateTime.now() : occurredAt);
        activity.setSourceType(sourceType);
        activity.setSourceId(sourceId);
        activity.setOperatorId(operatorId);
        activity.setProcedureStage(procedureStage);
        activity.setMetadataJson(metadataJson);
        return toDTO(repository.save(activity));
    }

    public List<CaseActivityDTO> list(Long caseId) {
        return repository.findByCaseIdAndDeletedFalseOrderByOccurredAtDesc(caseId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CaseActivityDTO toDTO(CaseActivity activity) {
        CaseActivityDTO dto = new CaseActivityDTO();
        BeanUtils.copyProperties(activity, dto);
        return dto;
    }
}
