-- 工作汇报表
CREATE TABLE IF NOT EXISTS `work_report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `title` VARCHAR(200) NOT NULL COMMENT '汇报标题',
    `report_date` DATETIME NOT NULL COMMENT '汇报日期',
    `report_type` VARCHAR(20) NOT NULL COMMENT '汇报类型',
    `content` TEXT COMMENT '汇报内容',
    `work_summary` VARCHAR(1000) COMMENT '工作总结',
    `next_plan` VARCHAR(1000) COMMENT '下周计划',
    `problems` VARCHAR(1000) COMMENT '遇到问题',
    `suggestions` VARCHAR(1000) COMMENT '建议意见',
    `reporter_id` BIGINT NOT NULL COMMENT '汇报人ID',
    `reporter_name` VARCHAR(50) COMMENT '汇报人姓名',
    `department` VARCHAR(100) COMMENT '部门',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `reviewer_name` VARCHAR(50) COMMENT '审核人姓名',
    `review_comment` VARCHAR(500) COMMENT '审核意见',
    `reviewed_at` DATETIME COMMENT '审核时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_reporter_id` (`reporter_id`),
    INDEX `idx_report_date` (`report_date`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作汇报表';

-- 插入测试数据
INSERT INTO work_report (title, report_date, report_type, work_summary, next_plan, problems, suggestions, reporter_id, reporter_name, department, status, created_at, updated_at, deleted) VALUES
('周报 - 2026年第16周', '2026-04-19 10:00:00', 'WEEKLY', '本周完成案件管理模块开发，解决Hibernate表名映射问题', '下周继续开发AI OCR功能', 'H2数据库兼容性问题较多', '建议切换到MySQL生产环境', 2, '张三律师', '技术部', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('周报 - 2026年第16周', '2026-04-19 11:00:00', 'WEEKLY', '完成3个案件的法律文书起草', '准备下周开庭材料', '客户联系不及时', '加强客户沟通管理', 3, '李四律师', '诉讼部', 'SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
