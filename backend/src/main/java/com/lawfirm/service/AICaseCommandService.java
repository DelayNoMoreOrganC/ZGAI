package com.lawfirm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AICaseCommandService {

    private static final Pattern CN_DATE_TIME = Pattern.compile(
            "(?:(\\d{4})年)?(\\d{1,2})月(\\d{1,2})日(?:\\s*(上午|下午|中午|晚上))?\\s*(\\d{1,2})(?:[:：时点](\\d{1,2})分?)?");
    private static final Pattern RELATIVE_DATE_TIME = Pattern.compile(
            "(今天|明天|后天|今晚)(?:\\s*(上午|下午|中午|晚上))?\\s*(\\d{1,2})(?:[:：时点](\\d{1,2})分?)?");
    private static final Pattern ISO_DATE_TIME = Pattern.compile(
            "(\\d{4})-(\\d{1,2})-(\\d{1,2})[ T](\\d{1,2}):([0-5]\\d)");
    private static final Pattern LOCATION = Pattern.compile("(?:地点(?:为|是|：|:)?|在)([^，。；,;]+?)(?:开庭|参加|$)");
    private static final Pattern STAGE = Pattern.compile("(?:进入|变更为|调整为)[「\"“]?([^」\"”。，,]{1,30}?)[」\"”]?(?:阶段|$)");

    private final AICaseCommandRepository commandRepository;
    private final CaseRepository caseRepository;
    private final CaseService caseService;
    private final CalendarService calendarService;
    private final CalendarReminderService calendarReminderService;
    private final TodoService todoService;
    private final CaseTimelineService timelineService;
    private final CaseActivityService activityService;
    private final CaseStageService caseStageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AICaseCommandResponse submit(AICaseCommandRequest request, Long userId) {
        String rawInstruction = request.getInstruction().trim();
        Optional<AICaseCommand> existing = commandRepository
                .findByUserIdAndIdempotencyKeyAndDeletedFalse(userId, request.getIdempotencyKey().trim());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        AICaseCommand command = new AICaseCommand();
        command.setUserId(userId);
        command.setIdempotencyKey(request.getIdempotencyKey().trim());
        command.setInstruction(AIContentPrivacy.commandSummary(rawInstruction));
        command.setInstructionHash(AIContentPrivacy.sha256(rawInstruction));
        command.setPrivacySanitizedAt(java.time.LocalDateTime.now());
        command.setModelName("RULE_ENGINE_V1");
        command.setStatus("PARSING");
        command = commandRepository.save(command);

        Case targetCase = resolveCase(request.getCaseId(), rawInstruction, userId);
        if (targetCase == null) {
            return clarify(command, "请先指定案件；全局指令只有在案件名称或案号能够唯一匹配时才会执行。");
        }
        command.setCaseId(targetCase.getId());

        List<AIActionDTO> actions = parseActions(rawInstruction, targetCase);
        if (actions.isEmpty()) {
            return clarify(command, "我暂时不能安全识别该操作。请明确说明要建立日程、待办、记录进展或变更阶段。");
        }

        Optional<String> missing = requiredClarification(actions);
        if (missing.isPresent()) {
            command.setActionsJson(write(actions));
            return clarify(command, missing.get());
        }

        boolean requiresConfirmation = actions.stream()
                .anyMatch(action -> Boolean.TRUE.equals(action.getRequiresConfirmation()));
        command.setActionsJson(write(actions));
        command.setRiskLevel(requiresConfirmation ? "HIGH" : "LOW");
        if (requiresConfirmation) {
            command.setStatus("PROPOSED");
            commandRepository.save(command);
            return toResponse(command);
        }

        executeActions(command, actions, userId);
        return toResponse(command);
    }

    @Transactional
    public AICaseCommandResponse confirm(Long commandId, Long userId) {
        AICaseCommand command = commandRepository.findById(commandId)
                .orElseThrow(() -> new IllegalArgumentException("AI操作不存在"));
        if (!Objects.equals(command.getUserId(), userId)) {
            throw new IllegalArgumentException("只能确认本人发起的AI操作");
        }
        if ("AUTO_EXECUTED".equals(command.getStatus()) || "CONFIRMED".equals(command.getStatus())) {
            return toResponse(command);
        }
        if (!"PROPOSED".equals(command.getStatus())) {
            throw new IllegalArgumentException("当前AI操作不可确认");
        }
        caseService.assertCaseEditable(command.getCaseId(), userId);
        List<AIActionDTO> actions = readActions(command.getActionsJson());
        executeActions(command, actions, userId);
        command.setStatus("CONFIRMED");
        commandRepository.save(command);
        return toResponse(command);
    }

    private Case resolveCase(Long requestedCaseId, String instruction, Long userId) {
        if (requestedCaseId != null) {
            caseService.assertCaseVisible(requestedCaseId, userId);
            return caseRepository.findById(requestedCaseId)
                    .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                    .orElseThrow(() -> new IllegalArgumentException("案件不存在"));
        }
        List<Case> matches = caseRepository.findByDeletedFalse().stream()
                .filter(item -> caseService.canAccessCase(item.getId(), userId))
                .filter(item -> containsIdentifier(instruction, item))
                .collect(Collectors.toList());
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean containsIdentifier(String instruction, Case item) {
        return (StringUtils.hasText(item.getCaseName()) && instruction.contains(item.getCaseName()))
                || (StringUtils.hasText(item.getCaseNumber()) && instruction.contains(item.getCaseNumber()))
                || (StringUtils.hasText(item.getCourtCaseNumber()) && instruction.contains(item.getCourtCaseNumber()));
    }

    private List<AIActionDTO> parseActions(String instruction, Case targetCase) {
        List<AIActionDTO> actions = new ArrayList<>();
        if (instruction.contains("开庭") || instruction.contains("听证")) {
            AIActionDTO action = baseAction("CREATE_CALENDAR", "LOW", targetCase.getId(), false,
                    "律师明确要求建立开庭或听证日程");
            LocalDateTime start = parseDateTime(instruction);
            Matcher locationMatcher = LOCATION.matcher(instruction);
            action.getPayload().put("title", targetCase.getCaseName() + (instruction.contains("听证") ? " - 听证" : " - 开庭"));
            action.getPayload().put("calendarType", "HEARING");
            action.getPayload().put("startTime", start == null ? null : start.toString());
            action.getPayload().put("endTime", start == null ? null : start.plusHours(2).toString());
            action.getPayload().put("location", locationMatcher.find() ? locationMatcher.group(1).trim() : null);
            actions.add(action);
        } else if (instruction.contains("待办") || instruction.contains("提醒我")) {
            AIActionDTO action = baseAction("CREATE_TODO", "LOW", targetCase.getId(), false,
                    "律师明确要求建立个人待办");
            LocalDateTime due = parseDateTime(instruction);
            action.getPayload().put("title", cleanInstruction(instruction));
            action.getPayload().put("dueTime", due == null ? null : due.toString());
            action.getPayload().put("priority", "HIGH");
            actions.add(action);
        } else if (instruction.contains("记录") || instruction.contains("进展")) {
            AIActionDTO action = baseAction("ADD_ACTIVITY", "LOW", targetCase.getId(), false,
                    "律师明确要求登记案件进展");
            action.getPayload().put("title", "律师登记进展");
            action.getPayload().put("content", cleanInstruction(instruction));
            actions.add(action);
        }

        Matcher stageMatcher = STAGE.matcher(instruction);
        if (stageMatcher.find()) {
            AIActionDTO action = baseAction("CHANGE_STAGE", "HIGH", targetCase.getId(), true,
                    "案件阶段变更属于高风险操作，必须再次确认");
            action.getPayload().put("targetStage", stageMatcher.group(1).trim());
            action.getPayload().put("reason", "律师通过AI指令申请变更阶段");
            actions.add(action);
        }
        return actions;
    }

    private Optional<String> requiredClarification(List<AIActionDTO> actions) {
        for (AIActionDTO action : actions) {
            if ("CREATE_CALENDAR".equals(action.getActionType())) {
                if (action.getPayload().get("startTime") == null) {
                    return Optional.of("请补充具体日期和时间，例如“8月10日9:30”。");
                }
                if (!LocalDateTime.parse(String.valueOf(action.getPayload().get("startTime")))
                        .isAfter(LocalDateTime.now())) {
                    return Optional.of("开庭或听证时间已经过去，请补充未来的日期和时间。");
                }
                if (!StringUtils.hasText((String) action.getPayload().get("location"))) {
                    return Optional.of("请补充开庭或听证地点。");
                }
            }
            if ("CREATE_TODO".equals(action.getActionType()) && action.getPayload().get("dueTime") == null) {
                return Optional.of("请补充待办的具体截止日期和时间。");
            }
            if ("CREATE_TODO".equals(action.getActionType())
                    && !LocalDateTime.parse(String.valueOf(action.getPayload().get("dueTime")))
                    .isAfter(LocalDateTime.now())) {
                return Optional.of("待办截止时间已经过去，请补充未来的日期和时间。");
            }
        }
        return Optional.empty();
    }

    private void executeActions(AICaseCommand command, List<AIActionDTO> actions, Long userId) {
        caseService.assertCaseEditable(command.getCaseId(), userId);
        for (AIActionDTO action : actions) {
            switch (action.getActionType()) {
                case "CREATE_CALENDAR":
                    executeCalendar(command, action, userId);
                    break;
                case "CREATE_TODO":
                    executeTodo(command, action, userId);
                    break;
                case "ADD_ACTIVITY":
                    executeActivity(command, action, userId);
                    break;
                case "CHANGE_STAGE":
                    caseStageService.changeStatus(command.getCaseId(),
                            String.valueOf(action.getPayload().get("targetStage")),
                            String.valueOf(action.getPayload().get("reason")), userId);
                    activityService.create(command.getCaseId(), "STAGE_CHANGED", "案件阶段变更",
                            command.getInstruction(), LocalDateTime.now(), "AI_COMMAND", command.getId(),
                            userId, String.valueOf(action.getPayload().get("targetStage")), write(action.getPayload()));
                    break;
                default:
                    throw new IllegalArgumentException("AI动作不在白名单中: " + action.getActionType());
            }
        }
        command.setStatus("AUTO_EXECUTED");
        command.setExecutedAt(LocalDateTime.now());
        commandRepository.save(command);
    }

    private void executeCalendar(AICaseCommand command, AIActionDTO action, Long userId) {
        CalendarDTO dto = new CalendarDTO();
        dto.setTitle(String.valueOf(action.getPayload().get("title")));
        dto.setCalendarType(String.valueOf(action.getPayload().get("calendarType")));
        dto.setStartTime(LocalDateTime.parse(String.valueOf(action.getPayload().get("startTime"))));
        dto.setEndTime(LocalDateTime.parse(String.valueOf(action.getPayload().get("endTime"))));
        dto.setLocation(String.valueOf(action.getPayload().get("location")));
        dto.setCaseId(command.getCaseId());
        dto.setReminder(true);
        dto.setReminderMinutes(120);
        CalendarDTO saved = calendarService.createCalendar(dto, userId);
        calendarReminderService.scheduleUpcomingHearingReminders(saved.getId(), dto.getStartTime());
        String content = "已登记" + dto.getCalendarType() + "：" + dto.getStartTime() + "，地点：" + dto.getLocation();
        timelineService.createSystemTimeline(command.getCaseId(), "AI_CALENDAR_CREATED", content);
        activityService.create(command.getCaseId(), "HEARING", dto.getTitle(), content,
                dto.getStartTime(), "AI_COMMAND", command.getId(), userId, null, write(action.getPayload()));
    }

    private void executeTodo(AICaseCommand command, AIActionDTO action, Long userId) {
        TodoDTO dto = new TodoDTO();
        dto.setTitle(String.valueOf(action.getPayload().get("title")));
        dto.setDescription("由案件AI根据律师明确指令建立");
        dto.setDueDate(LocalDateTime.parse(String.valueOf(action.getPayload().get("dueTime"))));
        dto.setPriority(String.valueOf(action.getPayload().get("priority")));
        dto.setAssigneeId(userId);
        dto.setCaseId(command.getCaseId());
        dto.setReminder(true);
        TodoDTO saved = todoService.createTodo(dto, userId);
        activityService.create(command.getCaseId(), "TODO_CREATED", dto.getTitle(), dto.getDescription(),
                LocalDateTime.now(), "AI_COMMAND", command.getId(), userId, null,
                write(Map.of("todoId", saved.getId(), "dueTime", dto.getDueDate().toString())));
    }

    private void executeActivity(AICaseCommand command, AIActionDTO action, Long userId) {
        String title = String.valueOf(action.getPayload().get("title"));
        String content = String.valueOf(action.getPayload().get("content"));
        timelineService.createSystemTimeline(command.getCaseId(), "AI_PROGRESS_RECORDED", content);
        activityService.create(command.getCaseId(), "PROGRESS", title, content, LocalDateTime.now(),
                "AI_COMMAND", command.getId(), userId, null, write(action.getPayload()));
    }

    private LocalDateTime parseDateTime(String instruction) {
        Matcher cn = CN_DATE_TIME.matcher(instruction);
        if (cn.find()) {
            try {
                boolean yearSpecified = cn.group(1) != null;
                int year = yearSpecified ? Integer.parseInt(cn.group(1)) : LocalDate.now().getYear();
                int hour = normalizeHour(Integer.parseInt(cn.group(5)), cn.group(4));
                int minute = cn.group(6) == null ? 0 : Integer.parseInt(cn.group(6));
                LocalDateTime result = LocalDateTime.of(year, Integer.parseInt(cn.group(2)),
                        Integer.parseInt(cn.group(3)), hour, minute);
                if (!yearSpecified && !result.isAfter(LocalDateTime.now())) {
                    result = result.plusYears(1);
                }
                return result;
            } catch (DateTimeException | NumberFormatException ignored) {
                return null;
            }
        }
        Matcher relative = RELATIVE_DATE_TIME.matcher(instruction);
        if (relative.find()) {
            try {
                String relativeDay = relative.group(1);
                int days = "明天".equals(relativeDay) ? 1 : "后天".equals(relativeDay) ? 2 : 0;
                String period = "今晚".equals(relativeDay) ? "晚上" : relative.group(2);
                int hour = normalizeHour(Integer.parseInt(relative.group(3)), period);
                int minute = relative.group(4) == null ? 0 : Integer.parseInt(relative.group(4));
                return LocalDate.now().plusDays(days).atTime(hour, minute);
            } catch (DateTimeException | NumberFormatException ignored) {
                return null;
            }
        }
        Matcher iso = ISO_DATE_TIME.matcher(instruction);
        if (iso.find()) {
            try {
                return LocalDateTime.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)),
                        Integer.parseInt(iso.group(3)), Integer.parseInt(iso.group(4)), Integer.parseInt(iso.group(5)));
            } catch (DateTimeException | NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int normalizeHour(int hour, String period) {
        if (("下午".equals(period) || "晚上".equals(period)) && hour < 12) return hour + 12;
        return hour;
    }

    private AICaseCommandResponse clarify(AICaseCommand command, String message) {
        command.setStatus("NEEDS_CLARIFICATION");
        command.setClarification(message);
        commandRepository.save(command);
        return toResponse(command);
    }

    private AIActionDTO baseAction(String type, String risk, Long caseId, boolean confirmation, String reason) {
        AIActionDTO action = new AIActionDTO();
        action.setActionType(type);
        action.setRiskLevel(risk);
        action.setCaseId(caseId);
        action.setConfidence(0.98);
        action.setRequiresConfirmation(confirmation);
        action.setReason(reason);
        return action;
    }

    private String cleanInstruction(String instruction) {
        return instruction.replaceFirst("^(请|帮我|请帮我)", "").trim();
    }

    private AICaseCommandResponse toResponse(AICaseCommand command) {
        AICaseCommandResponse response = new AICaseCommandResponse();
        response.setCommandId(command.getId());
        response.setStatus(command.getStatus());
        response.setClarification(command.getClarification());
        response.setCaseId(command.getCaseId());
        if (command.getCaseId() != null) {
            caseRepository.findById(command.getCaseId()).ifPresent(item -> response.setCaseName(item.getCaseName()));
        }
        response.setActions(readActions(command.getActionsJson()));
        return response;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("AI动作序列化失败", e);
        }
    }

    private List<AIActionDTO> readActions(String value) {
        if (!StringUtils.hasText(value)) return new ArrayList<>();
        try {
            return objectMapper.readValue(value, new TypeReference<List<AIActionDTO>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("AI动作记录损坏", e);
        }
    }
}
