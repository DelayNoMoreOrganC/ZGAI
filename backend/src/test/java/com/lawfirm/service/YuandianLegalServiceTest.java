package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.KnowledgeArticleDTO;
import com.lawfirm.dto.KnowledgeArticleVO;
import com.lawfirm.dto.YuandianSearchRequest;
import com.lawfirm.dto.YuandianSearchResponse;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIProviderType;
import com.lawfirm.repository.AIConfigRepository;
import com.lawfirm.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YuandianLegalServiceTest {

    private AIConfigRepository configRepository;
    private KnowledgeArticleService knowledgeArticleService;
    private SecurityUtils securityUtils;
    private YuandianLegalService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        configRepository = mock(AIConfigRepository.class);
        knowledgeArticleService = mock(KnowledgeArticleService.class);
        securityUtils = mock(SecurityUtils.class);
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new YuandianLegalService(
                configRepository,
                knowledgeArticleService,
                securityUtils,
                new ObjectMapper(),
                restTemplate);
        ReflectionTestUtils.setField(service, "configuredBaseUrl", "https://open.chineselaw.com");
        ReflectionTestUtils.setField(service, "environmentApiKey", "");
        ReflectionTestUtils.setField(service, "maxResults", 10);
        ReflectionTestUtils.setField(service, "cacheMinutes", 30);
        when(configRepository.findByProviderTypeAndDeletedFalse(AIProviderType.YUANDIAN_LEGAL.name()))
                .thenReturn(List.of(enabledConfig()));
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
    }

    @Test
    void lawSearchUsesOfficialContractAndProducesImportToken() {
        server.expect(requestTo("https://open.chineselaw.com/open/law_vector_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-key"))
                .andExpect(jsonPath("$.fatiao_filter.sxx[0]").value("现行有效"))
                .andExpect(jsonPath("$.return_num").value(3))
                .andRespond(withSuccess("{\"code\":201,\"extra\":{\"fatiao\":[{"
                        + "\"ftid\":\"law-1\",\"fgid\":\"parent-1\","
                        + "\"fgtitle\":\"中华人民共和国民法典\",\"num\":\"第一条\","
                        + "\"content\":\"为了保护民事主体的合法权益。\",\"sxx\":\"现行有效\","
                        + "\"effect1\":\"法律\",\"location\":\"全国\",\"score\":0.96}]}}",
                        MediaType.APPLICATION_JSON));

        YuandianSearchRequest request = new YuandianSearchRequest();
        request.setQuery("民事主体权益保护");
        request.setLimit(3);
        YuandianSearchResponse response = service.searchLaws(request);

        assertEquals(1, response.getResults().size());
        assertEquals("LAW", response.getResults().get(0).getResultType());
        assertEquals(KnowledgeArticlePolicy.VALIDITY_EFFECTIVE,
                response.getResults().get(0).getValidityStatus());
        assertNotNull(response.getResults().get(0).getImportToken());
        assertTrue(response.getResults().get(0).getRagEligible());
        server.verify();
    }

    @Test
    void importedCaseRemainsReferenceOnlyAndOutsideSharedRag() {
        server.expect(requestTo("https://open.chineselaw.com/open/case_vector_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.wenshu_filter.dianxing").value(true))
                .andRespond(withSuccess("{\"code\":201,\"extra\":{\"wenshu\":[{"
                        + "\"scid\":\"case-1\",\"title\":\"测试案例\",\"ah\":\"(2026)粤01民终1号\","
                        + "\"jbdw\":\"测试法院\",\"ajlb\":\"民事案件\",\"ay\":[\"合同纠纷\"],"
                        + "\"content\":\"裁判要旨\",\"jaDate\":20260720,\"score\":0.88}]}}",
                        MediaType.APPLICATION_JSON));
        KnowledgeArticleVO created = new KnowledgeArticleVO();
        created.setId(99L);
        when(knowledgeArticleService.createArticle(any())).thenReturn(created);

        YuandianSearchRequest request = new YuandianSearchRequest();
        request.setQuery("合同纠纷");
        request.setLimit(1);
        String token = service.searchCases(request).getResults().get(0).getImportToken();
        KnowledgeArticleVO result = service.importToKnowledge(token);

        ArgumentCaptor<KnowledgeArticleDTO> captor = ArgumentCaptor.forClass(KnowledgeArticleDTO.class);
        verify(knowledgeArticleService).createArticle(captor.capture());
        assertEquals(99L, result.getId());
        assertEquals(KnowledgeArticlePolicy.REFERENCE_MATERIAL, captor.getValue().getKnowledgeSource());
        assertFalse(captor.getValue().getAuthorizationConfirmed());
        assertFalse(captor.getValue().getKnowledgeEligible());
        server.verify();
    }

    private AIConfig enabledConfig() {
        AIConfig config = new AIConfig();
        config.setProviderType(AIProviderType.YUANDIAN_LEGAL.name());
        config.setApiKey("test-key");
        config.setIsEnabled(true);
        config.setDeleted(false);
        return config;
    }
}
