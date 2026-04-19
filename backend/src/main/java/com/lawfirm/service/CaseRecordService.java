package com.lawfirm.service;

import com.lawfirm.dto.CaseRecordDTO;
import com.lawfirm.entity.CaseRecord;
import com.lawfirm.repository.CaseRecordRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.vo.CaseRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 办案记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseRecordService {

    private final CaseRecordRepository caseRecordRepository;
    private final UserRepository userRepository;

    /**
     * 创建办案记录
     */
    public CaseRecord create(CaseRecordDTO dto, Long caseId, Long userId) {
        CaseRecord record = new CaseRecord();
        BeanUtils.copyProperties(dto, record);
        record.setCaseId(caseId);
        record.setCreatedBy(userId);

        // 处理附件列表
        if (dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            record.setAttachments(String.join(",", dto.getAttachmentUrls()));
        }

        return caseRecordRepository.save(record);
    }

    /**
     * 更新办案记录
     */
    public CaseRecord update(Long id, CaseRecordDTO dto) {
        CaseRecord record = caseRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("办案记录不存在"));

        BeanUtils.copyProperties(dto, record, "id", "caseId", "createdBy");

        // 处理附件列表
        if (dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            record.setAttachments(String.join(",", dto.getAttachmentUrls()));
        }

        return caseRecordRepository.save(record);
    }

    /**
     * 删除办案记录（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        CaseRecord record = caseRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("办案记录不存在"));
        record.setDeleted(true);
        caseRecordRepository.save(record);
    }

    /**
     * 根据ID查询
     */
    public CaseRecord getById(Long id) {
        return caseRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("办案记录不存在"));
    }

    /**
     * 根据案件ID查询所有记录
     */
    public List<CaseRecord> getByCaseId(Long caseId) {
        return caseRecordRepository.findByCaseIdAndDeletedFalseOrderByRecordDateDescCreatedAtDesc(caseId);
    }

    /**
     * 根据案件ID和阶段查询记录
     */
    public List<CaseRecord> getByCaseIdAndStage(Long caseId, String stage) {
        return caseRecordRepository.findByCaseIdAndStageAndDeletedFalse(caseId, stage);
    }

    /**
     * 统计案件总工时
     */
    public BigDecimal getTotalWorkHours(Long caseId) {
        List<CaseRecord> records = getByCaseId(caseId);
        return records.stream()
                .map(r -> r.getWorkHours() != null ? r.getWorkHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 转换为VO对象
     */
    public CaseRecordVO toVO(CaseRecord record) {
        if (record == null) {
            return null;
        }

        CaseRecordVO vo = new CaseRecordVO();
        BeanUtils.copyProperties(record, vo);

        // 字段映射：workHours -> hours
        vo.setHours(record.getWorkHours());

        // 字段映射：createdAt -> createTime
        vo.setCreateTime(record.getCreatedAt());
        vo.setUpdateTime(record.getUpdatedAt());

        // 获取创建人姓名
        if (record.getCreatedBy() != null) {
            userRepository.findById(record.getCreatedBy()).ifPresent(user -> {
                vo.setAuthorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            });
        }

        // 解析附件字符串为列表
        if (record.getAttachments() != null && !record.getAttachments().isEmpty()) {
            vo.setAttachments(List.of(record.getAttachments().split(",")));
        }

        // 默认完成状态为true（后续可根据业务逻辑调整）
        vo.setCompleted(true);

        return vo;
    }

    /**
     * 批量转换为VO对象
     */
    public List<CaseRecordVO> toVOList(List<CaseRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        // 批量获取用户信息以优化性能
        List<Long> userIds = records.stream()
                .map(CaseRecord::getCreatedBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getRealName() != null ? user.getRealName() : user.getUsername()
                ));

        return records.stream()
                .map(record -> {
                    CaseRecordVO vo = new CaseRecordVO();
                    BeanUtils.copyProperties(record, vo);
                    vo.setHours(record.getWorkHours());
                    vo.setCreateTime(record.getCreatedAt());
                    vo.setUpdateTime(record.getUpdatedAt());

                    // 设置创建人姓名
                    if (record.getCreatedBy() != null) {
                        vo.setAuthorName(userMap.get(record.getCreatedBy()));
                    }

                    // 解析附件字符串为列表
                    if (record.getAttachments() != null && !record.getAttachments().isEmpty()) {
                        vo.setAttachments(List.of(record.getAttachments().split(",")));
                    }

                    vo.setCompleted(true);
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
