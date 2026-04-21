package com.lawfirm.service;

import com.lawfirm.dto.OfficeSupplyDTO;
import com.lawfirm.dto.SupplyOperationDTO;
import com.lawfirm.entity.OfficeSupply;
import com.lawfirm.entity.SupplyRecord;
import com.lawfirm.repository.OfficeSupplyRepository;
import com.lawfirm.repository.SupplyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 办公用品Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeSupplyService {

    private final OfficeSupplyRepository officeSupplyRepository;
    private final SupplyRecordRepository supplyRecordRepository;

    /**
     * 分页查询用品
     */
    public Page<OfficeSupplyDTO> getSupplies(String name, String category, String stockStatus, Pageable pageable) {
        Page<OfficeSupply> supplies;

        // 简化实现：先不分筛选条件，后续可以添加Specification
        supplies = officeSupplyRepository.findByDeletedFalse(pageable);

        return supplies.map(this::toDTO);
    }

    /**
     * 根据ID查询用品详情
     */
    public OfficeSupplyDTO getSupplyDetail(Long id) {
        OfficeSupply supply = officeSupplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("用品不存在"));

        OfficeSupplyDTO dto = toDTO(supply);

        // 加载出入库记录
        List<SupplyRecord> records = supplyRecordRepository.findBySupplyIdOrderByCreatedAtDesc(id);
        dto.setRecords(records);

        return dto;
    }

    /**
     * 创建用品
     */
    @Transactional
    public OfficeSupplyDTO createSupply(OfficeSupplyDTO dto) {
        OfficeSupply supply = new OfficeSupply();
        supply.setName(dto.getName());
        supply.setCategory(dto.getCategory());
        supply.setSpecification(dto.getSpecification());
        supply.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0);
        supply.setUnit(dto.getUnit());
        supply.setUnitPrice(dto.getUnitPrice());
        supply.setMinStock(dto.getMinStock() != null ? dto.getMinStock() : 10);
        supply.setLocation(dto.getLocation());
        supply.setRemark(dto.getRemark());

        supply = officeSupplyRepository.save(supply);
        log.info("创建办公用品成功: id={}, name={}", supply.getId(), supply.getName());

        return toDTO(supply);
    }

    /**
     * 更新用品
     */
    @Transactional
    public OfficeSupplyDTO updateSupply(Long id, OfficeSupplyDTO dto) {
        OfficeSupply supply = officeSupplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("用品不存在"));

        supply.setName(dto.getName());
        supply.setCategory(dto.getCategory());
        supply.setSpecification(dto.getSpecification());
        supply.setQuantity(dto.getQuantity());
        supply.setUnit(dto.getUnit());
        supply.setUnitPrice(dto.getUnitPrice());
        supply.setMinStock(dto.getMinStock());
        supply.setLocation(dto.getLocation());
        supply.setRemark(dto.getRemark());

        supply = officeSupplyRepository.save(supply);
        log.info("更新办公用品成功: id={}", id);

        return toDTO(supply);
    }

    /**
     * 删除用品
     */
    @Transactional
    public void deleteSupply(Long id) {
        OfficeSupply supply = officeSupplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("用品不存在"));

        supply.setDeleted(true);
        officeSupplyRepository.save(supply);

        log.info("删除办公用品成功: id={}", id);
    }

    /**
     * 入库
     */
    @Transactional
    public void inbound(Long id, SupplyOperationDTO operation) {
        OfficeSupply supply = officeSupplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("用品不存在"));

        // 增加库存
        supply.setQuantity(supply.getQuantity() + operation.getQuantity());
        supply.setLastInboundDate(LocalDate.now());

        officeSupplyRepository.save(supply);

        // 记录出入库
        SupplyRecord record = new SupplyRecord();
        record.setSupplyId(id);
        record.setType("入库");
        record.setQuantity(operation.getQuantity());
        record.setOperator(operation.getOperator());
        record.setRemark(operation.getRemark());

        supplyRecordRepository.save(record);

        log.info("入库成功: supplyId={}, quantity={}", id, operation.getQuantity());
    }

    /**
     * 出库
     */
    @Transactional
    public void outbound(Long id, SupplyOperationDTO operation) {
        OfficeSupply supply = officeSupplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("用品不存在"));

        // 检查库存
        if (supply.getQuantity() < operation.getQuantity()) {
            throw new RuntimeException("库存不足");
        }

        // 减少库存
        supply.setQuantity(supply.getQuantity() - operation.getQuantity());
        officeSupplyRepository.save(supply);

        // 记录出入库
        SupplyRecord record = new SupplyRecord();
        record.setSupplyId(id);
        record.setType("出库");
        record.setQuantity(operation.getQuantity());
        record.setOperator(operation.getOperator());
        record.setReceiver(operation.getReceiver());
        record.setPurpose(operation.getPurpose());
        record.setRemark(operation.getRemark());

        supplyRecordRepository.save(record);

        log.info("出库成功: supplyId={}, quantity={}", id, operation.getQuantity());
    }

    /**
     * 转换为DTO
     */
    private OfficeSupplyDTO toDTO(OfficeSupply supply) {
        OfficeSupplyDTO dto = new OfficeSupplyDTO();
        dto.setId(supply.getId());
        dto.setName(supply.getName());
        dto.setCategory(supply.getCategory());
        dto.setSpecification(supply.getSpecification());
        dto.setQuantity(supply.getQuantity());
        dto.setUnit(supply.getUnit());
        dto.setUnitPrice(supply.getUnitPrice());
        dto.setMinStock(supply.getMinStock());
        dto.setLocation(supply.getLocation());
        dto.setLastInboundDate(supply.getLastInboundDate());
        dto.setRemark(supply.getRemark());
        return dto;
    }
}
