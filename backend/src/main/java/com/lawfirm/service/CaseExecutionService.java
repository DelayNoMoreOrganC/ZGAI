package com.lawfirm.service;

import com.lawfirm.dto.CaseExecutionDTO;
import com.lawfirm.entity.CaseExecution;
import com.lawfirm.repository.CaseExecutionRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 案件执行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseExecutionService {

    private final CaseExecutionRepository repository;
    private final UserRepository userRepository;

    public CaseExecutionDTO create(CaseExecutionDTO dto, Long userId) {
        CaseExecution entity = new CaseExecution();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreatedBy(userId);
        CaseExecution saved = repository.save(entity);
        return toVO(saved);
    }

    public CaseExecutionDTO update(Long id, CaseExecutionDTO dto) {
        CaseExecution entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件执行记录不存在"));
        BeanUtils.copyProperties(dto, entity, "id", "caseId", "createdBy");
        CaseExecution saved = repository.save(entity);
        return toVO(saved);
    }

    @Transactional
    public void delete(Long id) {
        CaseExecution entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件执行记录不存在"));
        entity.setDeleted(true);
        repository.save(entity);
    }

    public CaseExecutionDTO getById(Long id) {
        CaseExecution entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件执行记录不存在"));
        return toVO(entity);
    }

    public List<CaseExecutionDTO> getByCaseId(Long caseId) {
        List<CaseExecution> entities = repository.findByCaseIdAndDeletedFalseOrderByExecutionDateDesc(caseId);
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private CaseExecutionDTO toVO(CaseExecution entity) {
        CaseExecutionDTO vo = new CaseExecutionDTO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getCreatedBy() != null) {
            userRepository.findById(entity.getCreatedBy()).ifPresent(user -> {
                vo.setCreatedByName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            });
        }
        return vo;
    }
}
