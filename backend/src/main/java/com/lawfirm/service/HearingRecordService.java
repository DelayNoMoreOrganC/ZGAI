package com.lawfirm.service;

import com.lawfirm.dto.HearingRecordDTO;
import com.lawfirm.entity.HearingRecord;
import com.lawfirm.repository.HearingRecordRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 庭审记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HearingRecordService {

    private final HearingRecordRepository repository;
    private final UserRepository userRepository;

    public HearingRecordDTO create(HearingRecordDTO dto, Long userId) {
        HearingRecord entity = new HearingRecord();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreatedBy(userId);
        HearingRecord saved = repository.save(entity);
        return toVO(saved);
    }

    public HearingRecordDTO update(Long id, HearingRecordDTO dto) {
        HearingRecord entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("庭审记录不存在"));
        BeanUtils.copyProperties(dto, entity, "id", "caseId", "createdBy");
        HearingRecord saved = repository.save(entity);
        return toVO(saved);
    }

    @Transactional
    public void delete(Long id) {
        HearingRecord entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("庭审记录不存在"));
        entity.setDeleted(true);
        repository.save(entity);
    }

    public HearingRecordDTO getById(Long id) {
        HearingRecord entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("庭审记录不存在"));
        return toVO(entity);
    }

    public List<HearingRecordDTO> getByCaseId(Long caseId) {
        List<HearingRecord> entities = repository.findByCaseIdAndDeletedFalseOrderByHearingDateDesc(caseId);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private HearingRecordDTO toVO(HearingRecord entity) {
        HearingRecordDTO vo = new HearingRecordDTO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getCreatedBy() != null) {
            userRepository.findById(entity.getCreatedBy()).ifPresent(user -> {
                vo.setCreatedByName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            });
        }
        return vo;
    }
}
