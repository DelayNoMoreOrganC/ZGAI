package com.lawfirm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HighRiskControllerSecurityTest {

    @Test
    void aiConfigurationRequiresDedicatedAuthority() throws Exception {
        assertEveryPublicEndpointUses(AIConfigController.class, "hasAuthority('AI_CONFIG')");
    }

    @Test
    void systemConfigurationRequiresSystemAuthority() {
        PreAuthorize policy = SystemConfigController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(policy);
        assertEquals("hasAuthority('SYSTEM_CONFIG')", policy.value());
    }

    @Test
    void roleWritesAndReadsUseSeparateAuthorities() throws Exception {
        assertPolicy(RoleController.class, "createRole", "hasAuthority('ROLE_EDIT')",
                com.lawfirm.dto.RoleCreateRequest.class);
        assertPolicy(RoleController.class, "updateRole", "hasAuthority('ROLE_EDIT')",
                Long.class, com.lawfirm.dto.RoleCreateRequest.class);
        assertPolicy(RoleController.class, "deleteRole", "hasAuthority('ROLE_EDIT')", Long.class);
        assertPolicy(RoleController.class, "assignPermissions", "hasAuthority('ROLE_EDIT')",
                Long.class, java.util.Map.class);
        assertPolicy(RoleController.class, "getRoleList", "hasAuthority('ROLE_VIEW')", int.class, int.class);
        assertPolicy(RoleController.class, "getRoleDetail", "hasAuthority('ROLE_VIEW')", Long.class);
        assertPolicy(RoleController.class, "getAllRoles", "hasAuthority('ROLE_VIEW')");
        assertPolicy(RoleController.class, "getAvailablePermissions", "hasAuthority('ROLE_VIEW')");
    }

    @Test
    void departmentDirectoryIsReadableButMutationsRequireSystemAuthority() throws Exception {
        assertPolicy(DepartmentController.class, "getDepartmentList", "isAuthenticated()");
        assertPolicy(DepartmentController.class, "getDepartmentTree", "isAuthenticated()");
        assertPolicy(DepartmentController.class, "getDepartmentDetail", "isAuthenticated()", Long.class);
        assertPolicy(DepartmentController.class, "createDepartment", "hasAuthority('SYSTEM_CONFIG')",
                com.lawfirm.dto.DepartmentCreateRequest.class);
        assertPolicy(DepartmentController.class, "updateDepartment", "hasAuthority('SYSTEM_CONFIG')",
                Long.class, com.lawfirm.dto.DepartmentCreateRequest.class);
        assertPolicy(DepartmentController.class, "deleteDepartment", "hasAuthority('SYSTEM_CONFIG')", Long.class);
    }

    @Test
    void batchCaseMutationsUseCaseAuthorities() throws Exception {
        assertPolicy(CaseBatchController.class, "batchCloseCases", "hasAuthority('CASE_EDIT')",
                com.lawfirm.dto.BatchOperationRequest.class);
        assertPolicy(CaseBatchController.class, "batchArchiveCases", "hasAuthority('CASE_ARCHIVE')",
                com.lawfirm.dto.BatchOperationRequest.class);
        assertPolicy(CaseBatchController.class, "batchDeleteCases", "hasAuthority('CASE_DELETE')",
                com.lawfirm.dto.BatchOperationRequest.class);
        assertPolicy(CaseBatchController.class, "batchChangeOwner", "hasAuthority('CASE_EDIT')",
                com.lawfirm.dto.BatchOperationRequest.class);
    }

    @Test
    void statisticsReadsAndExportsUseSeparateAuthorities() throws Exception {
        assertPolicy(StatisticsController.class, "getOverview", "hasAuthority('STATISTICS_VIEW')",
                java.time.LocalDate.class, java.time.LocalDate.class);
        assertPolicy(StatisticsController.class, "getCaseTrends", "hasAuthority('STATISTICS_VIEW')",
                String.class, java.time.LocalDate.class, java.time.LocalDate.class);
        assertPolicy(StatisticsController.class, "getCaseTypes", "hasAuthority('STATISTICS_VIEW')",
                java.time.LocalDate.class, java.time.LocalDate.class);
        assertPolicy(StatisticsController.class, "getFeeStatistics", "hasAuthority('STATISTICS_VIEW')",
                String.class, java.time.LocalDate.class, java.time.LocalDate.class);
        assertPolicy(StatisticsController.class, "getLawyerPerformance", "hasAuthority('STATISTICS_VIEW')",
                String.class, java.time.LocalDate.class, java.time.LocalDate.class);
        assertPolicy(StatisticsController.class, "getWinRate", "hasAuthority('STATISTICS_VIEW')",
                java.time.LocalDate.class, java.time.LocalDate.class);
        assertPolicy(StatisticsController.class, "getCollectionRate", "hasAuthority('STATISTICS_VIEW')",
                java.time.LocalDate.class, java.time.LocalDate.class);
        assertPolicy(StatisticsController.class, "exportExcel", "hasAuthority('STATISTICS_EXPORT')",
                java.util.Map.class);
        assertPolicy(StatisticsController.class, "exportPdf", "hasAuthority('STATISTICS_EXPORT')",
                java.util.Map.class);
    }

    @Test
    void legacyWorkbenchEndpointsDeclareAuthorities() throws Exception {
        assertPolicy(WorkbenchController.class, "getStats", "hasAuthority('STATISTICS_VIEW')",
                String.class, String.class);
        assertPolicy(WorkbenchController.class, "getCaseTrend", "hasAuthority('STATISTICS_VIEW')",
                String.class, String.class, String.class);
        assertPolicy(WorkbenchController.class, "getTodoStats", "hasAuthority('TODO_VIEW')", Long.class);
        assertPolicy(WorkbenchController.class, "getCalendarStats", "isAuthenticated()",
                Long.class, String.class, String.class);
    }

    @Test
    void personalTodoEndpointsDeclareReadAndWriteAuthorities() throws Exception {
        assertPolicy(TodoController.class, "createTodo", "hasAuthority('TODO_EDIT')",
                com.lawfirm.dto.TodoDTO.class);
        assertPolicy(TodoController.class, "updateTodo", "hasAuthority('TODO_EDIT')",
                Long.class, com.lawfirm.dto.TodoDTO.class);
        assertPolicy(TodoController.class, "deleteTodo", "hasAuthority('TODO_EDIT')", Long.class);
        assertPolicy(TodoController.class, "getTodo", "hasAuthority('TODO_VIEW')", Long.class);
        assertPolicy(TodoController.class, "getTodosByAssignee", "hasAuthority('TODO_VIEW')", Long.class);
        assertPolicy(TodoController.class, "getTodosByCase", "hasAuthority('TODO_VIEW')", Long.class);
    }

    @Test
    void calendarControllerRequiresAuthentication() {
        PreAuthorize policy = CalendarController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(policy);
        assertEquals("isAuthenticated()", policy.value());
    }

    @Test
    void aiUsageControllersRequireAuthentication() {
        assertClassPolicy(AiChatController.class, "isAuthenticated()");
        assertClassPolicy(AIDocumentController.class, "isAuthenticated()");
        assertClassPolicy(AIFeaturesController.class, "isAuthenticated()");
        assertClassPolicy(OcrController.class, "isAuthenticated()");
    }

    @Test
    void documentGenerationUsesCaseViewAuthorityInsteadOfNarrowRoleNames() throws Exception {
        assertPolicy(DocGenerateController.class, "generateDocument", "hasAuthority('CASE_VIEW')",
                com.lawfirm.dto.DocGenerateRequest.class);
    }

    @Test
    void chunkedUploadsRequireDocumentEditAuthority() {
        assertEveryPublicEndpointUses(ChunkedUploadController.class, "hasAuthority('DOCUMENT_EDIT')");
    }

    @Test
    void compatibilityDocumentReadsRequireDocumentViewAuthority() {
        assertEveryPublicEndpointUses(DocumentControllerCompat.class, "hasAuthority('DOCUMENT_VIEW')");
    }

    @Test
    void detailedSystemHealthRequiresSystemAuthority() throws Exception {
        assertPolicy(SystemHealthController.class, "details", "hasAuthority('SYSTEM_CONFIG')");
    }

    @Test
    void auditLogReadsRequireSystemAuthority() {
        assertClassPolicy(AuditLogController.class, "hasAuthority('SYSTEM_CONFIG')");
    }

    @Test
    void knowledgeImportAndReviewRequireDedicatedAuthority() {
        assertEveryPublicEndpointUses(KnowledgeImportController.class, "hasAuthority('KNOWLEDGE_MANAGE')");
    }

    @Test
    void pendingKnowledgeReviewRequiresDedicatedAuthority() throws Exception {
        assertPolicy(KnowledgeArticleController.class, "getPendingReviewArticles",
                "hasAuthority('KNOWLEDGE_MANAGE')", int.class, int.class);
    }

    @Test
    void caseCreationRequiresDedicatedAuthority() throws Exception {
        assertPolicy(CaseController.class, "createCase", "hasAuthority('CASE_CREATE')",
                com.lawfirm.dto.CaseCreateRequest.class);
    }

    @Test
    void archiveWorkflowSeparatesLawyerAndAdministrativeAuthorities() throws Exception {
        assertPolicy(ArchiveWorkflowController.class, "create", "hasAuthority('CASE_ARCHIVE')",
                Long.class, com.lawfirm.dto.ArchiveJobCreateRequest.class);
        assertPolicy(ArchiveWorkflowController.class, "patchDocuments", "hasAuthority('CASE_ARCHIVE')",
                Long.class, com.lawfirm.dto.ArchiveDocumentPatchRequest.class);
        assertPolicy(ArchiveWorkflowController.class, "uploadSupplement", "hasAuthority('CASE_ARCHIVE')",
                Long.class, org.springframework.web.multipart.MultipartFile.class, Integer.class);
        assertPolicy(ArchiveWorkflowController.class, "patchFields", "hasAuthority('CASE_ARCHIVE')",
                Long.class, com.lawfirm.dto.ArchiveFieldsPatchRequest.class);
        assertPolicy(ArchiveWorkflowController.class, "submit", "hasAuthority('CASE_ARCHIVE')", Long.class);
        assertPolicy(ArchiveWorkflowController.class, "review", "hasAuthority('CASE_ARCHIVE_REVIEW')",
                Long.class, com.lawfirm.dto.ArchiveReviewRequest.class);
    }

    @Test
    void formalConflictReviewRequiresFilingReviewAuthority() throws Exception {
        assertPolicy(ClientController.class, "reviewConflictCheck", "hasAuthority('CASE_FILING_REVIEW')",
                Long.class, com.lawfirm.dto.ConflictCheckReviewRequest.class);
        assertPolicy(ClientController.class, "uploadConflictWaiverAttachment", "hasAuthority('CASE_FILING_REVIEW')",
                Long.class, org.springframework.web.multipart.MultipartFile.class);
    }

    @Test
    void clientRelationshipWritesRequireClientEditAuthority() throws Exception {
        assertPolicy(ClientController.class, "createClientRelation", "hasAuthority('CLIENT_EDIT')",
                Long.class, com.lawfirm.dto.ClientSubjectRelationDTO.class);
        assertPolicy(ClientController.class, "deleteClientRelation", "hasAuthority('CLIENT_EDIT')",
                Long.class, Long.class);
    }

    @Test
    void clientCommunicationWritesRequireClientEditAuthority() throws Exception {
        assertPolicy(ClientController.class, "createCommunication", "hasAuthority('CLIENT_EDIT')",
                Long.class, com.lawfirm.dto.CommunicationRecordDTO.class);
        assertPolicy(ClientController.class, "updateCommunication", "hasAuthority('CLIENT_EDIT')",
                Long.class, Long.class, com.lawfirm.dto.CommunicationRecordDTO.class);
        assertPolicy(ClientController.class, "deleteCommunication", "hasAuthority('CLIENT_EDIT')",
                Long.class, Long.class);
    }

    @Test
    void legacyMaterialSearchAndDownloadRequireCaseView() throws Exception {
        assertPolicy(LegacyMaterialSearchController.class, "search", "hasAuthority('CASE_VIEW')",
                com.lawfirm.dto.LegacyMaterialSearchRequest.class);
        assertPolicy(LegacyMaterialSearchController.class, "download", "hasAuthority('CASE_VIEW')",
                Long.class);
    }

    @Test
    void approvalEndpointsDeclareReadAndWriteAuthorities() throws Exception {
        assertPolicy(ApprovalController.class, "createApproval", "hasAuthority('APPROVAL_EDIT')",
                com.lawfirm.dto.ApprovalCreateRequest.class);
        assertPolicy(ApprovalController.class, "approveApproval", "hasAuthority('APPROVAL_EDIT')",
                Long.class, com.lawfirm.dto.ApprovalDecisionRequest.class);
        assertPolicy(ApprovalController.class, "rejectApproval", "hasAuthority('APPROVAL_EDIT')",
                Long.class, com.lawfirm.dto.ApprovalDecisionRequest.class);
        assertPolicy(ApprovalController.class, "transferApproval", "hasAuthority('APPROVAL_EDIT')",
                Long.class, com.lawfirm.dto.ApprovalTransferRequest.class);
        assertPolicy(ApprovalController.class, "withdrawApproval", "hasAuthority('APPROVAL_EDIT')", Long.class);
        assertPolicy(ApprovalController.class, "urgeApproval", "hasAuthority('APPROVAL_VIEW')", Long.class);
        assertPolicy(ApprovalController.class, "getApprovalList", "hasAuthority('APPROVAL_VIEW')",
                com.lawfirm.dto.ApprovalQueryRequest.class);
        assertPolicy(ApprovalController.class, "getApprovalDetail", "hasAuthority('APPROVAL_VIEW')", Long.class);
        assertPolicy(ApprovalController.class, "getApprovalFlow", "hasAuthority('APPROVAL_VIEW')", Long.class);
        assertPolicy(ApprovalController.class, "getApprovalTypes", "hasAuthority('APPROVAL_VIEW')");
    }

    @Test
    void coreBusinessMutationsAreAuditedWithoutRequestBodies() {
        assertEveryMutationAudited(CaseController.class);
        assertEveryMutationAudited(ClientController.class);
        assertEveryMutationAudited(ApprovalController.class);
        assertEveryMutationAudited(CaseDocumentController.class);
        assertEveryMutationAudited(CaseBatchController.class);
        assertEveryMutationAudited(FinanceController.class);
        assertEveryMutationAudited(KnowledgeArticleController.class);
        assertEveryMutationAudited(KnowledgeImportController.class);
        assertEveryMutationAudited(BackupController.class);
        assertEveryMutationAudited(OcrController.class);
    }

    @Test
    void passwordChangesAreAuditedWithoutPasswordBodies() throws Exception {
        assertMutationAudited(AuthController.class.getMethod("changePassword", String.class,
                AuthController.ChangePasswordRequest.class));
        assertMutationAudited(UserController.class.getMethod("changePassword", java.util.Map.class));
    }

    private void assertEveryPublicEndpointUses(Class<?> controllerClass, String expected) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            PreAuthorize policy = method.getAnnotation(PreAuthorize.class);
            assertNotNull(policy, method.getName() + " must declare a method security policy");
            assertEquals(expected, policy.value(), method.getName());
        }
    }

    private void assertPolicy(Class<?> type, String methodName, String expected,
                              Class<?>... parameterTypes) throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        PreAuthorize policy = method.getAnnotation(PreAuthorize.class);
        assertNotNull(policy, methodName + " must declare a method security policy");
        assertEquals(expected, policy.value());
    }

    private void assertClassPolicy(Class<?> type, String expected) {
        PreAuthorize policy = type.getAnnotation(PreAuthorize.class);
        assertNotNull(policy, type.getSimpleName() + " must declare a class security policy");
        assertEquals(expected, policy.value());
    }

    private void assertEveryMutationAudited(Class<?> controllerClass) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            boolean mutation = method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(PutMapping.class)
                    || method.isAnnotationPresent(DeleteMapping.class);
            if (!mutation) {
                continue;
            }
            assertMutationAudited(method);
        }
    }

    private void assertMutationAudited(Method method) {
        com.lawfirm.annotation.AuditLog audit = method.getAnnotation(com.lawfirm.annotation.AuditLog.class);
        assertNotNull(audit, method.getDeclaringClass().getSimpleName() + "." + method.getName()
                + " must be audited");
        org.junit.jupiter.api.Assertions.assertFalse(audit.logParams(),
                method.getDeclaringClass().getSimpleName() + "." + method.getName()
                        + " must not log request bodies");
    }
}
