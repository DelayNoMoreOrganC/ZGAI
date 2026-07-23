package com.lawfirm.service;

import com.lawfirm.dto.ClientSubjectRelationDTO;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.ClientSubjectRelation;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.ClientSubjectRelationRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientSubjectRelationService {

    private static final Set<String> RELATION_TYPES = new HashSet<>(Arrays.asList(
            "PARENT_COMPANY", "SUBSIDIARY", "AFFILIATE", "ACTUAL_CONTROLLER",
            "LEGAL_REPRESENTATIVE", "FORMER_NAME", "GUARANTOR", "OTHER"));

    private final ClientSubjectRelationRepository relationRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public List<ClientSubjectRelationDTO> list(Long clientId) {
        Map<Long, ClientSubjectRelationDTO> result = new LinkedHashMap<>();
        relationRepository.findBySourceClientIdAndDeletedFalseOrderByCreatedAtDesc(clientId)
                .forEach(relation -> result.put(relation.getId(), toDTO(relation, clientId)));
        relationRepository.findByTargetClientIdAndDeletedFalseOrderByCreatedAtDesc(clientId)
                .forEach(relation -> result.putIfAbsent(relation.getId(), toDTO(relation, clientId)));
        return new ArrayList<>(result.values());
    }

    @Transactional
    public ClientSubjectRelationDTO create(Long sourceClientId, ClientSubjectRelationDTO request, Long userId) {
        Client source = requireClient(sourceClientId);
        if (request == null || !StringUtils.hasText(request.getRelationType())) {
            throw new IllegalArgumentException("请选择关联关系类型");
        }
        String relationType = request.getRelationType().trim().toUpperCase();
        if (!RELATION_TYPES.contains(relationType)) {
            throw new IllegalArgumentException("关联关系类型无效");
        }

        Client target = null;
        if (request.getTargetClientId() != null) {
            if (sourceClientId.equals(request.getTargetClientId())) {
                throw new IllegalArgumentException("客户不能与自身建立关联关系");
            }
            target = requireClient(request.getTargetClientId());
        }
        String targetName = target == null ? request.getTargetSubjectName() : target.getClientName();
        String targetCreditCode = target == null ? request.getTargetCreditCode() : target.getCreditCode();
        if (!StringUtils.hasText(targetName)) {
            throw new IllegalArgumentException("请填写或选择关联主体");
        }
        if (normalizeName(source.getClientName()).equals(normalizeName(targetName))) {
            throw new IllegalArgumentException("客户不能与同名主体建立关联关系");
        }

        String normalizedTarget = normalizeName(targetName);
        boolean duplicate = relationRepository
                .findBySourceClientIdAndDeletedFalseOrderByCreatedAtDesc(sourceClientId).stream()
                .anyMatch(existing -> relationType.equals(existing.getRelationType())
                        && (request.getTargetClientId() != null
                        ? request.getTargetClientId().equals(existing.getTargetClientId())
                        : normalizedTarget.equals(normalizeName(existing.getTargetSubjectName()))));
        if (duplicate) {
            throw new IllegalArgumentException("该关联主体关系已存在");
        }

        ClientSubjectRelation relation = new ClientSubjectRelation();
        relation.setSourceClientId(source.getId());
        relation.setTargetClientId(target == null ? null : target.getId());
        relation.setTargetSubjectName(targetName.trim());
        relation.setTargetCreditCode(StringUtils.hasText(targetCreditCode) ? targetCreditCode.trim() : null);
        relation.setRelationType(relationType);
        relation.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        relation.setCreatedBy(userId);
        return toDTO(relationRepository.save(relation), sourceClientId);
    }

    @Transactional
    public void delete(Long sourceClientId, Long relationId) {
        ClientSubjectRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("关联主体关系不存在"));
        if (!sourceClientId.equals(relation.getSourceClientId())) {
            throw new IllegalArgumentException("只能由关系来源客户一侧删除该记录");
        }
        relation.setDeleted(true);
        relationRepository.save(relation);
    }

    public List<ClientSubjectRelation> findAllActive() {
        return relationRepository.findByDeletedFalse();
    }

    public String relationTypeName(String type) {
        if ("PARENT_COMPANY".equals(type)) return "母公司";
        if ("SUBSIDIARY".equals(type)) return "子公司";
        if ("AFFILIATE".equals(type)) return "关联企业";
        if ("ACTUAL_CONTROLLER".equals(type)) return "实际控制人";
        if ("LEGAL_REPRESENTATIVE".equals(type)) return "法定代表人";
        if ("FORMER_NAME".equals(type)) return "曾用名";
        if ("GUARANTOR".equals(type)) return "担保关系";
        return "其他关联";
    }

    public String inverseRelationTypeName(String type) {
        if ("PARENT_COMPANY".equals(type)) return "子公司";
        if ("SUBSIDIARY".equals(type)) return "母公司";
        if ("ACTUAL_CONTROLLER".equals(type)) return "受控企业";
        if ("LEGAL_REPRESENTATIVE".equals(type)) return "任职企业";
        if ("FORMER_NAME".equals(type)) return "现用名";
        return relationTypeName(type);
    }

    private ClientSubjectRelationDTO toDTO(ClientSubjectRelation relation, Long viewedClientId) {
        ClientSubjectRelationDTO dto = new ClientSubjectRelationDTO();
        dto.setId(relation.getId());
        dto.setSourceClientId(relation.getSourceClientId());
        dto.setSourceClientName(resolveClientName(relation.getSourceClientId()));
        dto.setTargetClientId(relation.getTargetClientId());
        dto.setTargetSubjectName(relation.getTargetSubjectName());
        boolean outbound = viewedClientId.equals(relation.getSourceClientId());
        dto.setTargetCreditCode(outbound
                ? relation.getTargetCreditCode()
                : clientRepository.findById(relation.getSourceClientId()).map(Client::getCreditCode).orElse(null));
        dto.setRelationType(relation.getRelationType());
        dto.setRelationTypeName(outbound
                ? relationTypeName(relation.getRelationType())
                : inverseRelationTypeName(relation.getRelationType()));
        dto.setDescription(relation.getDescription());
        dto.setDirection(outbound ? "OUTBOUND" : "INBOUND");
        dto.setRelatedSubjectName(outbound ? relation.getTargetSubjectName() : resolveClientName(relation.getSourceClientId()));
        dto.setCreatedByName(userRepository.findById(relation.getCreatedBy())
                .map(user -> StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername())
                .orElse("未知人员"));
        dto.setCreatedAt(relation.getCreatedAt());
        return dto;
    }

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .filter(client -> !Boolean.TRUE.equals(client.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    private String resolveClientName(Long clientId) {
        return clientRepository.findById(clientId).map(Client::getClientName).orElse("未知客户");
    }

    private String normalizeName(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.replaceAll("[\\s（）()·,，。.-]", "")
                .replace("有限责任公司", "")
                .replace("股份有限公司", "")
                .replace("有限公司", "")
                .replace("公司", "")
                .toLowerCase();
    }
}
