package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.entity.Client;
import com.lawfirm.entity.Party;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.ClientRepository;
import com.lawfirm.repository.PartyRepository;
import com.lawfirm.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;
    private final PartyRepository partyRepository;
    private final CaseDocumentRepository caseDocumentRepository;

    /**
     * 全局搜索
     *
     * @param keyword 搜索关键词
     * @param type 搜索类型：all/case/client/document
     * @return 搜索结果列表
     */
    public List<SearchResultVO> globalSearch(String keyword, String type) {
        List<SearchResultVO> results = new ArrayList<>();

        try {
            String keywordLower = keyword.toLowerCase();

            // 根据类型决定搜索范围
            boolean searchCase = "all".equals(type) || "case".equals(type);
            boolean searchClient = "all".equals(type) || "client".equals(type);
            boolean searchDocument = "all".equals(type) || "document".equals(type);

            // 1. 搜索案件
            if (searchCase) {
                results.addAll(searchCases(keyword, keywordLower));
            }

            // 2. 搜索客户
            if (searchClient) {
                results.addAll(searchClients(keyword, keywordLower));
            }

            // 3. 搜索当事人（关联到案件）
            if (searchCase) {
                results.addAll(searchParties(keyword, keywordLower));
            }

            // 4. 搜索文档
            if (searchDocument) {
                results.addAll(searchDocuments(keyword, keywordLower));
            }

        } catch (Exception e) {
            log.error("全局搜索失败: keyword={}, type={}, error={}", keyword, type, e.getMessage(), e);
        }

        return results;
    }

    /**
     * 搜索案件
     */
    private List<SearchResultVO> searchCases(String keyword, String keywordLower) {
        List<SearchResultVO> results = new ArrayList<>();

        try {
            // 使用数据库查询优化，只查询未删除的案件
            List<Case> allCases = caseRepository.findByDeletedFalse();

            for (Case caseEntity : allCases) {
                boolean match = false;
                String matchField = "";

                // 搜索案号
                if (caseEntity.getCaseNumber() != null &&
                    caseEntity.getCaseNumber().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "案号";
                }
                // 搜索案件名称
                else if (caseEntity.getCaseName() != null &&
                         caseEntity.getCaseName().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "案件名称";
                }
                // 搜索案由
                else if (caseEntity.getCaseReason() != null &&
                         caseEntity.getCaseReason().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "案由";
                }
                // 搜索法院
                else if (caseEntity.getCourt() != null &&
                         caseEntity.getCourt().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "管辖法院";
                }

                if (match) {
                    SearchResultVO result = new SearchResultVO();
                    result.setType("CASE");
                    result.setTypeDesc("案件");
                    result.setTitle(caseEntity.getCaseName());
                    result.setSubtitle("案号：" + caseEntity.getCaseNumber());
                    result.setMatchField(matchField);
                    result.setUrl("/case/" + caseEntity.getId());
                    result.setCaseId(caseEntity.getId());
                    results.add(result);
                }
            }

        } catch (Exception e) {
            log.error("搜索案件失败: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 搜索客户
     */
    private List<SearchResultVO> searchClients(String keyword, String keywordLower) {
        List<SearchResultVO> results = new ArrayList<>();

        try {
            // 查询所有未删除的客户
            // 使用数据库查询优化，只查询未删除的客户
            List<Client> allClients = clientRepository.findByDeletedFalse();

            for (Client client : allClients) {
                boolean match = false;
                String matchField = "";

                // 搜索客户名称
                if (client.getClientName() != null &&
                    client.getClientName().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "客户名称";
                }
                // 搜索手机号
                else if (client.getPhone() != null &&
                         client.getPhone().contains(keyword)) {
                    match = true;
                    matchField = "手机号";
                }
                // 搜索身份证号
                else if (client.getIdCard() != null &&
                         client.getIdCard().contains(keyword)) {
                    match = true;
                    matchField = "身份证号";
                }
                // 搜索统一社会信用代码
                else if (client.getCreditCode() != null &&
                         client.getCreditCode().contains(keyword)) {
                    match = true;
                    matchField = "统一社会信用代码";
                }

                if (match) {
                    SearchResultVO result = new SearchResultVO();
                    result.setType("CLIENT");
                    result.setTypeDesc("客户");
                    result.setTitle(client.getClientName());
                    result.setSubtitle("手机：" + (client.getPhone() != null ? client.getPhone() : ""));
                    result.setMatchField(matchField);
                    result.setUrl("/client/" + client.getId());
                    result.setClientId(client.getId());
                    results.add(result);
                }
            }

        } catch (Exception e) {
            log.error("搜索客户失败: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 搜索当事人（关联到案件）
     */
    private List<SearchResultVO> searchParties(String keyword, String keywordLower) {
        List<SearchResultVO> results = new ArrayList<>();

        try {
            // 查询所有未删除的当事人
            // 使用数据库查询优化，只查询未删除的当事人
            List<Party> allParties = partyRepository.findByDeletedFalse();

            for (Party party : allParties) {
                boolean match = false;
                String matchField = "";

                // 搜索当事人姓名
                if (party.getName() != null &&
                    party.getName().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "当事人姓名";
                }
                // 搜索当事人电话
                else if (party.getPhone() != null &&
                         party.getPhone().contains(keyword)) {
                    match = true;
                    matchField = "当事人电话";
                }
                // 搜索身份证号
                else if (party.getIdCard() != null &&
                         party.getIdCard().contains(keyword)) {
                    match = true;
                    matchField = "身份证号";
                }

                if (match) {
                    // 获取关联的案件信息
                    Case caseEntity = caseRepository.findById(party.getCaseId()).orElse(null);
                    if (caseEntity != null && !caseEntity.getDeleted()) {
                        SearchResultVO result = new SearchResultVO();
                        result.setType("CASE");
                        result.setTypeDesc("案件");
                        result.setTitle(caseEntity.getCaseName());
                        result.setSubtitle("案号：" + caseEntity.getCaseNumber() + " | 当事人：" + party.getName());
                        result.setMatchField(matchField);
                        result.setUrl("/case/" + caseEntity.getId());
                        result.setCaseId(caseEntity.getId());
                        results.add(result);
                    }
                }
            }

        } catch (Exception e) {
            log.error("搜索当事人失败: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 搜索文档
     */
    private List<SearchResultVO> searchDocuments(String keyword, String keywordLower) {
        List<SearchResultVO> results = new ArrayList<>();

        try {
            // 使用数据库查询优化，只查询未删除的文档
            List<CaseDocument> allDocuments = caseDocumentRepository.findByDeletedFalse();

            for (CaseDocument document : allDocuments) {
                boolean match = false;
                String matchField = "";

                // 搜索文档名称
                if (document.getDocumentName() != null &&
                    document.getDocumentName().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "文档名称";
                }
                // 搜索标签
                else if (document.getTags() != null &&
                         document.getTags().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "标签";
                }
                // 搜索OCR结果
                else if (document.getOcrResult() != null &&
                         document.getOcrResult().toLowerCase().contains(keywordLower)) {
                    match = true;
                    matchField = "文档内容";
                }

                if (match) {
                    // 获取关联的案件信息
                    Case caseEntity = caseRepository.findById(document.getCaseId()).orElse(null);
                    if (caseEntity != null && !caseEntity.getDeleted()) {
                        SearchResultVO result = new SearchResultVO();
                        result.setType("DOCUMENT");
                        result.setTypeDesc("文档");
                        result.setTitle(document.getDocumentName());
                        result.setSubtitle("案件：" + caseEntity.getCaseName() + " | 类型：" + document.getDocumentType());
                        result.setMatchField(matchField);
                        result.setUrl("/case/" + caseEntity.getId() + "/documents");
                        result.setCaseId(caseEntity.getId());
                        result.setDocumentId(document.getId());
                        results.add(result);
                    }
                }
            }

        } catch (Exception e) {
            log.error("搜索文档失败: {}", e.getMessage());
        }

        return results;
    }
}
