package com.lawfirm.controller;

import com.lawfirm.service.SearchService;
import com.lawfirm.util.Result;
import com.lawfirm.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 全局搜索控制器
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 全局搜索
     * GET /api/search?q=keyword&type=all
     *
     * @param keyword 搜索关键词
     * @param type 搜索类型：all/case/client/document
     * @return 搜索结果列表
     */
    @GetMapping
    public Result<List<SearchResultVO>> globalSearch(
            @RequestParam("q") String keyword,
            @RequestParam(value = "type", defaultValue = "all") String type) {

        try {
            log.info("全局搜索: keyword={}, type={}", keyword, type);

            // 参数校验
            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.error("搜索关键词不能为空");
            }

            if (keyword.length() < 2) {
                return Result.error("搜索关键词至少包含2个字符");
            }

            // 执行搜索
            List<SearchResultVO> results = searchService.globalSearch(keyword, type);

            log.info("搜索完成: keyword={}, type={}, 结果数={}", keyword, type, results.size());

            return Result.success(results);

        } catch (Exception e) {
            log.error("全局搜索失败: keyword={}, type={}, error={}", keyword, type, e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }
}
