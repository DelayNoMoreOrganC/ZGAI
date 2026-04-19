package com.lawfirm.service;

import com.lawfirm.dto.AttendanceRecordDTO;
import com.lawfirm.entity.AttendanceRecord;
import com.lawfirm.repository.AttendanceRecordRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考勤记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceRecordService {

    private final AttendanceRecordRepository repository;
    private final UserRepository userRepository;

    /**
     * 创建考勤记录
     */
    public AttendanceRecordDTO create(AttendanceRecordDTO dto, Long currentUserId) {
        // 自动计算时长
        if (dto.getDuration() == null && dto.getStartDate() != null && dto.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
            dto.setDuration(BigDecimal.valueOf(days));
            dto.setDurationUnit("DAY");
        }

        // 设置申请人为当前用户
        if (dto.getUserId() == null) {
            dto.setUserId(currentUserId);
        }

        // 默认状态为待审批
        if (dto.getApprovalStatus() == null) {
            dto.setApprovalStatus("PENDING");
        }

        AttendanceRecord entity = new AttendanceRecord();
        BeanUtils.copyProperties(dto, entity);
        AttendanceRecord saved = repository.save(entity);

        log.info("创建考勤记录成功: id={}, type={}, user={}", saved.getId(),
                saved.getAttendanceType(), saved.getUserId());

        return toVO(saved);
    }

    /**
     * 更新考勤记录
     */
    public AttendanceRecordDTO update(Long id, AttendanceRecordDTO dto) {
        AttendanceRecord entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("考勤记录不存在"));

        // 只允许待审批的记录修改
        if (!"PENDING".equals(entity.getApprovalStatus())) {
            throw new RuntimeException("只能修改待审批的记录");
        }

        BeanUtils.copyProperties(dto, entity, "id", "userId", "approvalStatus", "approverId", "approvalTime");

        // 重新计算时长
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
            entity.setDuration(BigDecimal.valueOf(days));
            entity.setDurationUnit("DAY");
        }

        AttendanceRecord saved = repository.save(entity);
        log.info("更新考勤记录成功: id={}", id);

        return toVO(saved);
    }

    /**
     * 删除考勤记录（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        AttendanceRecord entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("考勤记录不存在"));

        // 只允许删除待审批的记录
        if (!"PENDING".equals(entity.getApprovalStatus())) {
            throw new RuntimeException("只能删除待审批的记录");
        }

        entity.setDeleted(true);
        repository.save(entity);
        log.info("删除考勤记录成功: id={}", id);
    }

    /**
     * 根据ID查询
     */
    public AttendanceRecordDTO getById(Long id) {
        AttendanceRecord entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("考勤记录不存在"));
        return toVO(entity);
    }

    /**
     * 查询用户的所有考勤记录
     */
    public List<AttendanceRecordDTO> getByUserId(Long userId) {
        // 如果userId为null，返回所有记录
        if (userId == null) {
            List<AttendanceRecord> entities = repository.findAll();
            return entities.stream()
                    .filter(e -> !e.getDeleted())
                    .map(this::toVO)
                    .collect(Collectors.toList());
        }

        List<AttendanceRecord> entities = repository.findByUserIdAndStartDateBetweenAndDeletedFalse(
                userId, LocalDate.of(2000, 1, 1), LocalDate.of(2099, 12, 31));
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询待审批的记录
     */
    public List<AttendanceRecordDTO> getPendingApprovals() {
        List<AttendanceRecord> entities = repository.findByApprovalStatusAndDeletedFalseOrderByCreatedAtDesc(
                "PENDING");
        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 审批考勤记录
     */
    @Transactional
    public AttendanceRecordDTO approve(Long id, String status, String comment, Long approverId) {
        AttendanceRecord entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("考勤记录不存在"));

        if (!"PENDING".equals(entity.getApprovalStatus())) {
            throw new RuntimeException("该记录已审批，不能重复操作");
        }

        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new RuntimeException("无效的审批状态");
        }

        entity.setApprovalStatus(status);
        entity.setApproverId(approverId);
        entity.setApprovalTime(LocalDateTime.now());
        entity.setApprovalComment(comment);

        AttendanceRecord saved = repository.save(entity);
        log.info("审批考勤记录: id={}, status={}, approver={}", id, status, approverId);

        return toVO(saved);
    }

    /**
     * 获取月度考勤统计
     */
    public List<AttendanceRecordDTO> getMonthlyStats(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<AttendanceRecord> entities = repository.findByUserIdAndStartDateBetweenAndDeletedFalse(
                userId, startDate, endDate);

        return entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private AttendanceRecordDTO toVO(AttendanceRecord entity) {
        AttendanceRecordDTO vo = new AttendanceRecordDTO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCreateTime(entity.getCreatedAt());

        // 填充用户名
        if (entity.getUserId() != null) {
            userRepository.findById(entity.getUserId()).ifPresent(user -> {
                vo.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            });
        }

        // 填充审批人名
        if (entity.getApproverId() != null) {
            userRepository.findById(entity.getApproverId()).ifPresent(user -> {
                vo.setApproverName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            });
        }

        return vo;
    }
}
