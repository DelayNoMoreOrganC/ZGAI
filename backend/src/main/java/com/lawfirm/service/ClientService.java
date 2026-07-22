package com.lawfirm.service;

import com.lawfirm.dto.ClientDTO;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.ConflictCheckRecord;
import com.lawfirm.entity.User;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Set<String> ALL_CLIENT_VIEW_USER_NAMES = new HashSet<>(Arrays.asList(
            "田颖思", "黄智明", "邝凤兰", "何俊慧", "吴兴印"
    ));

    private final ClientRepository clientRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final com.lawfirm.repository.CommunicationRecordRepository communicationRecordRepository;
    private final com.lawfirm.repository.PartyRepository partyRepository;
    private final com.lawfirm.repository.DepartmentRepository departmentRepository;
    private final com.lawfirm.repository.ConflictCheckRecordRepository conflictCheckRecordRepository;

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
        assertClientVisible(id, currentUserId);
        return getClientById(id);
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
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 使用数据库查询优化，只查询未删除的客户
        List<Client> allClients = clientRepository.findByDeletedFalse();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allClients.size());

        // 参数验证：防止空数据时subList参数错误
        List<Client> pageClients;
        if (start >= end || start >= allClients.size()) {
            pageClients = new java.util.ArrayList<>();
        } else {
            pageClients = allClients.subList(start, end);
        }

        List<ClientDTO> dtoList = pageClients.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.lawfirm.util.PageResult<>((long) page, (long) size, (long) allClients.size(), dtoList);
    }

    /**
     * 分页查询当前用户可见客户
     */
    public com.lawfirm.util.PageResult<ClientDTO> getClients(int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Client> visibleClients = clientRepository.findByDeletedFalse().stream()
                .filter(client -> canAccessClient(client, currentUserId))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), visibleClients.size());
        List<Client> pageClients;
        if (start >= end || start >= visibleClients.size()) {
            pageClients = new ArrayList<>();
        } else {
            pageClients = visibleClients.subList(start, end);
        }

        List<ClientDTO> dtoList = pageClients.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new com.lawfirm.util.PageResult<>((long) page, (long) size, (long) visibleClients.size(), dtoList);
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
        if (isDevelopmentAdmin(currentUser) || "主任".equals(currentUser.getPosition())) {
            return;
        }
        if (client == null || Boolean.TRUE.equals(client.getDeleted())) {
            throw new IllegalArgumentException("客户不存在");
        }
        List<Long> relatedUserIds = new ArrayList<>();
        relatedUserIds.addAll(parseIds(client.getSourceUserIds()));
        relatedUserIds.addAll(parseIds(client.getClientOwnerIds()));
        if (client.getOwnerId() != null) {
            relatedUserIds.add(client.getOwnerId());
        }
        if (relatedUserIds.stream().anyMatch(currentUserId::equals)) {
            return;
        }
        throw new IllegalArgumentException("仅案源人、承办人、主任或管理员可修改该客户");
    }

    public boolean canAccessClient(Long clientId, Long currentUserId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("客户", clientId));
        return canAccessClient(client, currentUserId);
    }

    public boolean canAccessClient(Client client, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", currentUserId));
        if (isDevelopmentAdmin(currentUser)) {
            return true;
        }
        if (hasAllClientViewAccess(currentUser)) {
            return true;
        }
        if (client == null || Boolean.TRUE.equals(client.getDeleted())) {
            return false;
        }
        List<Long> relatedUserIds = new ArrayList<>();
        relatedUserIds.addAll(parseIds(client.getSourceUserIds()));
        relatedUserIds.addAll(parseIds(client.getClientOwnerIds()));
        if (client.getOwnerId() != null) {
            relatedUserIds.add(client.getOwnerId());
        }
        if (relatedUserIds.isEmpty()) {
            return false;
        }
        return relatedUserIds.stream()
                .distinct()
                .anyMatch(userId -> userId.equals(currentUserId));
    }

    private boolean isDevelopmentAdmin(User user) {
        return user != null && "admin".equals(user.getUsername());
    }

    private boolean hasAllClientViewAccess(User user) {
        if (user == null) {
            return false;
        }
        String position = user.getPosition();
        return ALL_CLIENT_VIEW_USER_NAMES.contains(user.getUsername())
                || ALL_CLIENT_VIEW_USER_NAMES.contains(user.getRealName())
                || "主任".equals(position);
    }

    /**
     * 利益冲突检索
     */
    public ClientDTO checkConflict(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        return checkConflictPreview(convertToDTO(client), clientId);
    }

    /**
     * 客户建档前利益冲突预检。
     */
    public ClientDTO checkConflictPreview(ClientDTO dto) {
        return checkConflictPreview(dto, null, null);
    }

    public ClientDTO checkConflictPreviewAndRecord(ClientDTO dto, Long checkedBy) {
        ClientDTO result = checkConflictPreview(dto, null, checkedBy);
        saveConflictCheckRecord(dto, result, checkedBy);
        return result;
    }

    public List<ConflictCheckRecord> getConflictCheckRecords(String subjectName) {
        if (!StringUtils.hasText(subjectName)) {
            return new ArrayList<>();
        }
        return conflictCheckRecordRepository.findBySubjectNameOrderByCreatedAtDesc(subjectName);
    }

    private ClientDTO checkConflictPreview(ClientDTO dto, Long excludedClientId) {
        return checkConflictPreview(dto, excludedClientId, null);
    }

    private ClientDTO checkConflictPreview(ClientDTO dto, Long excludedClientId, Long checkedBy) {
        ClientDTO result = new ClientDTO();
        result.setClientName(dto.getClientName());
        result.setClientType(dto.getClientType());
        result.setClientRelationship(dto.getClientRelationship());
        result.setClientRole(dto.getClientRole());
        result.setIdCard(dto.getIdCard());
        result.setHasConflict(false);
        result.setConflictLevel("NONE");
        result.setConflictCaseIds(new ArrayList<>());
        result.setSimilarClientNames(new ArrayList<>());

        if (!StringUtils.hasText(dto.getClientName())) {
            return result;
        }

        List<Client> clients = clientRepository.findByDeletedFalse().stream()
                .filter(c -> excludedClientId == null || !excludedClientId.equals(c.getId()))
                .collect(Collectors.toList());

        List<Client> exactMatches = clients.stream()
                .filter(c -> dto.getClientName().equals(c.getClientName()))
                .collect(Collectors.toList());

        for (Client existing : exactMatches) {
            if (isOpposingRole(dto.getClientRole(), existing.getClientRole())) {
                result.setHasConflict(true);
                result.setConflictLevel("DIRECT");
                result.setConflictDescription("存在直接利益冲突：客户库中已存在相同客户名称，且既有客户角色为“"
                        + nullToBlank(existing.getClientRole()) + "”，本次拟录入角色为“"
                        + nullToBlank(dto.getClientRole()) + "”。请停止建档并由行政管理进行利冲审查。");
                result.getConflictCaseIds().addAll(findCaseIdsByPartyName(dto.getClientName()));
                return result;
            }
        }

        if (!exactMatches.isEmpty()) {
            Client existing = exactMatches.get(0);
            result.setConflictLevel("EXISTING");
            result.setConflictDescription("此客户已存在，客户所属人为“"
                    + resolveUserNames(existing.getClientOwnerIds(), existing.getOwnerId()) + "”，所属部门为“"
                    + resolveDepartmentName(existing.getDepartmentId()) + "”。未发现直接利益冲突。");
        }

        String normalized = normalizeClientName(dto.getClientName());
        List<String> similarNames = clients.stream()
                .filter(c -> !dto.getClientName().equals(c.getClientName()))
                .filter(c -> isSimilarName(normalized, normalizeClientName(c.getClientName())))
                .map(Client::getClientName)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        if (!similarNames.isEmpty() && !"EXISTING".equals(result.getConflictLevel())) {
            result.setConflictLevel("SIMILAR");
            result.setSimilarClientNames(similarNames);
            result.setConflictDescription("发现高度相似客户：" + String.join("、", similarNames)
                    + "。请再次核实客户名称是否正确；核实后可继续新增。");
        }

        return result;
    }

    private void saveConflictCheckRecord(ClientDTO request, ClientDTO result, Long checkedBy) {
        if (!StringUtils.hasText(request.getClientName())) {
            return;
        }
        ConflictCheckRecord record = new ConflictCheckRecord();
        record.setSubjectName(request.getClientName());
        record.setClientType(request.getClientType());
        record.setClientRelationship(request.getClientRelationship());
        record.setClientRole(request.getClientRole());
        record.setIdCard(request.getIdCard());
        record.setCreditCode(request.getCreditCode());
        record.setCheckedBy(checkedBy);
        record.setConflictLevel(result.getConflictLevel());
        record.setConclusion(result.getConflictDescription());
        record.setSimilarNames(result.getSimilarClientNames() == null ? null : String.join(",", result.getSimilarClientNames()));
        record.setMatchedCaseIds(result.getConflictCaseIds() == null ? null : result.getConflictCaseIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        record.setMatchedClientIds(clientRepository.findAllByClientNameAndDeletedFalse(request.getClientName()).stream()
                .map(Client::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        conflictCheckRecordRepository.save(record);
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
        Set<String> plaintiffSide = new HashSet<>(Arrays.asList("原告", "共同原告", "申请人", "上诉人", "债权人"));
        Set<String> defendantSide = new HashSet<>(Arrays.asList("被告", "共同被告", "被申请人", "被上诉人"));
        return (plaintiffSide.contains(newRole) && defendantSide.contains(existingRole))
                || (defendantSide.contains(newRole) && plaintiffSide.contains(existingRole));
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

        // 提取案件ID集合
        List<Long> caseIds = parties.stream()
                .map(com.lawfirm.entity.Party::getCaseId)
                .distinct()
                .collect(Collectors.toList());

        // 查询对应的案件
        return caseIds.stream()
                .map(caseId -> caseRepository.findById(caseId))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(c -> !c.getDeleted())
                .map(this::convertToCaseListVO)
                .collect(Collectors.toList());
    }

    public List<com.lawfirm.vo.CaseListVO> getClientCases(Long clientId, Long currentUserId) {
        assertClientVisible(clientId, currentUserId);
        return getClientCases(clientId);
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
        return allRecords.subList(start, end);
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
        return communicationRecordRepository.save(record);
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
        return communicationRecordRepository.save(record);
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
        vo.setCourt(caseEntity.getCourt());
        vo.setCreatedAt(caseEntity.getCreatedAt() != null ?
            caseEntity.getCreatedAt().toLocalDate() : null);
        return vo;
    }
}
