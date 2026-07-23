package com.lawfirm.service;

import com.lawfirm.dto.ClientDTO;
import com.lawfirm.dto.ConflictCheckHitDTO;
import com.lawfirm.dto.ConflictCheckRecordDTO;
import com.lawfirm.dto.ConflictCheckResultDTO;
import com.lawfirm.dto.ConflictCheckReviewRequest;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.ConflictCheckRecord;
import com.lawfirm.entity.Party;
import com.lawfirm.entity.User;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final com.lawfirm.repository.CommunicationRecordRepository communicationRecordRepository;
    private final com.lawfirm.repository.PartyRepository partyRepository;
    private final com.lawfirm.repository.CaseMemberRepository caseMemberRepository;
    private final com.lawfirm.repository.DepartmentRepository departmentRepository;
    private final com.lawfirm.repository.ConflictCheckRecordRepository conflictCheckRecordRepository;
    private final UserPermissionService userPermissionService;
    private final ConflictWaiverAttachmentService conflictWaiverAttachmentService;
    private final ClientSubjectRelationService clientSubjectRelationService;

    /**
     * 创建客户
     */
    @Transactional
    public ClientDTO createClient(ClientDTO dto, Long userId) {
        validateClientBeforeSave(dto, null);

        Client client = new Client();
        applyClientFields(client, dto);
        client.setOwnerId(dto.getOwnerId() != null ? dto.getOwnerId() : userId);
        if (!StringUtils.hasText(client.getClientOwnerIds()) && client.getOwnerId() != null) {
            client.setClientOwnerIds(String.valueOf(client.getOwnerId()));
        }

        client = clientRepository.save(client);
        log.info("创建客户成功: {}", client.getId());

        return convertToDTO(client);
    }

    /**
     * 更新客户
     */
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));

        validateClientBeforeSave(dto, id);
        applyClientFields(client, dto);
        client.setOwnerId(dto.getOwnerId() != null ? dto.getOwnerId() : client.getOwnerId());

        client = clientRepository.save(client);
        log.info("更新客户成功: {}", id);

        return convertToDTO(client);
    }

    /**
     * 删除客户
     */
    @Transactional
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new IllegalArgumentException("客户不存在");
        }

        // 检查是否有关联案件（使用数据库查询优化）
        List<Case> relatedCases = caseRepository.findByClientIdOrderByCreatedAtDesc(id);

        if (!relatedCases.isEmpty()) {
            throw new IllegalArgumentException("该客户存在关联案件，无法删除");
        }

        clientRepository.deleteById(id);
        log.info("删除客户成功: {}", id);
    }

    /**
     * 根据ID查询客户
     */
    public ClientDTO getClientById(Long id) {
        return clientRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    /**
     * 根据ID查询当前用户可见客户
     */
    public ClientDTO getClientById(Long id, Long currentUserId) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", currentUserId));
        if (!canAccessClient(client, currentUser)) {
            throw new IllegalArgumentException("无权访问非本部门相关客户");
        }
        ClientDTO dto = convertToDTO(client);
        boolean relationEditable = canEditClientByRelation(client, currentUser);
        dto.setCanEdit(relationEditable && userPermissionService.hasPermission(currentUser, "CLIENT_EDIT"));
        dto.setCanDelete(relationEditable && userPermissionService.hasPermission(currentUser, "CLIENT_DELETE"));
        return dto;
    }

    /**
     * 搜索客户
     */
    public List<ClientDTO> searchClients(String keyword) {
        // 使用数据库查询优化，只查询未删除的客户
        return clientRepository.findByDeletedFalse().stream()
                .filter(c -> containsKeyword(c.getClientName(), keyword)
                        || containsKeyword(c.getPhone(), keyword)
                        || containsKeyword(c.getIdCard(), keyword)
                        || containsKeyword(c.getCreditCode(), keyword))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 搜索当前用户可见客户
     */
    public List<ClientDTO> searchClients(String keyword, Long currentUserId) {
        return searchClients(keyword).stream()
                .filter(client -> canAccessClient(client.getId(), currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 按类型查询客户
     */
    public List<ClientDTO> getClientsByType(String clientType) {
        // 使用数据库查询优化
        return clientRepository.findByClientTypeAndDeletedFalse(clientType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClientDTO> getClientsByType(String clientType, Long currentUserId) {
        return getClientsByType(clientType).stream()
                .filter(client -> canAccessClient(client.getId(), currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 按状态查询客户
     */
    public List<ClientDTO> getClientsByStatus(String status) {
        // 使用数据库查询优化
        return clientRepository.findByStatusAndDeletedFalse(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClientDTO> getClientsByStatus(String status, Long currentUserId) {
        return getClientsByStatus(status).stream()
                .filter(client -> canAccessClient(client.getId(), currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的客户
     */
    public List<ClientDTO> getClientsByOwner(Long ownerId) {
        // 使用数据库查询优化
        return clientRepository.findByOwnerIdAndDeletedFalse(ownerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClientDTO> getClientsByOwner(Long ownerId, Long currentUserId) {
        return getClientsByOwner(ownerId).stream()
                .filter(client -> canAccessClient(client.getId(), currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询客户
     */
    public com.lawfirm.util.PageResult<ClientDTO> getClients(int page, int size) {
        return paginateClients(clientRepository.findByDeletedFalse(), page, size);
    }

    /**
     * 分页查询当前用户可见客户
     */
    public com.lawfirm.util.PageResult<ClientDTO> getClients(int page, int size, Long currentUserId) {
        return getClients(page, size, currentUserId, null, null, null);
    }

    /**
     * 分页查询当前用户可见客户。筛选必须在分页前执行，避免只过滤当前页。
     */
    public com.lawfirm.util.PageResult<ClientDTO> getClients(
            int page,
            int size,
            Long currentUserId,
            String keyword,
            String clientType,
            Long departmentId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", currentUserId));
        String normalizedType = StringUtils.hasText(clientType) ? clientType.trim() : null;

        List<Client> filteredClients = clientRepository.findByDeletedFalse().stream()
                .filter(client -> canAccessClient(client, currentUser))
                .filter(client -> normalizedType == null || normalizedType.equals(client.getClientType()))
                .filter(client -> departmentId == null || departmentId.equals(client.getDepartmentId()))
                .filter(client -> matchesClientKeyword(client, keyword))
                .collect(Collectors.toList());

        return paginateClients(filteredClients, page, size);
    }

    private com.lawfirm.util.PageResult<ClientDTO> paginateClients(
            List<Client> clients, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        List<Client> sortedClients = new ArrayList<>(clients);
        sortedClients.sort(Comparator
                .comparing(Client::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Client::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        int start = safePage * safeSize;
        int end = Math.min(start + safeSize, sortedClients.size());
        List<ClientDTO> records = start >= sortedClients.size()
                ? new ArrayList<>()
                : sortedClients.subList(start, end).stream().map(this::convertToDTO).collect(Collectors.toList());
        return com.lawfirm.util.PageResult.of(
                (long) safePage, (long) safeSize, (long) sortedClients.size(), records);
    }

    private boolean matchesClientKeyword(Client client, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (containsNormalized(client.getClientName(), normalizedKeyword)
                || containsNormalized(client.getCreditCode(), normalizedKeyword)
                || containsNormalized(client.getIdCard(), normalizedKeyword)
                || containsNormalized(client.getSource(), normalizedKeyword)) {
            return true;
        }

        List<Long> relatedUserIds = new ArrayList<>();
        relatedUserIds.addAll(parseIds(client.getSourceUserIds()));
        relatedUserIds.addAll(parseIds(client.getClientOwnerIds()));
        if (client.getOwnerId() != null) {
            relatedUserIds.add(client.getOwnerId());
        }
        return relatedUserIds.stream()
                .distinct()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(user -> containsNormalized(user.getRealName(), normalizedKeyword)
                        || containsNormalized(user.getUsername(), normalizedKeyword));
    }

    private boolean containsNormalized(String value, String normalizedKeyword) {
        return StringUtils.hasText(value)
                && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    public void assertClientVisible(Long clientId, Long currentUserId) {
        if (!canAccessClient(clientId, currentUserId)) {
            throw new IllegalArgumentException("无权访问非本部门相关客户");
        }
    }

    public void assertClientEditable(Long clientId, Long currentUserId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("客户", clientId));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", currentUserId));
        if (canEditClientByRelation(client, currentUser)) {
            return;
        }
        throw new IllegalArgumentException("仅案源人、承办人、主任或管理员可修改该客户");
    }

    private boolean canEditClientByRelation(Client client, User currentUser) {
        if (client == null || Boolean.TRUE.equals(client.getDeleted())) {
            return false;
        }
        if (isDevelopmentAdmin(currentUser) || "主任".equals(currentUser.getPosition())) {
            return true;
        }
        List<Long> relatedUserIds = new ArrayList<>();
        relatedUserIds.addAll(parseIds(client.getSourceUserIds()));
        relatedUserIds.addAll(parseIds(client.getClientOwnerIds()));
        if (client.getOwnerId() != null) {
            relatedUserIds.add(client.getOwnerId());
        }
        if (relatedUserIds.stream().anyMatch(currentUser.getId()::equals)) {
            return true;
        }
        return false;
    }

    public boolean canAccessClient(Long clientId, Long currentUserId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("客户", clientId));
        return canAccessClient(client, currentUserId);
    }

    public boolean canAccessClient(Client client, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", currentUserId));
        return canAccessClient(client, currentUser);
    }

    private boolean canAccessClient(Client client, User currentUser) {
        if (client == null || Boolean.TRUE.equals(client.getDeleted())) {
            return false;
        }
        if (isDevelopmentAdmin(currentUser)) {
            return true;
        }
        if (hasAllClientViewAccess(currentUser)) {
            return true;
        }
        List<Long> relatedUserIds = new ArrayList<>();
        relatedUserIds.addAll(parseIds(client.getSourceUserIds()));
        relatedUserIds.addAll(parseIds(client.getClientOwnerIds()));
        if (client.getOwnerId() != null) {
            relatedUserIds.add(client.getOwnerId());
        }
        if (relatedUserIds.isEmpty()) {
            return currentUser.getDepartmentId() != null
                    && currentUser.getDepartmentId().equals(client.getDepartmentId());
        }
        Long currentUserId = currentUser.getId();
        if (currentUserId != null && relatedUserIds.stream().distinct().anyMatch(currentUserId::equals)) {
            return true;
        }
        Long currentDepartmentId = currentUser.getDepartmentId();
        if (currentDepartmentId == null) {
            return false;
        }
        if (currentDepartmentId.equals(client.getDepartmentId())) {
            return true;
        }
        return relatedUserIds.stream()
                .distinct()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(relatedUser -> currentDepartmentId.equals(relatedUser.getDepartmentId()));
    }

    private boolean isDevelopmentAdmin(User user) {
        return user != null && "admin".equals(user.getUsername());
    }

    private boolean hasAllClientViewAccess(User user) {
        if (user == null) {
            return false;
        }
        return userPermissionService.hasPermission(user, "CLIENT_VIEW_ALL");
    }

    /**
     * 利益冲突检索
     */
    public ClientDTO checkConflict(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        ConflictCheckResultDTO detailed = performConflictCheck(convertToDTO(client), clientId);
        ClientDTO result = convertToDTO(client);
        result.setHasConflict(detailed.getHasConflict());
        result.setConflictLevel(detailed.getConflictLevel());
        result.setConflictDescription(detailed.getConflictDescription());
        result.setSimilarClientNames(detailed.getSimilarClientNames());
        result.setConflictCaseIds(detailed.getConflictCaseIds());
        return result;
    }

    public ClientDTO checkConflict(Long clientId, Long currentUserId) {
        assertClientVisible(clientId, currentUserId);
        return checkConflict(clientId);
    }

    /**
     * 客户建档前利益冲突预检。
     */
    public ConflictCheckResultDTO checkConflictPreview(ClientDTO dto) {
        return performConflictCheck(dto, null);
    }

    @Transactional
    public ConflictCheckResultDTO checkConflictPreviewAndRecord(ClientDTO dto, Long checkedBy) {
        return checkConflictPreviewAndRecord(dto, checkedBy, null);
    }

    @Transactional
    public ConflictCheckResultDTO checkConflictPreviewAndRecord(ClientDTO dto, Long checkedBy, Long caseId) {
        ConflictCheckResultDTO result = performConflictCheck(dto, null);
        ConflictCheckRecord record = saveConflictCheckRecord(dto, result, checkedBy, caseId);
        result.setRecordId(record.getId());
        result.setReportNo(formatReportNo(record));
        result.setCheckedAt(record.getCreatedAt());
        return result;
    }

    public List<ConflictCheckRecordDTO> getConflictCheckRecords(String subjectName, Long currentUserId) {
        if (!StringUtils.hasText(subjectName)) {
            return new ArrayList<>();
        }
        return conflictCheckRecordRepository.findBySubjectNameOrderByCreatedAtDesc(subjectName).stream()
                .map(record -> toConflictCheckRecordDTO(record, currentUserId))
                .collect(Collectors.toList());
    }

    public List<ConflictCheckRecordDTO> getConflictCheckRecordsByCaseId(Long caseId, Long currentUserId) {
        if (caseId == null) {
            return new ArrayList<>();
        }
        return conflictCheckRecordRepository.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
                .map(record -> toConflictCheckRecordDTO(record, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConflictCheckRecordDTO reviewConflictCheck(Long recordId, ConflictCheckReviewRequest request, Long reviewerId) {
        ConflictCheckRecord record = conflictCheckRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("利冲检查记录不存在"));
        if ("COMPLETED".equals(record.getReviewStatus())) {
            throw new IllegalArgumentException("该利冲检查已完成正式审查，不允许覆盖原结论；如需复核请重新发起检查");
        }
        validateConflictReview(record, request);
        record.setReviewStatus("COMPLETED");
        record.setReviewDecision(request.getDecision().trim().toUpperCase());
        record.setReviewConclusion(request.getConclusion().trim());
        record.setWaiverBasis(StringUtils.hasText(request.getWaiverBasis()) ? request.getWaiverBasis().trim() : null);
        record.setReviewedBy(reviewerId);
        record.setReviewedAt(java.time.LocalDateTime.now());
        return toConflictCheckRecordDTO(conflictCheckRecordRepository.save(record), reviewerId);
    }

    private ConflictCheckResultDTO performConflictCheck(ClientDTO dto, Long excludedClientId) {
        ConflictCheckResultDTO result = new ConflictCheckResultDTO();
        result.setClientName(dto.getClientName());
        result.setHasConflict(false);
        result.setConflictLevel("NONE");

        if (!StringUtils.hasText(dto.getClientName())) {
            result.setConflictDescription("请输入拟签约客户姓名或名称。 ");
            return result;
        }

        String subjectName = dto.getClientName().trim();
        String normalized = normalizeClientName(subjectName);
        List<Client> clients = clientRepository.findByDeletedFalse().stream()
                .filter(c -> excludedClientId == null || !excludedClientId.equals(c.getId()))
                .collect(Collectors.toList());
        List<Party> parties = partyRepository.findByDeletedFalse();

        List<Client> exactMatches = clients.stream()
                .filter(c -> isExactName(subjectName, c.getClientName()) || isSameIdentity(dto, c))
                .collect(Collectors.toList());

        for (Client existing : exactMatches) {
            result.getHits().add(ConflictCheckHitDTO.builder()
                    .sourceType("CLIENT")
                    .subjectName(existing.getClientName())
                    .subjectRole(existing.getClientRole())
                    .matchType(isSameIdentity(dto, existing) ? "IDENTITY" : "EXACT_NAME")
                    .riskLevel(isOpposingRole(dto.getClientRole(), existing.getClientRole()) ? "DIRECT" : "EXISTING")
                    .relatedCaseCount(countCasesByPartyName(existing.getClientName()))
                    .reason(isSameIdentity(dto, existing) ? "证件号码或统一社会信用代码一致" : "客户库名称完全一致")
                    .build());
            if (isOpposingRole(dto.getClientRole(), existing.getClientRole())) {
                result.setHasConflict(true);
                result.setConflictLevel("DIRECT");
                result.setConflictDescription("存在直接利益冲突：客户库中已存在相同客户名称，且既有客户角色为“"
                        + nullToBlank(existing.getClientRole()) + "”，本次拟录入角色为“"
                        + nullToBlank(dto.getClientRole()) + "”。请停止建档并由行政管理进行利冲审查。");
                result.getConflictCaseIds().addAll(findCaseIdsByPartyName(existing.getClientName()));
            }
        }

        if (!exactMatches.isEmpty() && !"DIRECT".equals(result.getConflictLevel())) {
            result.setConflictLevel("EXISTING");
            result.setConflictDescription("客户库中存在相同主体记录，需由行政管理结合既有委托关系进一步核对。系统不会因利冲命中自动开放客户详情。");
        }

        Map<String, List<Party>> exactPartyGroups = parties.stream()
                .filter(p -> isExactName(subjectName, p.getName()) || isSameIdentity(dto, p))
                .collect(Collectors.groupingBy(
                        p -> p.getName() + "|" + nullToBlank(p.getPartyRole()),
                        LinkedHashMap::new,
                        Collectors.toList()));
        exactPartyGroups.forEach((key, group) -> {
            Party sample = group.get(0);
            boolean opposing = isOpposingRole(dto.getClientRole(), sample.getPartyRole());
            result.getHits().add(ConflictCheckHitDTO.builder()
                    .sourceType("CASE_PARTY")
                    .subjectName(sample.getName())
                    .subjectRole(sample.getPartyRole())
                    .matchType(isSameIdentity(dto, sample) ? "IDENTITY" : "EXACT_NAME")
                    .riskLevel(opposing ? "DIRECT" : "CASE_PARTY")
                    .relatedCaseCount((int) group.stream().map(Party::getCaseId).distinct().count())
                    .reason("该主体曾作为案件当事人出现，需核对其在既有案件中的立场")
                    .build());
            group.stream().map(Party::getCaseId).distinct().forEach(result.getConflictCaseIds()::add);
            if (opposing) {
                result.setHasConflict(true);
                result.setConflictLevel("DIRECT");
            } else if (!"DIRECT".equals(result.getConflictLevel()) && "NONE".equals(result.getConflictLevel())) {
                result.setHasConflict(true);
                result.setConflictLevel("CASE_PARTY");
            }
        });

        addRelatedSubjectHits(dto, result, clients, subjectName);

        List<String> similarNames = new ArrayList<>();
        clients.stream()
                .filter(c -> !isExactName(subjectName, c.getClientName()))
                .filter(c -> isSimilarName(normalized, normalizeClientName(c.getClientName())))
                .map(Client::getClientName)
                .distinct()
                .limit(5)
                .forEach(similarNames::add);
        parties.stream()
                .filter(p -> !isExactName(subjectName, p.getName()))
                .filter(p -> isSimilarName(normalized, normalizeClientName(p.getName())))
                .map(Party::getName)
                .distinct()
                .filter(name -> !similarNames.contains(name))
                .limit(Math.max(0, 5 - similarNames.size()))
                .forEach(similarNames::add);

        if (!similarNames.isEmpty()) {
            similarNames.forEach(name -> result.getHits().add(ConflictCheckHitDTO.builder()
                    .sourceType("SIMILAR_SUBJECT")
                    .subjectName(name)
                    .matchType("SIMILAR_NAME")
                    .riskLevel("SIMILAR")
                    .reason("名称去除地区、组织形式和标点后高度相似")
                    .build()));
        }
        if (!similarNames.isEmpty() && "NONE".equals(result.getConflictLevel())) {
            result.setConflictLevel("SIMILAR");
            result.setSimilarClientNames(similarNames);
            result.setConflictDescription("发现高度相似客户：" + String.join("、", similarNames)
                    + "。请再次核实客户名称是否正确；核实后可继续新增。");
        } else {
            result.setSimilarClientNames(similarNames);
        }

        result.setConflictCaseIds(result.getConflictCaseIds().stream().distinct().collect(Collectors.toList()));
        if ("DIRECT".equals(result.getConflictLevel())) {
            result.setConflictDescription("发现直接利益冲突线索。请暂停签约或立案，提交行政管理进行正式审查。 ");
            result.setRecommendation("暂停办理，由行政管理核对既有委托关系、案件立场和书面豁免条件。 ");
        } else if ("CASE_PARTY".equals(result.getConflictLevel())) {
            result.setConflictDescription("该主体曾出现在案件当事人库中，需要人工确认其案件角色和现有委托关系。 ");
            result.setRecommendation("联系行政管理完成正式利冲审查，不要仅凭本次初筛直接签约。 ");
        } else if ("EXISTING".equals(result.getConflictLevel())) {
            result.setRecommendation("核对是否为同一主体，并由既有案源人或承办人确认服务关系。 ");
        } else if ("RELATED".equals(result.getConflictLevel())) {
            result.setConflictDescription("主体关系图谱发现关联企业、控制人、曾用名或其他关联主体，需要核对既有委托关系和实际利益是否一致。 ");
            result.setRecommendation("由行政管理核对关联主体的控制关系、共同利益和既有案件立场后作出正式结论。 ");
        } else if ("SIMILAR".equals(result.getConflictLevel())) {
            result.setRecommendation("核对主体全称、曾用名及统一社会信用代码后再继续。 ");
        } else {
            result.setConflictDescription("全所客户库及案件当事人库未发现同名或高度相似主体。 ");
            result.setRecommendation("可继续后续流程，但正式立案仍应完成行政利冲审查。 ");
        }

        return result;
    }

    private ConflictCheckRecord saveConflictCheckRecord(
            ClientDTO request, ConflictCheckResultDTO result, Long checkedBy, Long caseId) {
        if (!StringUtils.hasText(request.getClientName())) {
            throw new IllegalArgumentException("检查对象不能为空");
        }
        ConflictCheckRecord record = new ConflictCheckRecord();
        record.setSubjectName(request.getClientName().trim());
        record.setClientType(request.getClientType());
        record.setClientRelationship(request.getClientRelationship());
        record.setClientRole(request.getClientRole());
        record.setIdCard(request.getIdCard());
        record.setCreditCode(request.getCreditCode());
        record.setCheckedBy(checkedBy);
        record.setCaseId(caseId);
        record.setConflictLevel(result.getConflictLevel());
        record.setConclusion(result.getConflictDescription());
        record.setSimilarNames(result.getSimilarClientNames() == null ? null : String.join(",", result.getSimilarClientNames()));
        record.setMatchedRelatedSubjects(result.getHits().stream()
                .filter(hit -> "RELATED_ENTITY".equals(hit.getSourceType()))
                .map(hit -> nullToBlank(hit.getSubjectRole()) + ":" + nullToBlank(hit.getSubjectName()))
                .distinct()
                .collect(Collectors.joining(",")));
        record.setMatchedCaseIds(result.getConflictCaseIds() == null ? null : result.getConflictCaseIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        record.setMatchedClientIds(clientRepository.findByDeletedFalse().stream()
                .filter(client -> isExactName(request.getClientName(), client.getClientName()) || isSameIdentity(request, client))
                .map(Client::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        return conflictCheckRecordRepository.save(record);
    }

    public String generateConflictCheckReport(Long recordId, Long currentUserId) {
        ConflictCheckRecord record = conflictCheckRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("利冲检查记录不存在"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        boolean canRead = canReadConflictRecord(record, currentUser);
        if (!canRead) {
            throw new IllegalArgumentException("无权下载该利冲检查报告");
        }

        String operator = resolveUserName(record.getCheckedBy());
        return String.join("\n",
                "ZGAI 利益冲突检查报告",
                "报告编号：" + formatReportNo(record),
                "检查对象：" + record.getSubjectName(),
                "检查时间：" + (record.getCreatedAt() == null ? "-" : record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                "检查人员：" + operator,
                "检查范围：全所客户库、案件当事人库",
                "风险等级：" + nullToBlank(record.getConflictLevel()),
                "客户库命中数：" + countCsvItems(record.getMatchedClientIds()),
                "关联案件数：" + countCsvItems(record.getMatchedCaseIds()),
                "相似名称：" + (StringUtils.hasText(record.getSimilarNames()) ? record.getSimilarNames() : "无"),
                "关联主体：" + (StringUtils.hasText(record.getMatchedRelatedSubjects()) ? record.getMatchedRelatedSubjects() : "无"),
                "系统结论：" + (StringUtils.hasText(record.getConclusion()) ? record.getConclusion() : "无"),
                "正式审查状态：" + reviewStatusLabel(record.getReviewStatus()),
                "正式审查结论：" + reviewDecisionLabel(record.getReviewDecision()),
                "审查意见：" + (StringUtils.hasText(record.getReviewConclusion()) ? record.getReviewConclusion() : "尚未审查"),
                "豁免或处置依据：" + (StringUtils.hasText(record.getWaiverBasis()) ? record.getWaiverBasis() : "无"),
                "复核人员：" + (record.getReviewedBy() == null ? "-" : resolveUserName(record.getReviewedBy())),
                "复核时间：" + (record.getReviewedAt() == null ? "-" : record.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                "",
                "声明：系统初筛不替代行政管理人员依据律所制度作出的正式利益冲突审查结论。",
                "");
    }

    private ConflictCheckRecordDTO toConflictCheckRecordDTO(ConflictCheckRecord record, Long currentUserId) {
        ConflictCheckRecordDTO dto = new ConflictCheckRecordDTO();
        dto.setId(record.getId());
        dto.setReportNo(formatReportNo(record));
        dto.setSubjectName(record.getSubjectName());
        dto.setCaseId(record.getCaseId());
        dto.setCheckedByName(resolveUserName(record.getCheckedBy()));
        dto.setConflictLevel(record.getConflictLevel());
        dto.setConclusion(record.getConclusion());
        dto.setSimilarNames(record.getSimilarNames());
        dto.setMatchedClientCount(countCsvItems(record.getMatchedClientIds()));
        dto.setMatchedCaseCount(countCsvItems(record.getMatchedCaseIds()));
        dto.setMatchedRelatedSubjectCount(countCsvItems(record.getMatchedRelatedSubjects()));
        dto.setMatchedRelatedSubjects(record.getMatchedRelatedSubjects());
        dto.setCheckedAt(record.getCreatedAt());
        dto.setReviewStatus(StringUtils.hasText(record.getReviewStatus()) ? record.getReviewStatus() : "PENDING_REVIEW");
        dto.setReviewDecision(record.getReviewDecision());
        dto.setReviewConclusion(record.getReviewConclusion());
        dto.setWaiverBasis(record.getWaiverBasis());
        dto.setReviewedByName(record.getReviewedBy() == null ? null : resolveUserName(record.getReviewedBy()));
        dto.setReviewedAt(record.getReviewedAt());
        dto.setArchivedDocumentId(record.getArchivedDocumentId());
        dto.setArchivedAt(record.getArchivedAt());
        dto.setWaiverAttachments(conflictWaiverAttachmentService.list(record.getId()));
        User currentUser = currentUserId == null ? null : userRepository.findById(currentUserId).orElse(null);
        dto.setCanDownload(currentUser != null && canReadConflictRecord(record, currentUser));
        return dto;
    }

    private void validateConflictReview(ConflictCheckRecord record, ConflictCheckReviewRequest request) {
        if (request == null || !StringUtils.hasText(request.getDecision())) {
            throw new IllegalArgumentException("请选择正式审查结论");
        }
        String decision = request.getDecision().trim().toUpperCase();
        Set<String> allowed = new HashSet<>(Arrays.asList("PASSED", "REJECTED", "CONDITIONAL"));
        if (!allowed.contains(decision)) {
            throw new IllegalArgumentException("正式审查结论无效");
        }
        if (!StringUtils.hasText(request.getConclusion())) {
            throw new IllegalArgumentException("请填写审查意见");
        }
        if ("CONDITIONAL".equals(decision) && !StringUtils.hasText(request.getWaiverBasis())) {
            throw new IllegalArgumentException("附条件通过必须填写书面豁免或风险处置依据");
        }
        if ("CONDITIONAL".equals(decision) && !conflictWaiverAttachmentService.hasAttachment(record.getId())) {
            throw new IllegalArgumentException("附条件通过必须上传书面豁免或风险处置依据原件");
        }
    }

    public void assertConflictRecordReadable(Long recordId, Long currentUserId) {
        ConflictCheckRecord record = conflictCheckRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("利冲检查记录不存在"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        if (!canReadConflictRecord(record, currentUser)) {
            throw new IllegalArgumentException("无权查看该利冲检查记录");
        }
    }

    public Long getConflictRecordCaseId(Long recordId) {
        return conflictCheckRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("利冲检查记录不存在"))
                .getCaseId();
    }

    private boolean canReadConflictRecord(ConflictCheckRecord record, User currentUser) {
        return currentUser != null && ((record.getCheckedBy() != null && record.getCheckedBy().equals(currentUser.getId()))
                || isDevelopmentAdmin(currentUser)
                || hasAllClientViewAccess(currentUser)
                || userPermissionService.hasPermission(currentUser, "CASE_FILING_REVIEW"));
    }

    private String reviewStatusLabel(String status) {
        return "COMPLETED".equals(status) ? "已完成" : "待行政审查";
    }

    private String reviewDecisionLabel(String decision) {
        if ("PASSED".equals(decision)) {
            return "无冲突，通过";
        }
        if ("REJECTED".equals(decision)) {
            return "存在冲突，不通过";
        }
        if ("CONDITIONAL".equals(decision)) {
            return "附条件通过";
        }
        return "尚未审查";
    }

    private String formatReportNo(ConflictCheckRecord record) {
        String date = record.getCreatedAt() == null
                ? "00000000"
                : record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("LC-%s-%06d", date, record.getId() == null ? 0L : record.getId());
    }

    private int countCsvItems(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        return (int) Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return "未知人员";
        }
        return userRepository.findById(userId)
                .map(User::getRealName)
                .filter(StringUtils::hasText)
                .orElse("未知人员");
    }

    private boolean isSameIdentity(ClientDTO request, Client existing) {
        return (StringUtils.hasText(request.getIdCard()) && request.getIdCard().equalsIgnoreCase(nullToBlank(existing.getIdCard())))
                || (StringUtils.hasText(request.getCreditCode()) && request.getCreditCode().equalsIgnoreCase(nullToBlank(existing.getCreditCode())));
    }

    private boolean isSameIdentity(ClientDTO request, Party existing) {
        return (StringUtils.hasText(request.getIdCard()) && request.getIdCard().equalsIgnoreCase(nullToBlank(existing.getIdCard())))
                || (StringUtils.hasText(request.getCreditCode()) && request.getCreditCode().equalsIgnoreCase(nullToBlank(existing.getCreditCode())));
    }

    private void addRelatedSubjectHits(
            ClientDTO request, ConflictCheckResultDTO result, List<Client> clients, String subjectName) {
        Map<Long, Client> clientsById = clients.stream()
                .filter(client -> client.getId() != null)
                .collect(Collectors.toMap(Client::getId, client -> client, (left, right) -> left));
        Set<String> added = new HashSet<>();
        clientSubjectRelationService.findAllActive().forEach(relation -> {
            Client source = clientsById.get(relation.getSourceClientId());
            Client target = relation.getTargetClientId() == null ? null : clientsById.get(relation.getTargetClientId());
            String sourceName = source == null ? null : source.getClientName();
            String targetName = target == null ? relation.getTargetSubjectName() : target.getClientName();
            boolean matchesSource = isExactName(subjectName, sourceName)
                    || (source != null && isSameIdentity(request, source));
            boolean matchesTarget = isExactName(subjectName, targetName)
                    || (StringUtils.hasText(request.getCreditCode())
                    && request.getCreditCode().equalsIgnoreCase(nullToBlank(relation.getTargetCreditCode())))
                    || (target != null && isSameIdentity(request, target));
            if (!matchesSource && !matchesTarget) {
                return;
            }

            String relatedName = matchesSource ? targetName : sourceName;
            if (!StringUtils.hasText(relatedName)) {
                return;
            }
            String relationName = matchesSource
                    ? clientSubjectRelationService.relationTypeName(relation.getRelationType())
                    : clientSubjectRelationService.inverseRelationTypeName(relation.getRelationType());
            String key = relation.getId() + "|" + relatedName;
            if (!added.add(key)) {
                return;
            }
            result.getHits().add(ConflictCheckHitDTO.builder()
                    .sourceType("RELATED_ENTITY")
                    .subjectName(relatedName)
                    .subjectRole(relationName)
                    .matchType("RELATION_GRAPH")
                    .riskLevel("RELATED")
                    .relatedCaseCount(countCasesByPartyName(relatedName))
                    .reason("主体关系图谱显示该主体与检查对象存在“" + relationName + "”关系")
                    .build());
            result.getConflictCaseIds().addAll(findCaseIdsByPartyName(relatedName));
            result.setHasConflict(true);
            if ("NONE".equals(result.getConflictLevel()) || "SIMILAR".equals(result.getConflictLevel())) {
                result.setConflictLevel("RELATED");
            }
        });
    }

    private boolean isExactName(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private int countCasesByPartyName(String name) {
        return (int) partyRepository.findByNameAndDeletedFalse(name).stream()
                .map(Party::getCaseId)
                .distinct()
                .count();
    }

    /**
     * 关键词匹配
     */
    private boolean containsKeyword(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return text.toLowerCase().contains(keyword.toLowerCase());
    }

    private void validateClientBeforeSave(ClientDTO dto, Long currentClientId) {
        if ("委托人".equals(dto.getClientRelationship())) {
            if (dto.getDepartmentId() == null) {
                throw new IllegalArgumentException("委托人客户必须选择客户所属部门");
            }
            if (!StringUtils.hasText(dto.getSourceUserIds())) {
                throw new IllegalArgumentException("委托人客户必须选择案源人");
            }
            if (!StringUtils.hasText(dto.getClientOwnerIds())) {
                throw new IllegalArgumentException("委托人客户必须选择客户所属人");
            }
        }

        if ("个人".equals(dto.getClientType())) {
            if (StringUtils.hasText(dto.getIdCard())) {
                clientRepository.findByIdCardAndDeletedFalse(dto.getIdCard())
                        .filter(c -> currentClientId == null || !currentClientId.equals(c.getId()))
                        .ifPresent(c -> log.warn("身份证号码已存在，保留历史客户重复记录: {}", dto.getClientName()));
            }
            return;
        }
    }

    private void applyClientFields(Client client, ClientDTO dto) {
        client.setClientType(dto.getClientType());
        client.setClientName(dto.getClientName());
        client.setClientRelationship(dto.getClientRelationship());
        client.setClientRole(dto.getClientRole());
        client.setGender(dto.getGender());
        client.setEthnicity(dto.getEthnicity());
        client.setIdCard(dto.getIdCard());
        client.setCreditCode(dto.getCreditCode());
        client.setPhone(dto.getPhone());
        client.setEmail(dto.getEmail());
        client.setAddress(dto.getAddress());
        client.setContactPerson(dto.getContactPerson());
        client.setWechat(dto.getWechat());
        client.setLegalRepresentative(dto.getLegalRepresentative());
        client.setLegalRepresentativeIdCard(dto.getLegalRepresentativeIdCard());
        client.setInvoiceTitle(dto.getInvoiceTitle());
        client.setInvoiceTaxNo(dto.getInvoiceTaxNo());
        client.setInvoiceAddressPhone(dto.getInvoiceAddressPhone());
        client.setInvoiceBankAccount(dto.getInvoiceBankAccount());
        client.setOpposingLawyer(dto.getOpposingLawyer());
        client.setIndustry(dto.getIndustry());
        client.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        client.setSource(dto.getSource());
        client.setNotes(dto.getNotes());
        client.setDepartmentId(dto.getDepartmentId());
        client.setSourceUserIds(dto.getSourceUserIds());
        client.setClientOwnerIds(dto.getClientOwnerIds());
    }

    private List<Long> findCaseIdsByPartyName(String clientName) {
        return partyRepository.findByNameAndDeletedFalse(clientName).stream()
                .map(com.lawfirm.entity.Party::getCaseId)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isOpposingRole(String newRole, String existingRole) {
        if (!StringUtils.hasText(newRole) || !StringUtils.hasText(existingRole)) {
            return false;
        }
        newRole = normalizePartyRole(newRole);
        existingRole = normalizePartyRole(existingRole);
        Set<String> plaintiffSide = new HashSet<>(Arrays.asList("原告", "共同原告", "申请人", "上诉人", "债权人"));
        Set<String> defendantSide = new HashSet<>(Arrays.asList("被告", "共同被告", "被申请人", "被上诉人"));
        return (plaintiffSide.contains(newRole) && defendantSide.contains(existingRole))
                || (defendantSide.contains(newRole) && plaintiffSide.contains(existingRole));
    }

    private String normalizePartyRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }
        switch (role.trim().toUpperCase()) {
            case "PLAINTIFF": return "原告";
            case "CO_PLAINTIFF": return "共同原告";
            case "DEFENDANT": return "被告";
            case "CO_DEFENDANT": return "共同被告";
            case "APPLICANT": return "申请人";
            case "RESPONDENT": return "被申请人";
            case "APPELLANT": return "上诉人";
            case "APPELLEE": return "被上诉人";
            case "CREDITOR": return "债权人";
            case "DEBTOR": return "债务人";
            default: return role.trim();
        }
    }

    private String normalizeClientName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.replaceAll("[\\s（）()·,，。.-]", "")
                .replace("广东省", "")
                .replace("广东", "")
                .replace("佛山市", "佛山")
                .replace("广州市", "广州")
                .replace("深圳市", "深圳")
                .replace("有限责任公司", "")
                .replace("股份有限公司", "")
                .replace("有限公司", "")
                .replace("公司", "")
                .replace("律所", "")
                .toLowerCase();
    }

    private boolean isSimilarName(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }
        return left.length() >= 4 && right.length() >= 4 && (left.contains(right) || right.contains(left));
    }

    private String resolveDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return "未设置";
        }
        return departmentRepository.findById(departmentId).map(d -> d.getDeptName()).orElse("未设置");
    }

    private String resolveUserNames(String userIds, Long fallbackUserId) {
        String ids = StringUtils.hasText(userIds) ? userIds : (fallbackUserId == null ? "" : String.valueOf(fallbackUserId));
        List<String> names = parseIds(ids).stream()
                .map(id -> userRepository.findById(id).map(u -> u.getRealName()).orElse(String.valueOf(id)))
                .collect(Collectors.toList());
        return names.isEmpty() ? "未设置" : String.join("、", names);
    }

    private List<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return new ArrayList<>();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    /**
     * 转换为DTO
     */
    private ClientDTO convertToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setClientType(client.getClientType());
        dto.setClientName(client.getClientName());
        dto.setClientRelationship(client.getClientRelationship());
        dto.setClientRole(client.getClientRole());
        dto.setGender(client.getGender());
        dto.setEthnicity(client.getEthnicity());
        dto.setIdCard(client.getIdCard());
        dto.setCreditCode(client.getCreditCode());
        dto.setPhone(client.getPhone());
        dto.setEmail(client.getEmail());
        dto.setAddress(client.getAddress());
        dto.setContactPerson(client.getContactPerson());
        dto.setWechat(client.getWechat());
        dto.setLegalRepresentative(client.getLegalRepresentative());
        dto.setLegalRepresentativeIdCard(client.getLegalRepresentativeIdCard());
        dto.setInvoiceTitle(client.getInvoiceTitle());
        dto.setInvoiceTaxNo(client.getInvoiceTaxNo());
        dto.setInvoiceAddressPhone(client.getInvoiceAddressPhone());
        dto.setInvoiceBankAccount(client.getInvoiceBankAccount());
        dto.setOpposingLawyer(client.getOpposingLawyer());
        dto.setIndustry(client.getIndustry());
        dto.setStatus(client.getStatus());
        dto.setSource(client.getSource());
        dto.setNotes(client.getNotes());
        dto.setDepartmentId(client.getDepartmentId());
        dto.setOwnerId(client.getOwnerId());
        dto.setSourceUserIds(client.getSourceUserIds());
        dto.setClientOwnerIds(client.getClientOwnerIds());
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());

        if (client.getDepartmentId() != null) {
            departmentRepository.findById(client.getDepartmentId())
                    .ifPresent(dept -> dto.setDepartmentName(dept.getDeptName()));
        }

        // 加载负责人名称
        if (client.getOwnerId() != null) {
            userRepository.findById(client.getOwnerId()).ifPresent(u -> dto.setOwnerName(u.getRealName()));
        }
        dto.setSourceUserNames(resolveUserNames(client.getSourceUserIds(), null));
        dto.setClientOwnerNames(resolveUserNames(client.getClientOwnerIds(), client.getOwnerId()));

        // 当前系统通过案件当事人姓名关联客户和案件。
        List<com.lawfirm.entity.Party> parties = partyRepository.findByNameAndDeletedFalse(client.getClientName());
        int caseCount = (int) parties.stream()
                .map(com.lawfirm.entity.Party::getCaseId)
                .distinct()
                .count();
        dto.setCaseCount(caseCount);

        List<com.lawfirm.entity.CommunicationRecord> communications =
                communicationRecordRepository.findByClientIdOrderByCommunicationDateDesc(client.getId());
        dto.setCommunicationCount(communications.size());
        if (!communications.isEmpty()) {
            dto.setLastCommunicationDate(communications.get(0).getCommunicationDate().atStartOfDay());
        }

        return dto;
    }

    /**
     * 获取客户的案件列表
     */
    public List<com.lawfirm.vo.CaseListVO> getClientCases(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));

        // 通过Party表关联客户和案件（性能优化：使用数据库查询而非全表加载）
        List<com.lawfirm.entity.Party> parties = partyRepository.findByNameAndDeletedFalse(client.getClientName());

        // 同时兼容结构化 client_id 和历史按当事人名称建立的关联。
        Set<Long> caseIds = parties.stream()
                .map(com.lawfirm.entity.Party::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        caseRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(Case::getId)
                .filter(Objects::nonNull)
                .forEach(caseIds::add);

        // 查询对应的案件
        return caseIds.stream()
                .map(caseId -> caseRepository.findById(caseId))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .map(this::convertToCaseListVO)
                .collect(Collectors.toList());
    }

    public List<com.lawfirm.vo.CaseListVO> getClientCases(Long clientId, Long currentUserId) {
        assertClientVisible(clientId, currentUserId);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", currentUserId));
        return getClientCases(clientId).stream()
                .filter(caseItem -> canAccessRelatedCase(caseItem.getId(), currentUser))
                .collect(Collectors.toList());
    }

    private boolean canAccessRelatedCase(Long caseId, User currentUser) {
        Case caseEntity = caseRepository.findById(caseId).orElse(null);
        if (caseEntity == null || Boolean.TRUE.equals(caseEntity.getDeleted())) {
            return false;
        }
        if (isDevelopmentAdmin(currentUser) || "主任".equals(currentUser.getPosition())
                || (currentUser.getPosition() != null && currentUser.getPosition().startsWith("行政管理"))) {
            return true;
        }
        if (Objects.equals(caseEntity.getOwnerId(), currentUser.getId())) {
            return true;
        }
        List<com.lawfirm.entity.CaseMember> members = caseMemberRepository.findByCaseIdAndDeletedFalse(caseId);
        if (members.stream().map(com.lawfirm.entity.CaseMember::getUserId).anyMatch(currentUser.getId()::equals)) {
            return true;
        }
        String position = currentUser.getPosition();
        boolean departmentManager = "部门主管".equals(position) || "主管".equals(position) || "合伙人".equals(position);
        if (!departmentManager || currentUser.getDepartmentId() == null) {
            return false;
        }
        User owner = userRepository.findById(caseEntity.getOwnerId()).orElse(null);
        if (owner != null && Objects.equals(owner.getDepartmentId(), currentUser.getDepartmentId())) {
            return true;
        }
        return members.stream()
                .map(com.lawfirm.entity.CaseMember::getUserId)
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(user -> Objects.equals(user.getDepartmentId(), currentUser.getDepartmentId()));
    }

    /**
     * 获取客户的沟通记录
     */
    public List<com.lawfirm.entity.CommunicationRecord> getCommunications(Long clientId, int page, int size) {
        List<com.lawfirm.entity.CommunicationRecord> allRecords =
            communicationRecordRepository.findByClientIdOrderByCommunicationDateDesc(clientId);

        // 简单分页
        int start = page * size;
        int end = Math.min(start + size, allRecords.size());
        if (start >= allRecords.size()) {
            return new ArrayList<>();
        }
        List<com.lawfirm.entity.CommunicationRecord> records = new ArrayList<>(allRecords.subList(start, end));
        records.forEach(this::enrichCommunicationRecord);
        return records;
    }

    public List<com.lawfirm.entity.CommunicationRecord> getCommunications(Long clientId, int page, int size, Long currentUserId) {
        assertClientVisible(clientId, currentUserId);
        return getCommunications(clientId, page, size);
    }

    /**
     * 创建沟通记录
     */
    @Transactional
    public com.lawfirm.entity.CommunicationRecord createCommunication(Long clientId, com.lawfirm.dto.CommunicationRecordDTO dto, Long operatorId) {
        assertClientEditable(clientId, operatorId);
        if (!clientRepository.existsById(clientId)) {
            throw new IllegalArgumentException("客户不存在");
        }

        com.lawfirm.entity.CommunicationRecord record = new com.lawfirm.entity.CommunicationRecord();
        record.setClientId(clientId);
        record.setCommunicationType(dto.getCommunicationType());
        record.setCommunicationDate(dto.getCommunicationDate());
        record.setContent(dto.getContent());
        record.setNextFollowDate(dto.getNextFollowDate());
        record.setAttachments(dto.getAttachments());
        record.setOperatorId(operatorId);
        record = communicationRecordRepository.save(record);
        enrichCommunicationRecord(record);
        return record;
    }

    /**
     * 更新沟通记录
     */
    @Transactional
    public com.lawfirm.entity.CommunicationRecord updateCommunication(Long clientId, Long communicationId, com.lawfirm.dto.CommunicationRecordDTO dto) {
        com.lawfirm.entity.CommunicationRecord record = communicationRecordRepository.findById(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("沟通记录不存在"));

        if (!clientId.equals(record.getClientId())) {
            throw new IllegalArgumentException("沟通记录不属于当前客户");
        }

        record.setCommunicationType(dto.getCommunicationType());
        record.setCommunicationDate(dto.getCommunicationDate());
        record.setContent(dto.getContent());
        record.setNextFollowDate(dto.getNextFollowDate());
        record.setAttachments(dto.getAttachments());
        record = communicationRecordRepository.save(record);
        enrichCommunicationRecord(record);
        return record;
    }

    public com.lawfirm.entity.CommunicationRecord updateCommunication(Long clientId, Long communicationId, com.lawfirm.dto.CommunicationRecordDTO dto, Long currentUserId) {
        assertClientEditable(clientId, currentUserId);
        return updateCommunication(clientId, communicationId, dto);
    }

    /**
     * 删除沟通记录
     */
    @Transactional
    public void deleteCommunication(Long clientId, Long communicationId) {
        com.lawfirm.entity.CommunicationRecord record = communicationRecordRepository.findById(communicationId)
                .orElseThrow(() -> new IllegalArgumentException("沟通记录不存在"));

        if (!clientId.equals(record.getClientId())) {
            throw new IllegalArgumentException("沟通记录不属于当前客户");
        }

        communicationRecordRepository.delete(record);
    }

    public void deleteCommunication(Long clientId, Long communicationId, Long currentUserId) {
        assertClientEditable(clientId, currentUserId);
        deleteCommunication(clientId, communicationId);
    }

    /**
     * 转换为案件列表VO
     */
    private com.lawfirm.vo.CaseListVO convertToCaseListVO(Case caseEntity) {
        com.lawfirm.vo.CaseListVO vo = new com.lawfirm.vo.CaseListVO();
        vo.setId(caseEntity.getId());
        vo.setCaseName(caseEntity.getCaseName());
        vo.setCaseNumber(caseEntity.getCaseNumber());
        vo.setCaseType(caseEntity.getCaseType());
        vo.setStatus(caseEntity.getStatus());
        vo.setCurrentStage(caseEntity.getCurrentStage());
        vo.setCourt(caseEntity.getCourt());
        vo.setOwnerId(caseEntity.getOwnerId());
        vo.setOwnerName(resolveUserName(caseEntity.getOwnerId()));
        vo.setFilingDate(caseEntity.getFilingDate());
        vo.setAmount(caseEntity.getAmount());
        vo.setAttorneyFee(caseEntity.getAttorneyFee());
        vo.setCreatedAt(caseEntity.getCreatedAt() != null ?
            caseEntity.getCreatedAt().toLocalDate() : null);
        return vo;
    }

    private void enrichCommunicationRecord(com.lawfirm.entity.CommunicationRecord record) {
        if (record != null) {
            record.setOperatorName(resolveUserName(record.getOperatorId()));
        }
    }
}
