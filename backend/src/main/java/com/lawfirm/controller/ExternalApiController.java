package com.lawfirm.controller;

import com.lawfirm.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 外部功能集成接口
 *
 * - 省时宝功能：已集成 SSB 法律文档自动生成引擎（端口 5002）
 * - AC精算功能：已集成 Python 债权精算引擎（端口 5100）
 */
@Slf4j
@RestController
@RequestMapping("/external")
public class ExternalApiController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ac-calc.url:http://localhost:5100}")
    private String acCalcUrl;

    @Value("${ssb.url:http://localhost:5002}")
    private String ssbUrl;

    @PostConstruct
    public void init() {
        log.info("外部功能接口初始化完成，AC精算地址: {}, 省时宝地址: {}", acCalcUrl, ssbUrl);
    }

    // ============================================================
    // 省时宝 (SSB) — 法律文档自动生成
    // ============================================================

    /**
     * 省时宝 - 获取模板列表
     * GET /api/external/shengshibao/templates
     */
    @GetMapping("/shengshibao/templates")
    public Result<?> listTemplates() {
        log.info("省时宝: 获取模板列表");
        return proxyGet("/api/templates");
    }

    /**
     * 省时宝 - 获取模板文件列表
     * GET /api/external/shengshibao/templates/{projectPath}/files
     */
    @GetMapping("/shengshibao/templates/{projectPath}/files")
    public Result<?> getTemplateFiles(@PathVariable String projectPath) {
        log.info("省时宝: 获取模板文件列表: {}", projectPath);
        return proxyGet("/api/templates/" + projectPath + "/files");
    }

    /**
     * 省时宝 - 获取模板字段定义
     * GET /api/external/shengshibao/templates/{projectPath}/fields
     */
    @GetMapping("/shengshibao/templates/{projectPath}/fields")
    public Result<?> getTemplateFields(@PathVariable String projectPath) {
        log.info("省时宝: 获取模板字段: {}", projectPath);
        return proxyGet("/api/templates/" + projectPath + "/fields");
    }

    /**
     * 省时宝 - 生成文档
     * POST /api/external/shengshibao/generate
     *
     * 请求体:
     * {
     *   "project_path": "建设银行-个人快贷",
     *   "extracted_data": {
     *     "被告姓名": "张三",
     *     "身份证号": "440...",
     *     ...
     *   }
     * }
     */
    @PostMapping("/shengshibao/generate")
    public Result<?> generateDocument(@RequestBody Map<String, Object> request) {
        log.info("省时宝: 生成文档, 项目={}, 字段数={}",
            request.get("project_path"),
            request.get("extracted_data") instanceof Map ? ((Map<?, ?>) request.get("extracted_data")).size() : 0);
        return proxyPost("/api/generate", request);
    }

    /**
     * 省时宝 - AI 提取 PDF
     * POST /api/external/shengshibao/extract-pdf
     *
     * 文件上传通过 multipart/form-data 透传
     */
    @PostMapping("/shengshibao/extract-pdf")
    public Result<?> extractPdf(@RequestBody Map<String, Object> request) {
        log.info("省时宝: PDF 提取");
        return proxyPost("/api/extract-pdf", request);
    }

    /**
     * 省时宝 - 健康检查
     * GET /api/external/shengshibao/health
     */
    @GetMapping("/shengshibao/health")
    public Result<?> ssbHealth() {
        return proxyGet("/api/health");
    }

    // ============================================================
    // AC精算 — 债权精算引擎
    // ============================================================

    /**
     * AC精算 - 单笔计算
     * POST /api/external/ac-calc
     *
     * 请求体示例:
     * {
     *   "principal": 1000000.00,
     *   "annual_rate": 0.05,
     *   "start_date": "2020-01-01",
     *   "end_date": "2024-06-30",
     *   "repayment_records": [
     *     {"date": "2023-06-21", "amount": 200000.00, "type": "normal"}
     *   ]
     * }
     */
    @PostMapping("/ac-calc")
    public Result<?> acCalc(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        log.info("AC精算请求: principal={}, start={}, end={}",
            request.get("principal"), request.get("start_date"), request.get("end_date"));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                acCalcUrl + "/api/calc/compute",
                HttpMethod.POST,
                entity,
                Map.class
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("AC精算完成, 耗时: {}ms", duration);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && Integer.valueOf(200).equals(responseBody.get("code"))) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                data.put("processingTimeMs", duration);
                data.put("engine", "AC-CALC Python v2.0");
                return Result.success("计算成功", data);
            }

            return Result.error("精算引擎返回错误: " +
                (responseBody != null ? responseBody.get("message") : "未知错误"));

        } catch (Exception e) {
            log.error("AC精算调用失败", e);
            return Result.error("精算服务暂不可用: " + e.getMessage() +
                "。请确保 AC精算引擎已启动: cd ac-calc && python3 api_service.py");
        }
    }

    /**
     * AC精算 - 批量计算
     * POST /api/external/ac-calc/batch
     */
    @PostMapping("/ac-calc/batch")
    public Result<?> batchCalc(@RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                acCalcUrl + "/api/calc/batch",
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null && Integer.valueOf(200).equals(body.get("code"))) {
                return Result.success(body.get("data"));
            }
            return Result.error(body != null ? (String) body.get("message") : "未知错误");
        } catch (Exception e) {
            log.error("AC批量精算失败", e);
            return Result.error("批量精算失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 通用健康检查
    // ============================================================

    /**
     * 接口健康检查
     * GET /api/external/health
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        boolean acCalcOnline = false;
        boolean ssbOnline = false;
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                acCalcUrl + "/api/calc/health", Map.class);
            acCalcOnline = resp.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("AC精算服务健康检查失败: {}", e.getMessage());
        }
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                ssbUrl + "/api/health", Map.class);
            ssbOnline = resp.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("省时宝服务健康检查失败: {}", e.getMessage());
        }

        return Result.success(Map.of(
            "ssb", ssbOnline ? "online" : "offline",
            "ssbUrl", ssbUrl,
            "ssbTemplates", ssbOnline ? "available" : "unknown",
            "acCalc", acCalcOnline ? "online" : "offline",
            "acCalcUrl", acCalcUrl,
            "status", (acCalcOnline && ssbOnline) ? "all_ready"
                : acCalcOnline ? "ssb_offline"
                : ssbOnline ? "ac_calc_offline"
                : "all_offline"
        ));
    }

    // ============================================================
    // 代理方法
    // ============================================================

    private Result<?> proxyGet(String path) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(ssbUrl + path, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                return Result.success(body);
            }
            return Result.error(body != null ? (String) body.getOrDefault("error", "未知错误") : "服务无响应");
        } catch (Exception e) {
            log.error("省时宝代理请求失败 [{}]: {}", path, e.getMessage());
            return Result.error("省时宝服务暂不可用: " + e.getMessage() +
                "。请确保服务已启动: cd ssb && python3 ssb_api.py");
        }
    }

    private Result<?> proxyPost(String path, Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                ssbUrl + path, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                return Result.success(body);
            }
            return Result.error(body != null ? (String) body.getOrDefault("error", "未知错误") : "服务无响应");
        } catch (Exception e) {
            log.error("省时宝代理请求失败 [{}]: {}", path, e.getMessage());
            return Result.error("省时宝服务暂不可用: " + e.getMessage());
        }
    }
}
