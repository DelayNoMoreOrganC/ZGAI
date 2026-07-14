package com.lawfirm.controller;

import com.lawfirm.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 外部服务集成控制器
 * 为前端提供 SSB（省时宝）+ AC 精算的统一代理入口
 * 将请求转发到对应的 Flask 微服务
 */
@Slf4j
@RestController
@RequestMapping("/external")
public class ExternalApiController {

    @Value("${ac-calc.url:http://localhost:5100}")
    private String acCalcUrl;

    @Value("${ssb.url:http://localhost:5002}")
    private String ssbUrl;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
        log.info("ExternalApiController initialized: SSB={}, AC={}", ssbUrl, acCalcUrl);
    }

    /**
     * 获取外部服务聚合健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ssb", checkService(ssbUrl + "/api/health") ? "online" : "offline");
        status.put("acCalc", checkService(acCalcUrl + "/api/calc/health") ? "online" : "offline");

        boolean allOk = "online".equals(status.get("ssb")) && "online".equals(status.get("acCalc"));
        status.put("status", allOk ? "all_ready" : "partial");
        return Result.success(status);
    }

    // ================================================================
    //  省时宝 (SSB) — 法律文档自动生成
    // ================================================================

    /**
     * SSB 健康检查
     */
    @GetMapping("/shengshibao/health")
    public Result<?> ssbHealth() {
        try {
            Map response = restTemplate.getForObject(ssbUrl + "/api/health", Map.class);
            return Result.success(response);
        } catch (Exception e) {
            log.warn("SSB health check failed: {}", e.getMessage());
            return Result.error("省时宝服务不可用: " + e.getMessage());
        }
    }

    /**
     * 获取 SSB 模板项目列表
     */
    @GetMapping("/shengshibao/templates")
    public Result<?> ssbTemplates() {
        try {
            Map response = restTemplate.getForObject(ssbUrl + "/api/templates", Map.class);
            return Result.success(response);
        } catch (Exception e) {
            log.warn("SSB templates fetch failed: {}", e.getMessage());
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取 SSB 模板文件列表
     */
    @GetMapping("/shengshibao/templates/{projectPath}/files")
    public Result<?> ssbTemplateFiles(@PathVariable String projectPath) {
        try {
            String url = ssbUrl + "/api/templates/" + projectPath + "/files";
            Map response = restTemplate.getForObject(url, Map.class);
            return Result.success(response);
        } catch (Exception e) {
            log.warn("SSB template files fetch failed: {}", e.getMessage());
            return Result.error("获取模板文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取 SSB 模板字段定义
     */
    @GetMapping("/shengshibao/templates/{projectPath}/fields")
    public Result<?> ssbTemplateFields(@PathVariable String projectPath) {
        try {
            String url = ssbUrl + "/api/templates/" + projectPath + "/fields";
            Map response = restTemplate.getForObject(url, Map.class);
            return Result.success(response);
        } catch (Exception e) {
            log.warn("SSB template fields fetch failed: {}", e.getMessage());
            return Result.error("获取模板字段失败: " + e.getMessage());
        }
    }

    /**
     * SSB 文档生成
     */
    @PostMapping("/shengshibao/generate")
    public Result<?> ssbGenerate(@RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    ssbUrl + "/api/generate",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            return Result.success(response.getBody());
        } catch (Exception e) {
            log.warn("SSB document generate failed: {}", e.getMessage());
            return Result.error("文档生成失败: " + e.getMessage());
        }
    }

    /**
     * SSB PDF 提取
     */
    @PostMapping("/shengshibao/extract-pdf")
    public Result<?> ssbExtractPdf(@RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    ssbUrl + "/api/extract-pdf",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            return Result.success(response.getBody());
        } catch (Exception e) {
            log.warn("SSB PDF extract failed: {}", e.getMessage());
            return Result.error("PDF提取失败: " + e.getMessage());
        }
    }

    // ================================================================
    //  AC 精算 — 债权精算引擎
    // ================================================================

    /**
     * 单笔债权精算
     */
    @PostMapping("/ac-calc")
    public Result<?> acCalculate(@RequestBody Map<String, Object> data) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    acCalcUrl + "/api/calc/compute",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            return Result.success(response.getBody());
        } catch (Exception e) {
            log.warn("AC calc failed: {}", e.getMessage());
            return Result.error("债权精算失败: " + e.getMessage());
        }
    }

    /**
     * 批量债权精算
     */
    @PostMapping("/ac-calc/batch")
    public Result<?> acCalculateBatch(@RequestBody List<Map<String, Object>> dataList) {
        try {
            List<Object> results = new ArrayList<>();
            for (Map<String, Object> data : dataList) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);

                    ResponseEntity<Map> response = restTemplate.exchange(
                            acCalcUrl + "/api/calc/compute",
                            HttpMethod.POST,
                            entity,
                            Map.class
                    );
                    results.add(response.getBody());
                } catch (Exception e) {
                    results.add(Map.of("error", e.getMessage()));
                }
            }
            return Result.success(results);
        } catch (Exception e) {
            log.warn("AC batch calc failed: {}", e.getMessage());
            return Result.error("批量精算失败: " + e.getMessage());
        }
    }

    // ================================================================
    //  内部方法
    // ================================================================

    /**
     * 检查服务是否在线
     */
    private boolean checkService(String healthUrl) {
        try {
            restTemplate.getForObject(healthUrl, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
