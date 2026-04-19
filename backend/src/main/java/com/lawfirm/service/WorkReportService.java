package com.lawfirm.service;

import com.lawfirm.dto.WorkReportDTO;
import com.lawfirm.entity.WorkReport;
import com.lawfirm.repository.WorkReportRepository;
import com.lawfirm.vo.WorkReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作汇报Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkReportService {

    private final WorkReportRepository workReportRepository;

    /**
     * 创建汇报
     */
    @Transactional
    public WorkReportVO createReport(WorkReportDTO dto) {
        WorkReport report = new WorkReport();
        report.setTitle(dto.getTitle());
        report.setReportDate(dto.getReportDate());
        report.setReportType(dto.getReportType());
        report.setContent(dto.getContent());
        report.setWorkSummary(dto.getWorkSummary());
        report.setNextPlan(dto.getNextPlan());
        report.setProblems(dto.getProblems());
        report.setSuggestions(dto.getSuggestions());
        report.setDepartment(dto.getDepartment());
        report.setStatus(dto.getStatus() != null ? dto.getStatus() : "DRAFT");

        // 设置汇报人
        Long userId = getCurrentUserId();
        String username = getCurrentUsername();
        report.setReporterId(userId);
        report.setReporterName(username);

        WorkReport saved = workReportRepository.save(report);
        log.info("创建工作汇报成功: id={}, title={}", saved.getId(), saved.getTitle());
        return toVO(saved);
    }

    /**
     * 更新汇报
     */
    @Transactional
    public WorkReportVO updateReport(Long id, WorkReportDTO dto) {
        WorkReport report = workReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("汇报不存在"));

        // 只能更新草稿状态的汇报
        if (!"DRAFT".equals(report.getStatus())) {
            throw new RuntimeException("只能更新草稿状态的汇报");
        }

        report.setTitle(dto.getTitle());
        report.setReportDate(dto.getReportDate());
        report.setReportType(dto.getReportType());
        report.setContent(dto.getContent());
        report.setWorkSummary(dto.getWorkSummary());
        report.setNextPlan(dto.getNextPlan());
        report.setProblems(dto.getProblems());
        report.setSuggestions(dto.getSuggestions());
        report.setDepartment(dto.getDepartment());

        WorkReport updated = workReportRepository.save(report);
        log.info("更新工作汇报成功: id={}, title={}", updated.getId(), updated.getTitle());
        return toVO(updated);
    }

    /**
     * 提交汇报
     */
    @Transactional
    public void submitReport(Long id) {
        WorkReport report = workReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("汇报不存在"));

        report.setStatus("SUBMITTED");
        workReportRepository.save(report);
        log.info("提交工作汇报成功: id={}", id);
    }

    /**
     * 审核汇报
     */
    @Transactional
    public void reviewReport(Long id, String status, String comment) {
        WorkReport report = workReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("汇报不存在"));

        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new RuntimeException("只能审核已提交的汇报");
        }

        report.setStatus(status); // APPROVED or REJECTED
        report.setReviewComment(comment);

        Long reviewerId = getCurrentUserId();
        String reviewerName = getCurrentUsername();
        report.setReviewerId(reviewerId);
        report.setReviewerName(reviewerName);
        report.setReviewedAt(java.time.LocalDateTime.now());

        workReportRepository.save(report);
        log.info("审核工作汇报成功: id={}, status={}", id, status);
    }

    /**
     * 删除汇报
     */
    @Transactional
    public void deleteReport(Long id) {
        WorkReport report = workReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("汇报不存在"));

        // 只能删除草稿状态的汇报
        if (!"DRAFT".equals(report.getStatus())) {
            throw new RuntimeException("只能删除草稿状态的汇报");
        }

        report.setDeleted(true);
        workReportRepository.save(report);
        log.info("删除工作汇报成功: id={}", id);
    }

    /**
     * 获取汇报详情
     */
    @Transactional(readOnly = true)
    public WorkReportVO getReport(Long id) {
        WorkReport report = workReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("汇报不存在"));
        return toVO(report);
    }

    /**
     * 汇报列表
     */
    @Transactional(readOnly = true)
    public Page<WorkReportVO> listReports(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate"));
        Page<WorkReport> reports;

        if (status != null && !status.isEmpty()) {
            reports = workReportRepository.findByStatusAndDeletedFalseOrderByReportDateDesc(status, pageable);
        } else {
            reports = workReportRepository.findAll(pageable);
        }

        return reports.map(this::toVO);
    }

    /**
     * 我的汇报列表
     */
    @Transactional(readOnly = true)
    public Page<WorkReportVO> getMyReports(int page, int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate"));
        Page<WorkReport> reports = workReportRepository.findByReporterIdAndDeletedFalseOrderByReportDateDesc(userId, pageable);
        return reports.map(this::toVO);
    }

    /**
     * 待审核汇报列表
     */
    @Transactional(readOnly = true)
    public Page<WorkReportVO> getPendingReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate"));
        Page<WorkReport> reports = workReportRepository.findByStatusAndDeletedFalseOrderByReportDateDesc("SUBMITTED", pageable);
        return reports.map(this::toVO);
    }

    /**
     * 部门汇报列表
     */
    @Transactional(readOnly = true)
    public List<WorkReportVO> getDepartmentReports(String department) {
        List<WorkReport> reports = workReportRepository.findByDepartmentAndDeletedFalseOrderByReportDateDesc(department);
        return reports.stream().map(this::toVO).collect(Collectors.toList());
    }

    private WorkReportVO toVO(WorkReport entity) {
        WorkReportVO vo = new WorkReportVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setReportDate(entity.getReportDate());
        vo.setReportType(entity.getReportType());
        vo.setContent(entity.getContent());
        vo.setWorkSummary(entity.getWorkSummary());
        vo.setNextPlan(entity.getNextPlan());
        vo.setProblems(entity.getProblems());
        vo.setSuggestions(entity.getSuggestions());
        vo.setReporterId(entity.getReporterId());
        vo.setReporterName(entity.getReporterName());
        vo.setDepartment(entity.getDepartment());
        vo.setStatus(entity.getStatus());
        vo.setReviewerId(entity.getReviewerId());
        vo.setReviewerName(entity.getReviewerName());
        vo.setReviewComment(entity.getReviewComment());
        vo.setReviewedAt(entity.getReviewedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return 1L; // 简化实现
        }
        return 1L;
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return "admin";
    }
}
