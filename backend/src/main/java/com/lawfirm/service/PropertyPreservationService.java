package com.lawfirm.service;

import com.lawfirm.dto.PropertyPreservationDTO;
import com.lawfirm.entity.PropertyPreservation;
import com.lawfirm.repository.PropertyPreservationRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 财产保全服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyPreservationService {

    private final PropertyPreservationRepository repository;
    private final UserRepository userRepository;

    public PropertyPreservationDTO create(PropertyPreservationDTO dto, Long userId) {
        PropertyPreservation entity = new PropertyPreservation();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreatedBy(userId);
        PropertyPreservation saved = repository.save(entity);
        return toVO(saved);
    }

    public PropertyPreservationDTO update(Long id, PropertyPreservationDTO dto) {
        PropertyPreservation entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("财产保全记录不存在"));
        BeanUtils.copyProperties(dto, entity, "id", "caseId", "createdBy");
        PropertyPreservation saved = repository.save(entity);
        return toVO(saved);
    }

    @Transactional
    public void delete(Long id) {
        PropertyPreservation entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("财产保全记录不存在"));
        entity.setDeleted(true);
        repository.save(entity);
    }

    public PropertyPreservationDTO getById(Long id) {
        PropertyPreservation entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("财产保全记录不存在"));
        return toVO(entity);
    }

    public List<PropertyPreservationDTO> getByCaseId(Long caseId) {
        List<PropertyPreservation> entities = repository.findByCaseIdAndDeletedFalseOrderByPreservationDateDesc(caseId);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private PropertyPreservationDTO toVO(PropertyPreservation entity) {
        PropertyPreservationDTO vo = new PropertyPreservationDTO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getCreatedBy() != null) {
            userRepository.findById(entity.getCreatedBy()).ifPresent(user -> {
                vo.setCreatedByName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            });
        }
        return vo;
    }
}
