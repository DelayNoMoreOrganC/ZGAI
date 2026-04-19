package com.lawfirm.util;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果类
 *
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Long page;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long totalPages;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    public PageResult() {
    }

    public PageResult(Long page, Long size, Long total, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
        this.totalPages = (total + size - 1) / size;
        this.hasPrevious = page > 1;
        this.hasNext = page < totalPages;
    }

    /**
     * 构建分页结果
     */
    public static <T> PageResult<T> of(Long page, Long size, Long total, List<T> records) {
        return new PageResult<>(page, size, total, records);
    }

    /**
     * 空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(1L, 10L, 0L, List.of());
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }

    /**
     * 判断是否有数据
     */
    public boolean hasData() {
        return !isEmpty();
    }
}
