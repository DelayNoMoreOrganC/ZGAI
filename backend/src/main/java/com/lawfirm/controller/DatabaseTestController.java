package com.lawfirm.controller;

import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 临时测试控制器 - 查看H2数据库中的表
 */
@RestController
@RequestMapping("/test-db")
@RequiredArgsConstructor
public class DatabaseTestController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查看knowledge_article表结构 - 简化版
     */
    @GetMapping("/knowledge-article-structure")
    public Result<List<String>> getTableStructure() {
        try {
            // 使用DESCRIBE命令查看表结构
            String sql = "SELECT * FROM information_schema.columns " +
                        "WHERE table_schema = 'PUBLIC' AND UPPER(table_name) LIKE '%KNOWLEDGE%' " +
                        "ORDER BY table_name, ordinal_position";
            List<String> result = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getString("table_name") + "." + rs.getString("column_name")
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("Error: " + e.getMessage());
        }
    }

    /**
     * 查看所有表
     */
    @GetMapping("/tables")
    public Result<List<Map<String, Object>>> listTables() {
        String sql = "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'PUBLIC' ORDER BY table_name";
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(sql);
        return Result.success(tables);
    }
}
