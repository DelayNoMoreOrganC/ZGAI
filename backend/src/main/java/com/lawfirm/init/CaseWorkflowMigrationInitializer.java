package com.lawfirm.init;

import com.lawfirm.entity.Case;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.service.CaseStageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 将尚在立案审批中的旧通用流程安全迁移为案件类型专属流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(30)
public class CaseWorkflowMigrationInitializer implements CommandLineRunner {

    private final CaseRepository caseRepository;
    private final CaseStageService caseStageService;

    @Override
    public void run(String... args) {
        int migrated = 0;
        for (Case caseEntity : caseRepository.findByDeletedFalse()) {
            if (caseStageService.reconcilePendingApprovalWorkflow(caseEntity)) {
                migrated++;
            }
        }
        if (migrated > 0) {
            log.info("已将 {} 个立案审批中案件迁移为类型化办理流程", migrated);
        }
    }
}
