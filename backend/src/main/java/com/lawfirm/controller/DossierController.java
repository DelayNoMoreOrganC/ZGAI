package com.lawfirm.controller;

import com.lawfirm.entity.Dossier;
import com.lawfirm.repository.DossierRepository;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cases/{caseId}/dossiers")
@RequiredArgsConstructor
public class DossierController {
    private final DossierRepository dossierRepository;
    private final SecurityUtils securityUtils;
    
    @GetMapping
    public Result<List<Dossier>> getDossiers(@PathVariable Long caseId) {
        List<Dossier> dossiers = dossierRepository.findByCaseIdOrderBySortOrder(caseId);
        return Result.success(dossiers);
    }
    
    @PostMapping
    public Result<Dossier> createDossier(@PathVariable Long caseId, @RequestBody Dossier dossier) {
        dossier.setCaseId(caseId);
        dossier.setCreatedAt(LocalDateTime.now());
        dossier.setUpdatedAt(LocalDateTime.now());
        Dossier saved = dossierRepository.save(dossier);
        return Result.success(saved);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteDossier(@PathVariable Long id) {
        dossierRepository.deleteById(id);
        return Result.success();
    }
}
