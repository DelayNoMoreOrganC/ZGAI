package com.lawfirm.service;

import com.lawfirm.dto.AiChatRequest;
import com.lawfirm.entity.Case;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AILogService aiLogService;
    private final CaseRepository caseRepository;
    private final CaseService caseService;
    private final AIGenerationGateway generationGateway;

    public String generalChat(String message, Long userId) {
        return generalChat(message, userId, null);
    }

    public String generalChat(String message, Long userId, String providerType) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String resolvedProvider = null;
        try {
            AIGenerationGateway.GenerationResult generation = generationGateway.generate(
                    providerType, buildGeneralChatPrompt(message));
            modelName = generation.getModelName();
            resolvedProvider = generation.getProviderType();
            String result = generation.getContent();
            aiLogService.log(userId, null, AIFunctionType.LEGAL_QA,
                    message, null, result, null, resolvedProvider, modelName, "SUCCESS",
                    elapsedSince(startTime), null, null);
            return result;
        } catch (Exception e) {
            log.error("AI问答失败", e);
            aiLogService.log(userId, null, AIFunctionType.LEGAL_QA,
                    message, null, null, null, resolvedProvider, modelName, "FAILED",
                    elapsedSince(startTime), e.getMessage(), null);
            throw new RuntimeException("AI问答失败: " + e.getMessage());
        }
    }

    /**
     * Case chat is read-only. All case mutations go through AICaseCommandService,
     * which provides idempotency, permissions, risk classification and audit.
     */
    public String caseChat(AiChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String resolvedProvider = null;
        try {
            String caseContext = "";
            if (request.getCaseId() != null) {
                caseService.assertCaseVisible(request.getCaseId(), userId);
                Case caseEntity = caseRepository.findById(request.getCaseId())
                        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                        .orElseThrow(() -> new IllegalArgumentException("案件不存在"));
                caseContext = buildCaseContext(caseEntity);
            }

            AIGenerationGateway.GenerationResult generation = generationGateway.generate(
                    request.getProviderType(), buildCaseChatPrompt(request.getMessage(), caseContext));
            modelName = generation.getModelName();
            resolvedProvider = generation.getProviderType();
            String result = generation.getContent();
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.CASE_ANALYSIS,
                    request.getMessage(), null, result, null, resolvedProvider, modelName, "SUCCESS",
                    elapsedSince(startTime), null, null);
            return result;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("案件AI问答失败", e);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.CASE_ANALYSIS,
                    request.getMessage(), null, null, null, resolvedProvider, modelName, "FAILED",
                    elapsedSince(startTime), e.getMessage(), null);
            throw new RuntimeException("案件AI问答失败: " + e.getMessage());
        }
    }

    private String buildGeneralChatPrompt(String message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【角色定位】\n");
        prompt.append("你是一位资深律师，精通中国法律法规和司法实务。\n");
        prompt.append("你的职责是为用户提供准确、专业、实用的法律咨询建议。\n\n");

        prompt.append("【工作原则】\n");
        prompt.append("1. 准确性原则：基于现行有效的法律法规提供意见，不引用已废止的法律\n");
        prompt.append("2. 客观性原则：客观分析法律问题，不夸大或缩小法律风险\n");
        prompt.append("3. 实用性原则：提供可操作的建议，避免空洞的理论阐述\n");
        prompt.append("4. 谨慎性原则：对不确定的法律问题明确说明，避免误导用户\n");
        prompt.append("5. 保护隐私原则：不要求用户提供过多个人隐私信息\n\n");

        prompt.append("【回答结构要求】\n");
        prompt.append("1. 问题理解：简要概括你对用户问题的理解\n");
        prompt.append("2. 法律分析：相关法律规定、法律关系和关键法律要点\n");
        prompt.append("3. 风险提示：可能的法律风险和注意事项\n");
        prompt.append("4. 实务建议：具体、可操作的行动建议\n");
        prompt.append("5. 信息不足时明确列出需要补充的内容\n\n");

        prompt.append("【用户问题】\n").append(message).append("\n\n");
        prompt.append("不得虚构法律条文、案例、时效状态或来源；无法核验时必须明确说明。\n");
        return prompt.toString();
    }

    private String buildCaseChatPrompt(String message, String caseContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【角色定位】\n");
        prompt.append("你是至高律所的案件分析助手，只能在提供的案件上下文内协助律师。\n");
        prompt.append("区分已知事实、当事人陈述、推断和待核实事项，不得把推断写成事实。\n\n");

        prompt.append("【案件基本信息】\n");
        prompt.append(caseContext == null || caseContext.isEmpty() ? "未提供案件信息\n" : caseContext);
        prompt.append("\n【用户问题】\n").append(message).append("\n\n");

        prompt.append("【回答要求】\n");
        prompt.append("1. 概括案件要点和当前问题\n");
        prompt.append("2. 分析法律关系、争议焦点、程序和证据\n");
        prompt.append("3. 分别列出有利因素、不利因素和待核实事项\n");
        prompt.append("4. 给出可操作的下一步建议并提示关键期限风险\n");
        prompt.append("5. 引用法规时标明名称和条文；不能确认有效性时提示核验\n\n");

        prompt.append("【系统操作边界】\n");
        prompt.append("本接口只提供案件问答，不执行日程、待办、进展、文件或阶段变更。\n");
        prompt.append("如用户要求执行系统操作，请引导其使用案件AI助手；不得输出或伪造系统指令标记。\n");
        return prompt.toString();
    }

    private String buildCaseContext(Case caseEntity) {
        StringBuilder context = new StringBuilder();
        append(context, "案件名称", caseEntity.getCaseName());
        append(context, "律所案号", caseEntity.getCaseNumber());
        append(context, "法院案号", caseEntity.getCourtCaseNumber());
        append(context, "案件类型", caseEntity.getCaseType());
        append(context, "案由", caseEntity.getCaseReason());
        append(context, "法院/机构", caseEntity.getCourt());
        append(context, "案件简述", caseEntity.getSummary());
        append(context, "案件状态", caseEntity.getStatus());
        append(context, "当前阶段", caseEntity.getCurrentStage());
        return context.toString();
    }

    private void append(StringBuilder target, String label, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            target.append(label).append('：').append(value).append('\n');
        }
    }

    private int elapsedSince(long startTime) {
        long elapsed = Math.max(0, System.currentTimeMillis() - startTime);
        return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
    }
}
