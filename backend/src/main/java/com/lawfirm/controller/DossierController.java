package com.lawfirm.controller;

import com.lawfirm.entity.Dossier;
import com.lawfirm.repository.DossierRepository;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CaseService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cases/{caseId}/dossiers")
@RequiredArgsConstructor
public class DossierController {
    private final DossierRepository dossierRepository;
    private final SecurityUtils securityUtils;
    private final CaseService caseService;
    
    @GetMapping
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<Dossier>> getDossiers(@PathVariable Long caseId) {
        assertCaseVisible(caseId);
        List<Dossier> dossiers = dossierRepository.findByCaseIdOrderBySortOrder(caseId);
        return Result.success(dossiers);
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Dossier> createDossier(@PathVariable Long caseId, @RequestBody Dossier dossier) {
        assertCaseEditable(caseId);
        dossier.setCaseId(caseId);
        dossier.setCreatedAt(LocalDateTime.now());
        dossier.setUpdatedAt(LocalDateTime.now());
        Dossier saved = dossierRepository.save(dossier);
        return Result.success(saved);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteDossier(@PathVariable Long caseId, @PathVariable Long id) {
        assertCaseEditable(caseId);
        dossierRepository.deleteById(id);
        return Result.success();
    }

    private void assertCaseVisible(Long caseId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        caseService.assertCaseVisible(caseId, currentUserId);
    }

    private void assertCaseEditable(Long caseId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        caseService.assertCaseEditable(caseId, currentUserId);
    }
}
