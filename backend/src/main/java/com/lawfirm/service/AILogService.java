package com.lawfirm.service;

import com.lawfirm.entity.AILog;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.repository.AILogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AILogService {

    private final AILogRepository aiLogRepository;

    /**
     * 记录AI调用日志
     */
    public void log(Long userId, Long caseId, AIFunctionType functionType,
                    String inputContent, Integer inputTokens,
                    String outputContent, Integer outputTokens,
                    String modelName, String status, Integer duration, String errorMessage) {
        AILog log = new AILog();
        log.setUserId(userId);
        log.setCaseId(caseId);
        log.setFunctionType(functionType.getCode());
        log.setInputContent(inputContent);
        log.setInputTokens(inputTokens);
        log.setOutputContent(outputContent);
        log.setOutputTokens(outputTokens);
        log.setModelName(modelName);
        log.setStatus(status);
        log.setDuration(duration);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(LocalDateTime.now());

        aiLogRepository.save(log);
    }

    /**
     * 获取用户的AI使用日志
     */
    public Page<AILog> getUserLogs(Long userId, Pageable pageable) {
        return aiLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 获取案件的AI使用日志
     */
    public Page<AILog> getCaseLogs(Long caseId, Pageable pageable) {
        return aiLogRepository.findByCaseIdOrderByCreatedAtDesc(caseId, pageable);
    }

    /**
     * 获取所有AI使用日志
     */
    public Page<AILog> getAllLogs(Pageable pageable) {
        return aiLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
