package com.lawfirm.service;

import com.lawfirm.dto.NpaDueDiligenceDTO;
import com.lawfirm.entity.NpaAsset;
import com.lawfirm.entity.NpaDueDiligence;
import com.lawfirm.repository.NpaAssetRepository;
import com.lawfirm.repository.NpaDueDiligenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 尽职调查服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpaDueDiligenceService {

    private final NpaDueDiligenceRepository dueDiligenceRepository;
    private final NpaAssetRepository npaAssetRepository;

    /**
     * 获取某债权的尽调记录
     */
    public List<NpaDueDiligenceDTO> listByAsset(Long assetId) {
        return dueDiligenceRepository.findByAssetIdAndDeletedFalse(assetId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取尽调详情
     */
    public NpaDueDiligenceDTO getDueDiligence(Long id) {
        NpaDueDiligence dd = dueDiligenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("尽调记录不存在: " + id));
        return toDTO(dd);
    }

    /**
     * 创建尽调
     */
    @Transactional
    public NpaDueDiligenceDTO createDueDiligence(NpaDueDiligenceDTO dto) {
        npaAssetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new RuntimeException("债权不存在: " + dto.getAssetId()));

        NpaDueDiligence dd = new NpaDueDiligence();
        BeanUtils.copyProperties(dto, dd, "id", "aiGenerated");
        if (dd.getAiGenerated() == null) dd.setAiGenerated(false);
        dd = dueDiligenceRepository.save(dd);
        return toDTO(dd);
    }

    /**
     * 更新尽调
     */
    @Transactional
    public NpaDueDiligenceDTO updateDueDiligence(Long id, NpaDueDiligenceDTO dto) {
        NpaDueDiligence dd = dueDiligenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("尽调记录不存在: " + id));
        BeanUtils.copyProperties(dto, dd, "id", "assetId", "aiGenerated");
        dd = dueDiligenceRepository.save(dd);
        return toDTO(dd);
    }

    /**
     * 删除尽调记录
     */
    @Transactional
    public void deleteDueDiligence(Long id) {
        NpaDueDiligence dd = dueDiligenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("尽调记录不存在: " + id));
        dd.setDeleted(true);
        dueDiligenceRepository.save(dd);
    }

    private NpaDueDiligenceDTO toDTO(NpaDueDiligence dd) {
        NpaDueDiligenceDTO dto = new NpaDueDiligenceDTO();
        BeanUtils.copyProperties(dd, dto);

        // 补充债务人名称
        npaAssetRepository.findById(dd.getAssetId()).ifPresent(asset ->
                dto.setDebtorName(asset.getDebtorName()));

        return dto;
    }
}
