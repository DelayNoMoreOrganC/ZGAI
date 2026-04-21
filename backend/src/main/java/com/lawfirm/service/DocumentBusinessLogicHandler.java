package com.lawfirm.service;

import com.lawfirm.dto.*;
import com.lawfirm.entity.Case;
import com.lawfirm.exception.AIServiceException;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.vo.CaseDetailVO;
import com.lawfirm.vo.WorkReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * AI文档识别业务逻辑处理器
 * 负责根据识别结果自动执行业务操作
 *
 * 核心功能（按优先级）：
 * 1. 判决书处理 - 案件归类/创建 → 工作日志 → 待办提醒 → 日程安排
 * 2. 起诉状处理 - 创建草稿案件 → 待办提醒
 * 3. 答辩状处理 - 更新案件状态 → 创建确认待办
 * 4. 调解书处理 - 更新案件状态 → 设置履行期限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentBusinessLogicHandler {

    private final CaseService caseService;
    private final TodoService todoService;
    private final CalendarService calendarService;
    private final WorkReportService workReportService;
    private final CaseRepository caseRepository;

    /**
     * 处理判决书识别结果
     *
     * 业务逻辑流程：
     * 1. 根据案号查找或创建案件
     * 2. 创建工作日志：记录签收判决书
     * 3. 创建待办提醒：上诉期到期日
     * 4. 创建日程提醒：上诉期到期日
     *
     * @param result AI识别结果
     * @param userId 当前用户ID
     * @return 处理结果（创建的案件、待办、日程、日志信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleJudgment(AIDocumentRecognitionResult result, Long userId) {
        log.info("开始处理判决书识别结果: 案号={}, 法院={}", result.getCaseNumber(), result.getCourtName());

        Map<String, Object> processResult = new HashMap<>();

        try {
            // ========== 步骤1: 查找或创建案件 ==========
            Case relatedCase = findOrCreateCaseByJudgment(result, userId);
            processResult.put("caseId", relatedCase.getId());
            processResult.put("caseName", relatedCase.getCaseName());
            log.info("步骤1完成: 案件ID={}, 案件名称={}", relatedCase.getId(), relatedCase.getCaseName());

            // ========== 步骤2: 创建工作日志 ==========
            WorkReportDTO workReport = createWorkLogForJudgment(result, relatedCase.getId(), userId);
            WorkReportVO createdReport = workReportService.createReport(workReport);
            processResult.put("workReportId", createdReport.getId());
            log.info("步骤2完成: 工作日志ID={}", createdReport.getId());

            // ========== 步骤3: 计算上诉期并创建待办 ==========
            LocalDateTime appealDeadline = calculateAppealDeadline(result.getJudgmentDate());
            TodoDTO todo = createAppealDeadlineTodo(result, relatedCase.getId(), appealDeadline, userId);
            TodoDTO createdTodo = todoService.createTodo(todo, userId);
            processResult.put("todoId", createdTodo.getId());
            processResult.put("appealDeadline", appealDeadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            log.info("步骤3完成: 待办ID={}, 上诉期到期日={}", createdTodo.getId(), appealDeadline);

            // ========== 步骤4: 创建日程提醒 ==========
            CalendarDTO calendar = createAppealDeadlineCalendar(result, relatedCase.getId(), appealDeadline, userId);
            CalendarDTO createdCalendar = calendarService.createCalendar(calendar, userId);
            processResult.put("calendarId", createdCalendar.getId());
            log.info("步骤4完成: 日程ID={}", createdCalendar.getId());

            processResult.put("success", true);
            processResult.put("message", "判决书处理成功");

        } catch (Exception e) {
            log.error("处理判决书失败", e);
            throw new AIServiceException("处理判决书失败: " + e.getMessage(), e);
        }

        return processResult;
    }

    /**
     * 根据判决书信息查找或创建案件
     */
    private Case findOrCreateCaseByJudgment(AIDocumentRecognitionResult result, Long userId) {
        // 先尝试根据案号查找
        if (result.getCaseNumber() != null && !result.getCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> existingCase = caseRepository.findByCaseNumberAndDeletedFalse(result.getCaseNumber());
            if (existingCase.isPresent()) {
                Case caseEntity = existingCase.get();
                log.info("找到已存在案件: 案号={}, 案件ID={}", result.getCaseNumber(), caseEntity.getId());
                return caseEntity;
            }
        }

        // 未找到案件，创建新案件
        log.info("未找到已存在案件，创建新案件: 案号={}", result.getCaseNumber());
        CaseCreateRequest createRequest = buildCaseCreateRequest(result, userId);

        // CaseService.createCase 返回 CaseDetailVO，需要获取ID后查询实体
        CaseDetailVO caseVO = caseService.createCase(createRequest, userId);
        Case newCase = caseRepository.findById(caseVO.getId())
            .orElseThrow(() -> new AIServiceException("创建案件后无法查询到案件实体"));

        log.info("新案件创建成功: 案号={}, 案件ID={}", newCase.getCaseNumber(), newCase.getId());
        return newCase;
    }

    /**
     * 构建案件创建请求
     */
    private CaseCreateRequest buildCaseCreateRequest(AIDocumentRecognitionResult result, Long userId) {
        CaseCreateRequest createRequest = new CaseCreateRequest();

        // 案件名称：原告 vs 被告 + 案由
        String caseName = buildCaseName(result);
        createRequest.setCaseName(caseName);

        // 案号
        createRequest.setCaseNumber(result.getCaseNumber());

        // 案件类型：根据案由推断
        String caseType = inferCaseType(result.getCaseReason());
        createRequest.setCaseType(caseType);

        // 案件程序：默认一审
        createRequest.setProcedure("FIRST_INSTANCE");

        // 案由
        createRequest.setCaseReason(result.getCaseReason());

        // 案件等级：默认一般
        createRequest.setLevel("GENERAL");

        // 主办律师：当前用户
        createRequest.setOwnerId(userId);

        // 法院
        createRequest.setCourt(result.getCourtName());

        // 标签
        createRequest.setTags("AI识别,判决书");

        return createRequest;
    }

    /**
     * 创建签收判决书的工作日志
     */
    private WorkReportDTO createWorkLogForJudgment(AIDocumentRecognitionResult result, Long caseId, Long userId) {
        WorkReportDTO workReport = new WorkReportDTO();

        // 标题：签收{案号}判决书
        String title = String.format("签收%s判决书",
            result.getCaseNumber() != null ? result.getCaseNumber() : "相关案件");
        workReport.setTitle(title);

        // 报告日期：当前日期时间
        workReport.setReportDate(LocalDateTime.now());

        // 报告类型：工作日志
        workReport.setReportType("DAILY");

        // 工作内容
        StringBuilder content = new StringBuilder();
        content.append("## 签收法院文书\n\n");
        content.append("**文书类型**: 判决书\n\n");
        content.append("**案号**: ").append(result.getCaseNumber() != null ? result.getCaseNumber() : "未知").append("\n\n");
        content.append("**法院**: ").append(result.getCourtName() != null ? result.getCourtName() : "未知").append("\n\n");
        content.append("**当事人**:\n");
        if (result.getPlaintiffName() != null) {
            content.append("- 原告: ").append(result.getPlaintiffName()).append("\n");
        }
        if (result.getDefendantName() != null) {
            content.append("- 被告: ").append(result.getDefendantName()).append("\n");
        }
        content.append("\n**案由**: ").append(result.getCaseReason() != null ? result.getCaseReason() : "未知").append("\n\n");
        content.append("**判决日期**: ")
            .append(result.getJudgmentDate() != null ? result.getJudgmentDate() : "未知")
            .append("\n\n");

        // 工作总结
        workReport.setWorkSummary(content.toString());

        // 下一步计划：关注上诉期
        String nextPlan = String.format(
            "关注上诉期期限，确保在 %s 前完成上诉相关工作",
            result.getJudgmentDate() != null ? calculateAppealDeadline(result.getJudgmentDate()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "判决生效后15日内"
        );
        workReport.setNextPlan(nextPlan);

        // 状态：直接提交
        workReport.setStatus("SUBMITTED");

        log.info("创建工作日志: {}", title);
        return workReport;
    }

    /**
     * 创建上诉期到期待办
     */
    private TodoDTO createAppealDeadlineTodo(AIDocumentRecognitionResult result, Long caseId, LocalDateTime appealDeadline, Long userId) {
        TodoDTO todo = new TodoDTO();

        // 标题：{案号}上诉期到期提醒
        String title = String.format("%s上诉期到期提醒",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        todo.setTitle(title);

        // 描述
        String description = String.format(
            "《%s》判决书上诉期将于 %s 到期，请注意是否需要提起上诉。",
            result.getCaseNumber() != null ? result.getCaseNumber() : "相关案件",
            appealDeadline.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
        );
        todo.setDescription(description);

        // 状态：待处理
        todo.setStatus("PENDING");

        // 优先级：重要（上诉期是关键时间节点）
        todo.setPriority("IMPORTANT");

        // 截止时间：上诉期到期日
        todo.setDueDate(appealDeadline);

        // 负责人：当前用户
        todo.setAssigneeId(userId);

        // 关联案件
        todo.setCaseId(caseId);

        // 设置提醒
        todo.setReminder(true);

        log.info("创建待办提醒: {} 截止时间={}", title, appealDeadline);
        return todo;
    }

    /**
     * 创建上诉期到期日程
     */
    private CalendarDTO createAppealDeadlineCalendar(AIDocumentRecognitionResult result, Long caseId, LocalDateTime appealDeadline, Long userId) {
        CalendarDTO calendar = new CalendarDTO();

        // 标题：{案号}上诉期到期日
        String title = String.format("%s上诉期到期日",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        calendar.setTitle(title);

        // 日程类型：审限届满（橙色）
        calendar.setCalendarType("DEADLINE");

        // 开始时间：上诉期到期日 9:00
        calendar.setStartTime(appealDeadline);

        // 结束时间：上诉期到期日 18:00
        calendar.setEndTime(appealDeadline.plusHours(9));

        // 地点：案件关联
        calendar.setLocation(result.getCourtName() != null ? result.getCourtName() : "法院");

        // 关联案件
        calendar.setCaseId(caseId);

        // 提醒：提前3天提醒
        calendar.setReminder(true);
        calendar.setReminderMinutes(3 * 24 * 60); // 3天

        log.info("创建日程提醒: {} 时间={}", title, appealDeadline);
        return calendar;
    }

    /**
     * 计算上诉期到期日
     *
     * 规则：
     * - 民事判决：判决书送达之日起15日内
     * - 刑事判决：判决书送达之日起10日内
     * - 行政判决：判决书送达之日起15日内
     *
     * 这里默认使用15日（民事、行政），后续可根据案件类型调整
     */
    private LocalDateTime calculateAppealDeadline(String judgmentDateStr) {
        // 判决日期（如果识别不到，默认为上传当日）
        LocalDate judgmentDate;
        if (judgmentDateStr != null && !judgmentDateStr.trim().isEmpty()) {
            try {
                judgmentDate = LocalDate.parse(judgmentDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                log.warn("解析判决日期失败: {}, 使用当前日期", judgmentDateStr);
                judgmentDate = LocalDate.now();
            }
        } else {
            judgmentDate = LocalDate.now();
        }

        // 上诉期：15日
        LocalDate appealDeadline = judgmentDate.plusDays(15);

        // 返回到期日上午9点
        return appealDeadline.atTime(9, 0);
    }

    /**
     * 构建案件名称
     * 格式：原告 vs 被告 + 案由
     */
    private String buildCaseName(AIDocumentRecognitionResult result) {
        StringBuilder caseName = new StringBuilder();

        if (result.getPlaintiffName() != null) {
            caseName.append(result.getPlaintiffName());
        } else {
            caseName.append("原告");
        }

        caseName.append(" vs ");

        if (result.getDefendantName() != null) {
            caseName.append(result.getDefendantName());
        } else {
            caseName.append("被告");
        }

        if (result.getCaseReason() != null) {
            caseName.append(" - ").append(result.getCaseReason());
        }

        return caseName.toString();
    }

    /**
     * 根据案由推断案件类型
     */
    private String inferCaseType(String caseReason) {
        if (caseReason == null || caseReason.trim().isEmpty()) {
            return "CIVIL"; // 默认民事
        }

        String lowerReason = caseReason.toLowerCase();

        // 刑事案件关键词
        if (lowerReason.contains("诈骗") || lowerReason.contains("盗窃") ||
            lowerReason.contains("故意伤害") || lowerReason.contains("杀人") ||
            lowerReason.contains("毒品") || lowerReason.contains("贪污受贿")) {
            return "CRIMINAL";
        }

        // 行政案件关键词
        if (lowerReason.contains("行政") || lowerReason.contains("行政处罚") ||
            lowerReason.contains("行政复议") || lowerReason.contains("政府信息公开")) {
            return "ADMINISTRATIVE";
        }

        // 默认民事
        return "CIVIL";
    }

    // ========== 其他文书类型的处理方法（待实现） ==========

    /**
     * 处理起诉状识别结果
     *
     * 业务逻辑流程：
     * 1. 创建草稿案件（或关联到已存在的草稿案件）
     * 2. 创建待办提醒：答辩准备
     * 3. 创建待办提醒：庭前准备
     *
     * @param result AI识别结果
     * @param userId 当前用户ID
     * @return 处理结果（创建的案件、待办信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleComplaint(AIDocumentRecognitionResult result, Long userId) {
        log.info("开始处理起诉状识别结果: 案号={}, 法院={}", result.getCaseNumber(), result.getCourtName());

        Map<String, Object> processResult = new HashMap<>();

        try {
            // ========== 步骤1: 创建或关联草稿案件 ==========
            Case draftCase = findOrCreateDraftCaseByComplaint(result, userId);
            processResult.put("caseId", draftCase.getId());
            processResult.put("caseName", draftCase.getCaseName());
            log.info("步骤1完成: 草稿案件ID={}, 案件名称={}", draftCase.getId(), draftCase.getCaseName());

            // ========== 步骤2: 创建答辩准备待办 ==========
            TodoDTO answerTodo = createAnswerPreparationTodo(result, draftCase.getId(), userId);
            TodoDTO createdAnswerTodo = todoService.createTodo(answerTodo, userId);
            processResult.put("answerTodoId", createdAnswerTodo.getId());
            log.info("步骤2完成: 答辩准备待办ID={}", createdAnswerTodo.getId());

            // ========== 步骤3: 创建庭前准备待办 ==========
            TodoDTO pretrialTodo = createPretrialPreparationTodo(result, draftCase.getId(), userId);
            TodoDTO createdPretrialTodo = todoService.createTodo(pretrialTodo, userId);
            processResult.put("pretrialTodoId", createdPretrialTodo.getId());
            log.info("步骤3完成: 庭前准备待办ID={}", createdPretrialTodo.getId());

            processResult.put("success", true);
            processResult.put("message", "起诉状处理成功");

        } catch (Exception e) {
            log.error("处理起诉状失败", e);
            throw new AIServiceException("处理起诉状失败: " + e.getMessage(), e);
        }

        return processResult;
    }

    /**
     * 根据起诉状信息创建草稿案件
     * 起诉状通常表示案件刚刚开始，所以创建草稿状态案件
     */
    private Case findOrCreateDraftCaseByComplaint(AIDocumentRecognitionResult result, Long userId) {
        // 先尝试根据案号查找（避免重复创建）
        if (result.getCaseNumber() != null && !result.getCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> existingCase = caseRepository.findByCaseNumberAndDeletedFalse(result.getCaseNumber());
            if (existingCase.isPresent()) {
                Case caseEntity = existingCase.get();
                log.info("找到已存在案件: 案号={}, 案件ID={}", result.getCaseNumber(), caseEntity.getId());
                return caseEntity;
            }
        }

        // 未找到案件，创建新的草稿案件
        log.info("创建新的草稿案件: 案号={}", result.getCaseNumber());
        CaseCreateRequest createRequest = buildDraftCaseCreateRequest(result, userId);

        // CaseService.createCase 返回 CaseDetailVO，需要获取ID后查询实体
        CaseDetailVO caseVO = caseService.createCase(createRequest, userId);
        Case newCase = caseRepository.findById(caseVO.getId())
            .orElseThrow(() -> new AIServiceException("创建案件后无法查询到案件实体"));

        log.info("草稿案件创建成功: 案号={}, 案件ID={}", newCase.getCaseNumber(), newCase.getId());
        return newCase;
    }

    /**
     * 构建草稿案件创建请求
     */
    private CaseCreateRequest buildDraftCaseCreateRequest(AIDocumentRecognitionResult result, Long userId) {
        CaseCreateRequest createRequest = new CaseCreateRequest();

        // 案件名称：原告 vs 被告 + 案由
        String caseName = buildCaseName(result);
        createRequest.setCaseName(caseName);

        // 案号
        createRequest.setCaseNumber(result.getCaseNumber());

        // 案件类型：根据案由推断
        String caseType = inferCaseType(result.getCaseReason());
        createRequest.setCaseType(caseType);

        // 案件程序：默认一审
        createRequest.setProcedure("FIRST_INSTANCE");

        // 案由
        createRequest.setCaseReason(result.getCaseReason());

        // 案件状态：草稿（因为是起诉状，案件还未正式受理）
        createRequest.setStatus("DRAFT");

        // 当前阶段：待立案
        createRequest.setCurrentStage("待立案");

        // 案件等级：默认一般
        createRequest.setLevel("GENERAL");

        // 主办律师：当前用户
        createRequest.setOwnerId(userId);

        // 法院
        createRequest.setCourt(result.getCourtName());

        // 标签
        createRequest.setTags("AI识别,起诉状,待立案");

        return createRequest;
    }

    /**
     * 创建答辩准备待办
     */
    private TodoDTO createAnswerPreparationTodo(AIDocumentRecognitionResult result, Long caseId, Long userId) {
        TodoDTO todo = new TodoDTO();

        // 标题：{案号}答辩准备
        String title = String.format("%s答辩准备",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        todo.setTitle(title);

        // 描述
        StringBuilder description = new StringBuilder();
        description.append("收到起诉状，需准备答辩材料：\n\n");
        description.append("**原告**: ").append(result.getPlaintiffName() != null ? result.getPlaintiffName() : "未知").append("\n");
        description.append("**被告**: ").append(result.getDefendantName() != null ? result.getDefendantName() : "未知").append("\n");
        description.append("**案由**: ").append(result.getCaseReason() != null ? result.getCaseReason() : "未知").append("\n");
        description.append("**法院**: ").append(result.getCourtName() != null ? result.getCourtName() : "未知").append("\n\n");
        description.append("请完成以下工作：\n");
        description.append("1. 核对当事人信息\n");
        description.append("2. 整理答辩证据\n");
        description.append("3. 起草答辩状\n");
        description.append("4. 准备授权委托材料\n");
        todo.setDescription(description.toString());

        // 状态：待处理
        todo.setStatus("PENDING");

        // 优先级：重要
        todo.setPriority("IMPORTANT");

        // 截止时间：收到起诉状后15日内
        LocalDateTime dueDate = LocalDateTime.now().plusDays(15);
        todo.setDueDate(dueDate);

        // 负责人：当前用户
        todo.setAssigneeId(userId);

        // 关联案件
        todo.setCaseId(caseId);

        // 设置提醒
        todo.setReminder(true);

        log.info("创建答辩准备待办: {} 截止时间={}", title, dueDate);
        return todo;
    }

    /**
     * 创建庭前准备待办
     */
    private TodoDTO createPretrialPreparationTodo(AIDocumentRecognitionResult result, Long caseId, Long userId) {
        TodoDTO todo = new TodoDTO();

        // 标题：{案号}庭前准备
        String title = String.format("%s庭前准备",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        todo.setTitle(title);

        // 描述
        StringBuilder description = new StringBuilder();
        description.append("开庭前需完成以下准备工作：\n\n");
        description.append("1. 证据清单及证据原件\n");
        description.append("2. 代理词草稿\n");
        description.append("3. 证据质证意见\n");
        description.append("4. 庭审发问提纲\n");
        description.append("5. 与当事人庭前沟通\n");
        todo.setDescription(description.toString());

        // 状态：待处理
        todo.setStatus("PENDING");

        // 优先级：普通（在答辩准备之后）
        todo.setPriority("NORMAL");

        // 截止时间：收到起诉状后30日内（预计开庭时间）
        LocalDateTime dueDate = LocalDateTime.now().plusDays(30);
        todo.setDueDate(dueDate);

        // 负责人：当前用户
        todo.setAssigneeId(userId);

        // 关联案件
        todo.setCaseId(caseId);

        // 设置提醒
        todo.setReminder(false); // 不需要提前提醒，后续根据实际开庭时间调整

        log.info("创建庭前准备待办: {} 截止时间={}", title, dueDate);
        return todo;
    }

    /**
     * 处理答辩状识别结果
     *
     * 业务逻辑流程：
     * 1. 根据案号关联到已存在的案件
     * 2. 更新案件状态为"已立案"
     * 3. 创建待办提醒：庭审准备
     * 4. 创建待办提醒：证据提交
     *
     * @param result AI识别结果
     * @param userId 当前用户ID
     * @return 处理结果（关联的案件、待办信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleAnswer(AIDocumentRecognitionResult result, Long userId) {
        log.info("开始处理答辩状识别结果: 案号={}, 法院={}", result.getCaseNumber(), result.getCourtName());

        Map<String, Object> processResult = new HashMap<>();

        try {
            // ========== 步骤1: 根据案号关联已存在的案件 ==========
            Case relatedCase = findExistingCaseByAnswer(result, userId);
            if (relatedCase == null) {
                processResult.put("success", false);
                processResult.put("message", "未找到关联案件，请先创建案件或上传起诉状");
                return processResult;
            }
            processResult.put("caseId", relatedCase.getId());
            processResult.put("caseName", relatedCase.getCaseName());
            log.info("步骤1完成: 关联案件ID={}, 案件名称={}", relatedCase.getId(), relatedCase.getCaseName());

            // ========== 步骤2: 更新案件状态为"已立案" ==========
            updateCaseStatusForAnswer(relatedCase, userId);
            processResult.put("statusUpdated", true);
            log.info("步骤2完成: 案件状态已更新为已立案");

            // ========== 步骤3: 创建庭审准备待办 ==========
            TodoDTO trialTodo = createTrialPreparationTodo(result, relatedCase.getId(), userId);
            TodoDTO createdTrialTodo = todoService.createTodo(trialTodo, userId);
            processResult.put("trialTodoId", createdTrialTodo.getId());
            log.info("步骤3完成: 庭审准备待办ID={}", createdTrialTodo.getId());

            // ========== 步骤4: 创建证据提交待办 ==========
            TodoDTO evidenceTodo = createEvidenceSubmissionTodo(result, relatedCase.getId(), userId);
            TodoDTO createdEvidenceTodo = todoService.createTodo(evidenceTodo, userId);
            processResult.put("evidenceTodoId", createdEvidenceTodo.getId());
            log.info("步骤4完成: 证据提交待办ID={}", createdEvidenceTodo.getId());

            processResult.put("success", true);
            processResult.put("message", "答辩状处理成功");

        } catch (Exception e) {
            log.error("处理答辩状失败", e);
            throw new AIServiceException("处理答辩状失败: " + e.getMessage(), e);
        }

        return processResult;
    }

    /**
     * 根据答辩状信息查找已存在的案件
     */
    private Case findExistingCaseByAnswer(AIDocumentRecognitionResult result, Long userId) {
        // 答辩状必须关联到已存在的案件
        if (result.getCaseNumber() != null && !result.getCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> existingCase = caseRepository.findByCaseNumberAndDeletedFalse(result.getCaseNumber());
            if (existingCase.isPresent()) {
                Case caseEntity = existingCase.get();
                log.info("找到关联案件: 案号={}, 案件ID={}", result.getCaseNumber(), caseEntity.getId());
                return caseEntity;
            }
        }

        log.warn("未找到关联案件: 案号={}", result.getCaseNumber());
        return null;
    }

    /**
     * 更新案件状态为"已立案"
     */
    private void updateCaseStatusForAnswer(Case caseEntity, Long userId) {
        // 答辩状说明案件已经正式受理，更新状态为"已立案"
        CaseUpdateRequest updateRequest = new CaseUpdateRequest();
        updateRequest.setStatus("ACTIVE");
        updateRequest.setCurrentStage("已立案");

        caseService.updateCase(caseEntity.getId(), updateRequest);
        log.info("案件状态已更新: 案件ID={}, 状态=ACTIVE, 阶段=已立案", caseEntity.getId());
    }

    /**
     * 创建庭审准备待办
     */
    private TodoDTO createTrialPreparationTodo(AIDocumentRecognitionResult result, Long caseId, Long userId) {
        TodoDTO todo = new TodoDTO();

        // 标题：{案号}庭审准备
        String title = String.format("%s庭审准备",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        todo.setTitle(title);

        // 描述
        StringBuilder description = new StringBuilder();
        description.append("收到答辩状，需准备庭审材料：\n\n");
        description.append("**被告**: ").append(result.getDefendantName() != null ? result.getDefendantName() : "未知").append("\n");
        description.append("**法院**: ").append(result.getCourtName() != null ? result.getCourtName() : "未知").append("\n\n");
        description.append("请完成以下工作：\n");
        description.append("1. 整理质证意见\n");
        description.append("2. 准备代理词\n");
        description.append("3. 梳理争议焦点\n");
        description.append("4. 准备庭审发问提纲\n");
        description.append("5. 与当事人确认答辩策略\n");
        todo.setDescription(description.toString());

        // 状态：待处理
        todo.setStatus("PENDING");

        // 优先级：重要
        todo.setPriority("IMPORTANT");

        // 截止时间：收到答辩状后20日内
        LocalDateTime dueDate = LocalDateTime.now().plusDays(20);
        todo.setDueDate(dueDate);

        // 负责人：当前用户
        todo.setAssigneeId(userId);

        // 关联案件
        todo.setCaseId(caseId);

        // 设置提醒
        todo.setReminder(true);

        log.info("创建庭审准备待办: {} 截止时间={}", title, dueDate);
        return todo;
    }

    /**
     * 创建证据提交待办
     */
    private TodoDTO createEvidenceSubmissionTodo(AIDocumentRecognitionResult result, Long caseId, Long userId) {
        TodoDTO todo = new TodoDTO();

        // 标题：{案号}证据提交
        String title = String.format("%s证据提交",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        todo.setTitle(title);

        // 描述
        StringBuilder description = new StringBuilder();
        description.append("根据答辩状内容，需准备以下证据：\n\n");
        description.append("1. 核实证据原件\n");
        description.append("2. 整理证据清单\n");
        description.append("3. 准备证据交换材料\n");
        description.append("4. 提交法院证据副本\n");
        description.append("5. 与当事人确认证据完整性\n");
        todo.setDescription(description.toString());

        // 状态：待处理
        todo.setStatus("PENDING");

        // 优先级：普通（在庭审准备之后）
        todo.setPriority("NORMAL");

        // 截止时间：收到答辩状后15日内（举证期限）
        LocalDateTime dueDate = LocalDateTime.now().plusDays(15);
        todo.setDueDate(dueDate);

        // 负责人：当前用户
        todo.setAssigneeId(userId);

        // 关联案件
        todo.setCaseId(caseId);

        // 设置提醒
        todo.setReminder(true);

        log.info("创建证据提交待办: {} 截止时间={}", title, dueDate);
        return todo;
    }

    /**
     * 处理调解书识别结果
     *
     * 业务逻辑流程：
     * 1. 根据案号关联到已存在的案件
     * 2. 更新案件状态为"结案"（调解结案）
     * 3. 创建待办提醒：履行期限监控
     * 4. 创建待办提醒：卷宗整理
     *
     * @param result AI识别结果
     * @param userId 当前用户ID
     * @return 处理结果（关联的案件、待办信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleMediation(AIDocumentRecognitionResult result, Long userId) {
        log.info("开始处理调解书识别结果: 案号={}, 法院={}", result.getCaseNumber(), result.getCourtName());

        Map<String, Object> processResult = new HashMap<>();

        try {
            // ========== 步骤1: 根据案号关联已存在的案件 ==========
            Case relatedCase = findExistingCaseByMediation(result, userId);
            if (relatedCase == null) {
                processResult.put("success", false);
                processResult.put("message", "未找到关联案件，请先创建案件");
                return processResult;
            }
            processResult.put("caseId", relatedCase.getId());
            processResult.put("caseName", relatedCase.getCaseName());
            log.info("步骤1完成: 关联案件ID={}, 案件名称={}", relatedCase.getId(), relatedCase.getCaseName());

            // ========== 步骤2: 更新案件状态为"结案" ==========
            updateCaseStatusForMediation(relatedCase, result, userId);
            processResult.put("statusUpdated", true);
            log.info("步骤2完成: 案件状态已更新为结案");

            // ========== 步骤3: 创建履行期限监控待办 ==========
            TodoDTO performanceTodo = createPerformanceMonitoringTodo(result, relatedCase.getId(), userId);
            TodoDTO createdPerformanceTodo = todoService.createTodo(performanceTodo, userId);
            processResult.put("performanceTodoId", createdPerformanceTodo.getId());
            log.info("步骤3完成: 履行期限监控待办ID={}", createdPerformanceTodo.getId());

            // ========== 步骤4: 创建卷宗整理待办 ==========
            TodoDTO archiveTodo = createArchiveOrganizationTodo(result, relatedCase.getId(), userId);
            TodoDTO createdArchiveTodo = todoService.createTodo(archiveTodo, userId);
            processResult.put("archiveTodoId", createdArchiveTodo.getId());
            log.info("步骤4完成: 卷宗整理待办ID={}", createdArchiveTodo.getId());

            processResult.put("success", true);
            processResult.put("message", "调解书处理成功");

        } catch (Exception e) {
            log.error("处理调解书失败", e);
            throw new AIServiceException("处理调解书失败: " + e.getMessage(), e);
        }

        return processResult;
    }

    /**
     * 根据调解书信息查找已存在的案件
     */
    private Case findExistingCaseByMediation(AIDocumentRecognitionResult result, Long userId) {
        // 调解书必须关联到已存在的案件
        if (result.getCaseNumber() != null && !result.getCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> existingCase = caseRepository.findByCaseNumberAndDeletedFalse(result.getCaseNumber());
            if (existingCase.isPresent()) {
                Case caseEntity = existingCase.get();
                log.info("找到关联案件: 案号={}, 案件ID={}", result.getCaseNumber(), caseEntity.getId());
                return caseEntity;
            }
        }

        log.warn("未找到关联案件: 案号={}", result.getCaseNumber());
        return null;
    }

    /**
     * 更新案件状态为"结案"（调解结案）
     */
    private void updateCaseStatusForMediation(Case caseEntity, AIDocumentRecognitionResult result, Long userId) {
        // 调解书说明案件已经调解结案，更新状态
        CaseUpdateRequest updateRequest = new CaseUpdateRequest();
        updateRequest.setStatus("CLOSED");
        updateRequest.setCloseStatus("达成诉求"); // 调解结案视为达成诉求

        // 如果识别到了调解日期，设置结案日期
        if (result.getJudgmentDate() != null && !result.getJudgmentDate().trim().isEmpty()) {
            try {
                LocalDate closeDate = LocalDate.parse(result.getJudgmentDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                updateRequest.setCloseDate(closeDate);
            } catch (Exception e) {
                log.warn("解析调解日期失败: {}", result.getJudgmentDate());
            }
        }

        caseService.updateCase(caseEntity.getId(), updateRequest);
        log.info("案件状态已更新: 案件ID={}, 状态=CLOSED, 结案方式=调解", caseEntity.getId());
    }

    /**
     * 创建履行期限监控待办
     */
    private TodoDTO createPerformanceMonitoringTodo(AIDocumentRecognitionResult result, Long caseId, Long userId) {
        TodoDTO todo = new TodoDTO();

        // 标题：{案号}履行期限监控
        String title = String.format("%s履行期限监控",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        todo.setTitle(title);

        // 描述
        StringBuilder description = new StringBuilder();
        description.append("案件已调解结案，需监控履行情况：\n\n");
        description.append("**调解日期**: ")
            .append(result.getJudgmentDate() != null ? result.getJudgmentDate() : "未知")
            .append("\n\n");
        description.append("请完成以下工作：\n");
        description.append("1. 核对调解书内容\n");
        description.append("2. 确认履行期限\n");
        description.append("3. 提醒当事人按期履行\n");
        description.append("4. 跟踪履行进度\n");
        description.append("5. 申请强制执行（如需要）\n");
        todo.setDescription(description.toString());

        // 状态：待处理
        todo.setStatus("PENDING");

        // 优先级：重要（履行期限是关键时间节点）
        todo.setPriority("IMPORTANT");

        // 截止时间：调解书送达后30日内（一般履行期限）
        LocalDateTime dueDate = LocalDateTime.now().plusDays(30);
        todo.setDueDate(dueDate);

        // 负责人：当前用户
        todo.setAssigneeId(userId);

        // 关联案件
        todo.setCaseId(caseId);

        // 设置提醒
        todo.setReminder(true);

        log.info("创建履行期限监控待办: {} 截止时间={}", title, dueDate);
        return todo;
    }

    /**
     * 创建卷宗整理待办
     */
    private TodoDTO createArchiveOrganizationTodo(AIDocumentRecognitionResult result, Long caseId, Long userId) {
        TodoDTO todo = new TodoDTO();

        // 标题：{案号}卷宗整理
        String title = String.format("%s卷宗整理",
            result.getCaseNumber() != null ? result.getCaseNumber() : "案件");
        todo.setTitle(title);

        // 描述
        StringBuilder description = new StringBuilder();
        description.append("案件已调解结案，需整理卷宗：\n\n");
        description.append("请完成以下工作：\n");
        description.append("1. 整理起诉状、答辩状\n");
        description.append("2. 整理证据材料\n");
        description.append("3. 整理调解书正本\n");
        description.append("4. 整理庭审笔录\n");
        description.append("5. 编写卷宗目录\n");
        description.append("6. 扫描归档\n");
        todo.setDescription(description.toString());

        // 状态：待处理
        todo.setStatus("PENDING");

        // 优先级：普通
        todo.setPriority("NORMAL");

        // 截止时间：结案后15日内
        LocalDateTime dueDate = LocalDateTime.now().plusDays(15);
        todo.setDueDate(dueDate);

        // 负责人：当前用户
        todo.setAssigneeId(userId);

        // 关联案件
        todo.setCaseId(caseId);

        // 设置提醒
        todo.setReminder(false);

        log.info("创建卷宗整理待办: {} 截止时间={}", title, dueDate);
        return todo;
    }
}
