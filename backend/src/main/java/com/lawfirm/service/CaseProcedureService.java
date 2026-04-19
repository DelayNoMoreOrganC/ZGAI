package com.lawfirm.service;

import com.lawfirm.dto.CaseProcedureDTO;
import com.lawfirm.entity.CaseProcedure;
import com.lawfirm.repository.CaseProcedureRepository;
import com.lawfirm.vo.CaseProcedureVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 案件程序服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseProcedureService {

    private final CaseProcedureRepository caseProcedureRepository;

    /**
     * 创建案件程序
     */
    public CaseProcedure create(CaseProcedureDTO dto, Long caseId) {
        CaseProcedure procedure = new CaseProcedure();
        BeanUtils.copyProperties(dto, procedure);
        procedure.setCaseId(caseId);
        return caseProcedureRepository.save(procedure);
    }

    /**
     * 批量创建案件程序
     */
    @Transactional
    public List<CaseProcedure> batchCreate(List<CaseProcedureDTO> dtos, Long caseId) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(dto -> create(dto, caseId))
                .collect(Collectors.toList());
    }

    /**
     * 更新案件程序
     */
    public CaseProcedure update(Long id, CaseProcedureDTO dto) {
        CaseProcedure procedure = caseProcedureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件程序不存在"));

        BeanUtils.copyProperties(dto, procedure, "id", "caseId");
        return caseProcedureRepository.save(procedure);
    }

    /**
     * 删除案件程序（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        CaseProcedure procedure = caseProcedureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件程序不存在"));
        procedure.setDeleted(true);
        caseProcedureRepository.save(procedure);
    }

    /**
     * 根据ID查询
     */
    public CaseProcedureVO getById(Long id) {
        CaseProcedure procedure = caseProcedureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("案件程序不存在"));
        return toVO(procedure);
    }

    /**
     * 根据案件ID查询所有程序
     */
    public List<CaseProcedureVO> getByCaseId(Long caseId) {
        List<CaseProcedure> procedures = caseProcedureRepository.findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(caseId);
        return procedures.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private CaseProcedureVO toVO(CaseProcedure procedure) {
        CaseProcedureVO vo = new CaseProcedureVO();
        BeanUtils.copyProperties(procedure, vo);
        vo.setProcedureTypeDesc(getProcedureTypeDesc(procedure.getProcedureType()));
        vo.setCreatedAt(procedure.getCreatedAt().toString());
        return vo;
    }

    private String getProcedureTypeDesc(String type) {
        if (type == null) return null;
        switch (type) {
            case "FIRST_INSTANCE": return "一审";
            case "SECOND_INSTANCE": return "二审";
            case "RETRIAL": return "再审";
            case "EXECUTION": return "执行";
            case "ARBITRATION": return "仲裁";
            default: return type;
        }
    }
}
