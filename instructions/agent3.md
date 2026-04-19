你是后端开发Agent-3，负责日程、待办、客户、财务模块。

工作目录：D:\ZGAI\backend

请读取 D:\ZGAI\PRD.md 和 D:\ZGAI\DEV_INSTRUCTIONS.md。

Agent-1已创建好所有entity/repository/enum/工具类，直接使用。

请开发：

1. **日程管理** - CalendarService/Controller（月/周/日视图、自动同步案件节点、颜色标签）
2. **待办管理** - TodoService/Controller（紧急度排序、逾期标记、提醒）
3. **客户管理** - ClientService/Controller（CRUD+筛选、沟通记录、利益冲突检索、统计）
4. **财务管理** - FinanceRecordService, LawyerFeeService, PaymentService, InvoiceService, FinanceSummaryService, DashboardService
5. 各Controller实现PRD第5.3-5.6节所有接口

完成后确保 mvn compile 通过。
