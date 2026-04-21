package com.lawfirm.service;

import com.lawfirm.dto.CasePersonnelDTO;
import com.lawfirm.entity.CasePersonnel;
import com.lawfirm.repository.CasePersonnelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 案件承办人员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CasePersonnelService {

    private final CasePersonnelRepository personnelRepository;

    /**
     * 根据案件ID获取承办人员列表
     */
    public List<CasePersonnelDTO> getByCaseId(Long caseId) {
        List<CasePersonnel> personnelList = personnelRepository.findByCaseIdOrderByCreatedAtDesc(caseId);
        return personnelList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建承办人员
     */
    public CasePersonnelDTO create(Long caseId, CasePersonnelDTO dto) {
        CasePersonnel personnel = new CasePersonnel();
        personnel.setCaseId(caseId);
        personnel.setName(dto.getName());
        personnel.setPosition(dto.getPosition());
        personnel.setPhone(dto.getPhone());
        personnel.setCourt(dto.getCourt());
        personnel.setDepartment(dto.getDepartment());
        personnel.setRemark(dto.getRemark());

        CasePersonnel saved = personnelRepository.save(personnel);
        log.info("创建案件承办人员成功: caseId={}, personnelId={}", caseId, saved.getId());
        return toDTO(saved);
    }

    /**
     * 更新承办人员
     */
    public CasePersonnelDTO update(Long caseId, Long personnelId, CasePersonnelDTO dto) {
        CasePersonnel personnel = personnelRepository.findById(personnelId)
                .orElseThrow(() -> new RuntimeException("承办人员不存在"));

        if (!personnel.getCaseId().equals(caseId)) {
            throw new RuntimeException("承办人员不属于该案件");
        }

        personnel.setName(dto.getName());
        personnel.setPosition(dto.getPosition());
        personnel.setPhone(dto.getPhone());
        personnel.setCourt(dto.getCourt());
        personnel.setDepartment(dto.getDepartment());
        personnel.setRemark(dto.getRemark());

        CasePersonnel updated = personnelRepository.save(personnel);
        log.info("更新案件承办人员成功: caseId={}, personnelId={}", caseId, personnelId);
        return toDTO(updated);
    }

    /**
     * 删除承办人员
     */
    @Transactional
    public void delete(Long caseId, Long personnelId) {
        CasePersonnel personnel = personnelRepository.findById(personnelId)
                .orElseThrow(() -> new RuntimeException("承办人员不存在"));

        if (!personnel.getCaseId().equals(caseId)) {
            throw new RuntimeException("承办人员不属于该案件");
        }

        personnelRepository.delete(personnel);
        log.info("删除案件承办人员成功: caseId={}, personnelId={}", caseId, personnelId);
    }

    /**
     * 删除案件的所有承办人员
     */
    @Transactional
    public void deleteByCaseId(Long caseId) {
        personnelRepository.deleteByCaseId(caseId);
        log.info("删除案件所有承办人员成功: caseId={}", caseId);
    }

    private CasePersonnelDTO toDTO(CasePersonnel entity) {
        CasePersonnelDTO dto = new CasePersonnelDTO();
        dto.setId(entity.getId());
        dto.setCaseId(entity.getCaseId());
        dto.setName(entity.getName());
        dto.setPosition(entity.getPosition());
        dto.setPhone(entity.getPhone());
        dto.setCourt(entity.getCourt());
        dto.setDepartment(entity.getDepartment());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
