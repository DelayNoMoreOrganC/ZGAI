package com.lawfirm.service;

import com.lawfirm.entity.Approval;
import com.lawfirm.enums.ApprovalStatus;
import com.lawfirm.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * 补偿历史审批通知，避免旧审批单因功能上线前未写入 notification 而在消息中心不可见。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalNotificationBackfillService implements CommandLineRunner {

    private static final String RELATED_TYPE_PENDING = "APPROVAL_PENDING";

    private final ApprovalRepository approvalRepository;
    private final NotificationService notificationService;

    @Override
    public void run(String... args) {
        int created = 0;
        for (Approval approval : approvalRepository.findByStatusOrderByApplyTimeDesc(ApprovalStatus.PENDING.getCode())) {
            if (approval.getCurrentApproverId() == null || Boolean.TRUE.equals(approval.getDeleted())) {
                continue;
            }
            boolean exists = notificationService.existsNotification(
                    approval.getCurrentApproverId(),
                    approval.getId(),
                    RELATED_TYPE_PENDING
            );
            if (exists) {
                continue;
            }
            notificationService.sendNotification(
                    approval.getCurrentApproverId(),
                    getNotificationTitle(approval),
                    "您有待处理审批：「" + approval.getTitle() + "」，请及时处理。",
                    NotificationService.CATEGORY_APPROVAL,
                    approval.getId(),
                    RELATED_TYPE_PENDING
            );
            created++;
        }
        if (created > 0) {
            log.info("历史审批通知补偿完成：新增 {} 条待审批通知", created);
        }
    }

    private String getNotificationTitle(Approval approval) {
        if ("CASE_FILING".equals(approval.getApprovalType())) {
            return "待处理立案审批";
        }
        if ("CASE_FILING_DIRECTOR".equals(approval.getApprovalType())) {
            return "待处理主任终审";
        }
        return "待处理审批";
    }
}
