package com.lawfirm.service;

import com.lawfirm.dto.PartyDTO;
import com.lawfirm.entity.Party;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.PartyRepository;
import com.lawfirm.vo.PartyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 当事人服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;

    /**
     * 创建当事人
     */
    @Transactional(rollbackFor = Exception.class)
    public Party create(PartyDTO dto, Long caseId) {
        Party party = new Party();
        BeanUtils.copyProperties(dto, party);
        party.setCaseId(caseId);
        return partyRepository.save(party);
    }

    /**
     * 批量创建当事人
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Party> batchCreate(List<PartyDTO> dtos, Long caseId) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(dto -> create(dto, caseId))
                .collect(Collectors.toList());
    }

    /**
     * 更新当事人
     */
    @Transactional(rollbackFor = Exception.class)
    public Party update(Long id, PartyDTO dto) {
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("当事人", id));

        BeanUtils.copyProperties(dto, party, "id", "caseId");
        return partyRepository.save(party);
    }

    /**
     * 删除当事人（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("当事人", id));
        party.setDeleted(true);
        partyRepository.save(party);
    }

    /**
     * 根据ID查询
     */
    @Transactional(readOnly = true)
    public PartyVO getById(Long id) {
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("当事人", id));
        return toVO(party);
    }

    /**
     * 根据案件ID查询所有当事人
     */
    @Transactional(readOnly = true)
    public List<PartyVO> getByCaseId(Long caseId) {
        List<Party> parties = partyRepository.findByCaseIdAndDeletedFalse(caseId);
        return parties.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private PartyVO toVO(Party party) {
        PartyVO vo = new PartyVO();
        BeanUtils.copyProperties(party, vo);
        vo.setPartyTypeDesc(getPartyTypeDesc(party.getPartyType()));
        vo.setPartyRoleDesc(getPartyRoleDesc(party.getPartyRole()));
        vo.setCreatedAt(party.getCreatedAt().toString());
        return vo;
    }

    private String getPartyTypeDesc(String type) {
        if ("INDIVIDUAL".equals(type)) return "个人";
        if ("ORGANIZATION".equals(type)) return "单位";
        return type;
    }

    private String getPartyRoleDesc(String role) {
        switch (role) {
            case "PLAINTIFF": return "原告";
            case "DEFENDANT": return "被告";
            case "THIRD_PARTY": return "第三人";
            case "CO_PLAINTIFF": return "共同原告";
            case "CO_DEFENDANT": return "共同被告";
            case "APPLICANT": return "申请人";
            case "RESPONDENT": return "被申请人";
            default: return role;
        }
    }
}
