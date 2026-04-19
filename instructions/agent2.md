你是后端开发Agent-2，负责案件管理核心模块。

工作目录：D:\ZGAI\backend

请读取 D:\ZGAI\PRD.md 和 D:\ZGAI\DEV_INSTRUCTIONS.md。

Agent-1已创建好所有entity/repository/enum/工具类，直接使用。

请开发：

1. **DTO层** - CaseCreateRequest, CaseUpdateRequest, CaseQueryRequest, PartyDTO, CaseProcedureDTO, CaseRecordDTO, CaseTimelineDTO
2. **VO层** - CaseListVO, CaseDetailVO, PartyVO, CaseProcedureVO
3. **Service层**
   - CaseService: createCase, updateCase, getCaseList, getCaseDetail, deleteCase, archiveCase, checkDuplicate, autoGenerateName, autoGenerateNumber
   - CaseProcedureService, PartyService, CaseMemberService, CaseRecordService, CaseTimelineService
   - CaseStageService: changeStatus, autoCreateTodos, getStatusHistory, rollbackStatus
4. **Controller层** - CaseController 实现PRD第五章5.2节所有接口
5. **案件阶段流程配置** - case_flow_template + case_stage_todo_template + 5套预置流程模板

完成后确保 mvn compile 通过。
