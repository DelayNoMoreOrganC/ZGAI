# 开发约定

## 项目根目录
D:\ZGAI

## 技术栈
- 后端: Spring Boot 3.2 + Java 17 + Maven
- 前端: Vue 3 + Element Plus + Vite + Pinia
- 数据库: MySQL 8.0
- 缓存: Redis
- 文件存储: MinIO
- 认证: JWT (Spring Security)

## 后端目录结构
D:\ZGAI\backend\src\main\java\com\lawfirm\
├── config/          # 配置类
├── controller/      # REST控制器
├── service/         # 业务逻辑层
├── repository/      # 数据访问层（JPA）
├── entity/          # JPA实体
├── dto/             # 数据传输对象
├── vo/              # 视图对象
├── enums/           # 枚举类
├── exception/       # 全局异常处理
├── util/            # 工具类
├── ai/              # AI模块
└── LawfirmApplication.java

## 前端目录结构
D:\ZGAI\frontend\src\
├── api/             # API请求封装
├── components/      # 公共组件
├── layouts/         # 布局组件
├── router/          # 路由配置
├── stores/          # Pinia状态管理
├── utils/           # 工具函数
├── views/           # 页面组件
│   ├── dashboard/   ├── case/   ├── calendar/
│   ├── client/      ├── document/   ├── finance/
│   ├── approval/    ├── admin/   ├── statistics/
│   └── settings/
├── App.vue
└── main.js

## 编码规范
1. RESTful API，统一返回: {"code": 200, "message": "success", "data": {}}
2. 分页统一: {"page": 1, "size": 20, "total": 100, "records": []}
3. 异常统一: {"code": 400/401/403/404/500, "message": "错误描述"}
4. 前端axios拦截器自动带JWT Token
5. 所有实体必须有 createdAt, updatedAt 字段
6. 逻辑删除优先于物理删除
7. 所有接口需要权限注解 @PreAuthorize
8. 数据库表名 snake_case，Java字段 camelCase
9. Git提交: [模块] 操作描述
10. Windows路径用 / 或 File.separator
