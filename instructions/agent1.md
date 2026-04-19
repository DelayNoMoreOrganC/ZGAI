你是后端开发Agent-1，负责基础层和认证系统。

工作目录：D:\ZGAI\backend

请读取 D:\ZGAI\PRD.md 和 D:\ZGAI\DEV_INSTRUCTIONS.md，然后开发以下内容：

1. **数据库初始化SQL脚本** (src/main/resources/schema.sql)
   - 所有29个实体的建表SQL（InnoDB引擎，UTF8MB4字符集）
   - 初始化数据（角色/权限/管理员账号admin/admin123/字典数据）

2. **JPA实体类** (src/main/java/com/lawfirm/entity/)
   - 所有29个实体，严格按PRD第四章数据模型
   - 包含关联关系注解（@OneToMany, @ManyToOne等）
   - 审计字段（createdAt, updatedAt）用 @CreationTimestamp @UpdateTimestamp
   - 逻辑删除字段（deleted）

3. **Spring Security + JWT认证**
   - JwtUtil: 生成/验证Token（密钥从配置文件读取）
   - JwtAuthFilter: 请求拦截，校验Token
   - SecurityConfig: 权限配置，放行登录接口
   - AuthController: POST /api/auth/login, POST /api/auth/logout, POST /api/auth/change-password, GET /api/auth/current-user
   - 登录失败5次锁定30分钟（Redis计数）
   - 密码BCrypt加密

4. **全局异常处理** (exception/GlobalExceptionHandler.java)
   - @RestControllerAdvice
   - 处理参数校验异常、权限异常、业务异常、未知异常
   - 统一错误返回格式

5. **通用工具类**
   - Result<T>: 统一返回 {code, message, data}
   - PageResult<T>: 分页返回 {page, size, total, records}
   - RedisUtil: 封装Redis常用操作
   - BusinessException: 自定义业务异常

6. **枚举类** (src/main/java/com/lawfirm/enums/)
   - CaseType, CaseStatus, CaseLevel, PartyType, PartyRole
   - TodoStatus, TodoPriority, FinanceType, PaymentMethod
   - ApprovalStatus, ClientType, ClientStatus, CalendarType
   - DocumentType, RoleCode, FeeMethod, CloseStatus, AIFunctionType

7. **Repository层** - 所有实体的基础Repository接口

8. **application.yml配置**
   - MySQL/Redis/MinIO/JWT/文件上传配置

完成后确保 mvn compile 通过，无语法错误。
