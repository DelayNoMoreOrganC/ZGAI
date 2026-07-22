package com.lawfirm.controller;

import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.dto.AIConfigVO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.service.AIConfigService;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI配置控制器
 */
@RestController
@RequestMapping("ai/config")
@RequiredArgsConstructor
public class AIConfigController {

    private final AIConfigService aiConfigService;

    /**
     * 创建AI配置
     */
    @PostMapping
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<AIConfigVO> createConfig(@Valid @RequestBody AIConfigDTO dto) {
        AIConfig config = aiConfigService.createConfig(dto);
        return Result.success(AIConfigVO.from(config));
    }

    /**
     * 更新AI配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<AIConfigVO> updateConfig(@PathVariable Long id, @Valid @RequestBody AIConfigDTO dto) {
        AIConfig config = aiConfigService.updateConfig(id, dto);
        return Result.success(AIConfigVO.from(config));
    }

    /**
     * 删除AI配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        aiConfigService.deleteConfig(id);
        return Result.success();
    }

    /**
     * 获取AI配置详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<AIConfigVO> getConfig(@PathVariable Long id) {
        AIConfig config = aiConfigService.getConfig(id);
        return Result.success(AIConfigVO.from(config));
    }

    /**
     * 获取所有AI配置
     */
    @GetMapping
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<List<AIConfigVO>> getAllConfigs() {
        List<AIConfig> configs = aiConfigService.getAllConfigs();
        return Result.success(configs.stream().map(AIConfigVO::from).collect(Collectors.toList()));
    }

    /**
     * 获取默认配置
     */
    @GetMapping("/default")
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<AIConfigVO> getDefaultConfig() {
        AIConfig config = aiConfigService.getDefaultConfig();
        return Result.success(AIConfigVO.from(config));
    }

    /**
     * 按提供商类型查找配置
     */
    @GetMapping("/provider/{providerType}")
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<List<AIConfigVO>> getConfigsByProvider(@PathVariable String providerType) {
        List<AIConfig> configs = aiConfigService.getConfigsByProvider(providerType);
        return Result.success(configs.stream().map(AIConfigVO::from).collect(Collectors.toList()));
    }

    /**
     * 测试AI配置连接
     * POST /api/ai/config/test/{id}
     */
    @PostMapping("/test/{id}")
    @PreAuthorize("hasAuthority('AI_CONFIG')")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        Map<String, Object> result = aiConfigService.testConnection(id);
        return Result.success(result);
    }
}
