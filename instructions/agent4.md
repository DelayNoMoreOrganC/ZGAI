你是后端开发Agent-4，负责审批、行政OA、系统管理模块。

工作目录：D:\ZGAI\backend

请读取 D:\ZGAI\PRD.md 和 D:\ZGAI\DEV_INSTRUCTIONS.md。

Agent-1已创建好所有entity/repository/enum/工具类，直接使用。

请开发：

1. **审批管理** - ApprovalService（同意/驳回/转审/撤回/催办，多级节点流转，6种预置模板）
2. **通知公告** - AnnouncementService（发布/范围/已读未读）
3. **通知系统** - NotificationService（站内通知/已读未读/分类/角标）
4. **会议室管理** - MeetingRoomService, MeetingBookingService（预约/冲突校验/开庭联动）
5. **考勤管理** - AttendanceService（请假出差加班/审批/报表）
6. **系统管理** - UserService, RoleService, DepartmentService, AuditLogService(AOP切面), SystemConfigService

完成后确保 mvn compile 通过。
