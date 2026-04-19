-- 测试数据初始化脚本
-- 密码统一为: admin123 (BCrypt加密后的值)

-- 插入测试用户
INSERT INTO "user" (username, password, real_name, email, phone, avatar, status, created_at, updated_at) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@lawfirm.com', '13800138000', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lawyer1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三律师', 'zhangsan@lawfirm.com', '13800138001', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lawyer2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四律师', 'lisi@lawfirm.com', '13800138002', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('assistant1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王助理', 'wang@lawfirm.com', '13800138003', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入角色
INSERT INTO role (role_name, role_code, description, created_at, updated_at) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('主办律师', 'PRIMARY_LAWYER', '主办律师，可创建和管理案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('协办律师', 'ASSIST_LAWYER', '协办律师，可协助办理案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('律师助理', 'ASSISTANT', '律师助理，基础权限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 分配用户角色（admin是管理员，lawyer1是主办律师，lawyer2是协办律师，assistant1是助理）
INSERT INTO user_role (user_id, role_id) VALUES
(1, 1),  -- admin -> ADMIN
(2, 2),  -- lawyer1 -> PRIMARY_LAWYER
(3, 3),  -- lawyer2 -> ASSIST_LAWYER
(4, 4);  -- assistant1 -> ASSISTANT

-- 插入测试客户
INSERT INTO party (name, type, phone, email, address, created_at, updated_at) VALUES
('张三', 'PERSON', '13900000001', 'zhangsan@example.com', '北京市朝阳区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('李四公司', 'COMPANY', '13900000002', 'lisi@company.com', '北京市海淀区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('王五', 'PERSON', '13900000003', 'wangwu@example.com', '北京市西城区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试案件
INSERT INTO "case" (case_name, case_number, case_type, procedure, case_reason, court, level, status, owner_id, filing_date, deadline_date, created_at, updated_at) VALUES
('张三诉李四合同纠纷案', '2026-CIVIL-0001', 'CIVIL', 'FIRST', '合同纠纷', '北京市朝阳区人民法院', 'IMPORTANT', 'ACTIVE', 2, '2026-04-01', '2026-07-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('王五劳动争议案', '2026-CIVIL-0002', 'CIVIL', 'FIRST', '劳动争议', '北京市海淀区人民法院', 'NORMAL', 'ACTIVE', 2, '2026-04-05', '2026-06-15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('李四公司商标侵权案', '2026-CIVIL-0003', 'CIVIL', 'FIRST', '商标侵权', '北京市西城区人民法院', 'IMPORTANT', 'CONSULTATION', 3, '2026-04-10', '2026-10-20', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试待办
INSERT INTO todo (title, description, priority, status, assignee_id, case_id, due_date, created_at, updated_at) VALUES
('准备张三案证据材料', '收集和整理张三诉李四合同纠纷案的证据材料', 'HIGH', 'PENDING', 2, 1, '2026-04-20 18:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('提交王五案起诉状', '向法院提交王五劳动争议案的起诉状', 'MEDIUM', 'PENDING', 2, 2, '2026-04-25 12:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('联系李四公司法务', '沟通商标侵权案的和解事宜', 'LOW', 'COMPLETED', 3, 3, '2026-04-18 17:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('张三案庭前准备', '准备开庭陈述和证据清单', 'HIGH', 'PENDING', 2, 1, '2026-04-15 10:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试日程
INSERT INTO calendar_event (title, type, start_time, end_time, location, description, creator_id, created_at, updated_at) VALUES
('张三案开庭', 'HEARING', '2026-04-20 09:00:00', '2026-04-20 12:00:00', '北京市朝阳区人民法院第三法庭', '张三诉李四合同纠纷案开庭审理', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('案件讨论会', 'MEETING', '2026-04-18 14:00:00', '2026-04-18 16:00:00', '律所会议室A', '讨论王五劳动争议案策略', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试通知
INSERT INTO notification (title, content, type, receiver_id, is_read, created_at, updated_at) VALUES
('新案件待办提醒', '您有一个新的待办事项：准备张三案证据材料', 'TODO', 2, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('系统通知', '欢迎使用律所智能案件管理系统', 'SYSTEM', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试考勤记录
INSERT INTO attendance_record (attendance_type, sub_type, user_id, start_date, end_date, duration, duration_unit, reason, approval_status, created_at, updated_at) VALUES
('LEAVE', '年假', 2, '2026-04-15', '2026-04-17', 3.0, 'DAY', '家庭事务', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LEAVE', '病假', 3, '2026-04-10', '2026-04-11', 2.0, 'DAY', '身体不适，医院开具病假证明', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BUSINESS_TRIP', '出差', 2, '2026-04-20', '2026-04-22', 3.0, 'DAY', '上海出庭', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('OVERTIME', '加班', 4, '2026-04-18', '2026-04-18', 4.0, 'HOUR', '整理案卷材料', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试办公用品
INSERT INTO office_supplies (name, category, specification, unit, stock_quantity, min_stock, unit_price, supplier, location, status, created_at, updated_at) VALUES
('A4打印纸', '纸品', '80g 500张/包', '包', 50, 10, 25.00, '晨光文具', 'A柜', 'IN_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('签字笔', '文具', '0.5mm 黑色', '支', 100, 20, 1.50, '晨光文具', 'B柜', 'IN_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('订书机', '文具', '重型', '个', 8, 3, 15.00, '得力文具', 'B柜', 'IN_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('文件夹', '文具', 'A4 蓝色', '个', 30, 10, 2.50, '得力文具', 'A柜', 'IN_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('计算器', '电子配件', '12位 太阳能', '个', 3, 2, 35.00, '卡西欧', 'C柜', 'IN_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('U盘', '电子配件', '32GB', '个', 2, 5, 45.00, '闪迪', 'C柜', 'LOW_STOCK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 插入测试固定资产
INSERT INTO fixed_asset (asset_name, asset_category, asset_code, specification, purchase_date, purchase_price, current_value, department, custodian, location, usage_status, depreciation_years, depreciation_method, created_at, updated_at) VALUES
('办公电脑', '电子设备', 'IT-2024-001', '联想ThinkPad E15', '2024-01-15', 5500.00, 4583.33, '技术部', '张三', '3楼办公室', 'IN_USE', 5, 'STRAIGHT_LINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('打印机', '办公设备', 'OF-2023-015', 'HP LaserJet Pro', '2023-06-20', 2800.00, 2100.00, '行政部', '李四', '2楼办公区', 'IN_USE', 5, 'STRAIGHT_LINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('办公桌椅', '家具', 'FU-2022-008', '人体工学椅', '2022-03-10', 1500.00, 900.00, '会议室', '王五', '4楼会议室', 'IN_USE', 5, 'STRAIGHT_LINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('轿车', '车辆', 'VE-2021-001', '大众帕萨特', '2021-05-01', 180000.00, 108000.00, '行政部', '赵六', '地下车库', 'IN_USE', 10, 'STRAIGHT_LINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;

-- ==================================================
-- 受理单位模块表（财产保全、案件执行、庭审记录）
-- ==================================================

-- 财产保全表
CREATE TABLE IF NOT EXISTS `property_preservation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `case_id` BIGINT NOT NULL COMMENT '案件ID',
    `target_person` VARCHAR(100) NOT NULL COMMENT '被申请人',
    `preservation_target` VARCHAR(500) NOT NULL COMMENT '保全标的',
    `amount` DECIMAL(15,2) COMMENT '金额',
    `court` VARCHAR(200) NOT NULL COMMENT '管辖法院',
    `preservation_date` DATE NOT NULL COMMENT '保全日期',
    `status` VARCHAR(20) NOT NULL COMMENT '状态',
    `case_number` VARCHAR(50) COMMENT '案件号',
    `insurance_amount` DECIMAL(15,2) COMMENT '保险金额',
    `insurance_company` VARCHAR(200) COMMENT '保险公司',
    `guarantee_type` VARCHAR(50) COMMENT '担保方式',
    `remarks` TEXT COMMENT '备注',
    `created_by` BIGINT NOT NULL COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_case_id` (`case_id`),
    INDEX `idx_preservation_date` (`preservation_date`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财产保全表';

-- 案件执行表
CREATE TABLE IF NOT EXISTS `case_execution` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `case_id` BIGINT NOT NULL COMMENT '案件ID',
    `execution_case_number` VARCHAR(100) NOT NULL COMMENT '执行案号',
    `court` VARCHAR(200) NOT NULL COMMENT '管辖法院',
    `applicant` VARCHAR(100) NOT NULL COMMENT '申请人',
    `respondent` VARCHAR(100) NOT NULL COMMENT '被执行人',
    `execution_target` VARCHAR(200) COMMENT '执行标的',
    `amount` DECIMAL(15,2) COMMENT '金额',
    `execution_date` DATE NOT NULL COMMENT '执行日期',
    `status` VARCHAR(20) NOT NULL COMMENT '状态',
    `executed_amount` DECIMAL(15,2) COMMENT '已执行金额',
    `execution_milestone` VARCHAR(200) COMMENT '执行里程碑',
    `next_step` VARCHAR(200) COMMENT '下一步',
    `remarks` TEXT COMMENT '备注',
    `created_by` BIGINT NOT NULL COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_case_id` (`case_id`),
    INDEX `idx_execution_date` (`execution_date`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案件执行表';

-- 庭审记录表
CREATE TABLE IF NOT EXISTS `hearing_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `case_id` BIGINT NOT NULL COMMENT '案件ID',
    `hearing_date` DATE NOT NULL COMMENT '庭审日期',
    `hearing_time` DATETIME NOT NULL COMMENT '庭审时间',
    `court_location` VARCHAR(200) NOT NULL COMMENT '法庭地点',
    `hearing_type` VARCHAR(50) NOT NULL COMMENT '庭审类型',
    `judge` VARCHAR(100) COMMENT '法官',
    `clerk` VARCHAR(100) COMMENT '书记员',
    `opposing_lawyers` VARCHAR(500) COMMENT '对方律师',
    `hearing_summary` TEXT COMMENT '庭审总结',
    `key_arguments` TEXT COMMENT '关键论点',
    `evidence_submitted` TEXT COMMENT '提交证据',
    `court_focus` TEXT COMMENT '法庭焦点',
    `next_hearing_date` DATE COMMENT '下次庭审日期',
    `attachments` TEXT COMMENT '附件',
    `remarks` TEXT COMMENT '备注',
    `created_by` BIGINT NOT NULL COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_case_id` (`case_id`),
    INDEX `idx_hearing_date` (`hearing_date`),
    INDEX `idx_hearing_type` (`hearing_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='庭审记录表';

-- 知识库文章表
CREATE TABLE IF NOT EXISTS `knowledge_article` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `article_type` VARCHAR(20) NOT NULL COMMENT '文章类型',
    `category` VARCHAR(50) COMMENT '分类',
    `tags` VARCHAR(500) COMMENT '标签',
    `summary` VARCHAR(1000) COMMENT '摘要',
    `content` TEXT COMMENT '内容',
    `attachment_path` VARCHAR(500) COMMENT '附件路径',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞次数',
    `is_top` BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    `is_public` BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `author_name` VARCHAR(50) COMMENT '作者姓名',
    `updater_id` BIGINT COMMENT '更新人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_category` (`category`),
    INDEX `idx_type` (`article_type`),
    INDEX `idx_tags` (`tags`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文章表';

-- 插入测试知识库文章
INSERT INTO knowledge_article (title, article_type, category, tags, summary, content, view_count, like_count, is_top, is_public, author_id, author_name, created_at, updated_at, deleted) VALUES
('劳动合同解除通知书模板', 'TEMPLATE', '劳动', '劳动合同,解除,通知', '标准劳动合同解除通知书模板，包含多种解除情形', '<h2>劳动合同解除通知书</h2><p>尊敬的______：</p><p>根据《劳动合同法》相关规定...</p>', 128, 15, true, true, 2, '张三律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('合同纠纷胜诉案例：违约金过高调整', 'CASE', '合同', '违约金,调整,胜诉', '代理某公司买卖合同纠纷案，成功将违约金从30%调整至实际损失的1.3倍', '<h2>案情简介</h2><p>原告某公司起诉被告...</p><h2>争议焦点</h2><p>违约金是否过高...</p>', 256, 32, true, true, 2, '张三律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('办案指南：如何高效收集证据', 'GUIDE', '办案技巧', '证据,收集,办案', '证据收集的三大原则和实务技巧分享', '<h2>证据收集三原则</h2><p>1. 及时性原则...</p>', 89, 8, false, true, 3, '李四律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('经验分享：与当事人沟通的5个技巧', 'EXPERIENCE', '沟通', '当事人,沟通,经验', '多年办案总结的当事人沟通心得', '<h2>沟通技巧</h2><p>1. 倾听为先...</p>', 45, 3, false, true, 3, '李四律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('交通事故赔偿计算标准', 'TEMPLATE', '侵权', '交通事故,赔偿,计算', '2024年最新交通事故赔偿计算标准及公式', '<h2>赔偿项目</h2><p>1. 医疗费...</p>', 312, 45, true, true, 2, '张三律师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

