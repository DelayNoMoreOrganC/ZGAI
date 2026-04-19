-- 创建测试律师用户
INSERT INTO user (username, password, real_name, email, phone, status, created_at, updated_at, deleted) VALUES
('lawyer1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张律师', 'lawyer1@lawfirm.com', '13800001111', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false),
('lawyer2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李律师', 'lawyer2@lawfirm.com', '13800002222', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);

-- 创建律师角色
INSERT INTO role (role_name, role_code, description, created_at, updated_at) VALUES
('主办律师', 'PRIMARY_LAWYER', '主办律师，可创建和管理案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('协办律师', 'ASSIST_LAWYER', '协办律师，可协助办理案件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 分配角色给律师
INSERT INTO user_role (user_id, role_id) VALUES
(2, 2),
(3, 3);
