package com.lawfirm.service;

import com.lawfirm.repository.NpaAssetRepository;
import com.lawfirm.repository.NpaPackageRepository;
import com.lawfirm.repository.NpaDisposalResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 不良资产绩效统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpaPerformanceService {

    private final NpaPackageRepository npaPackageRepository;
    private final NpaAssetRepository npaAssetRepository;
    private final NpaDisposalResultRepository resultRepository;

    /**
     * 综合绩效看板数据
     */
    public Map<String, Object> getPerformanceDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // 1. 总体概览
        dashboard.put("overview", getOverview());

        // 2. 按银行统计
        dashboard.put("byBank", getStatsByBank());

        // 3. 按处置方式统计
        dashboard.put("byDisposalMethod", getStatsByDisposalMethod());

        // 4. 月度回收趋势
        dashboard.put("monthlyTrend", getMonthlyRecoveryTrend());

        // 5. 风险分布
        dashboard.put("riskDistribution", getRiskDistribution());

        return dashboard;
    }

    /**
     * 总体概览卡片
     */
    private Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        long totalAssets = npaAssetRepository.countByDeletedFalse();
        long recovered = npaAssetRepository.countByStatusAndDeletedFalse("RECOVERED");
        long inProgress = npaAssetRepository.countByStatusAndDeletedFalse("IN_PROGRESS");
        long pending = npaAssetRepository.countByStatusAndDeletedFalse("PENDING");
        long chargedOff = npaAssetRepository.countByStatusAndDeletedFalse("CHARGE_OFF");

        // 回收率
        BigDecimal totalAmount = npaPackageRepository.sumTotalAmountByDeletedFalse();
        BigDecimal totalRecovered = npaPackageRepository.sumRecoveredAmountByActivePackages();
        BigDecimal recoveryRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalRecovered.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        overview.put("totalAssets", totalAssets);
        overview.put("recoveredCount", recovered);
        overview.put("inProgressCount", inProgress);
        overview.put("pendingCount", pending);
        overview.put("chargedOffCount", chargedOff);
        overview.put("totalAmount", totalAmount);
        overview.put("totalRecovered", totalRecovered);
        overview.put("recoveryRate", recoveryRate);

        return overview;
    }

    /**
     * 按银行统计
     */
    private List<Map<String, Object>> getStatsByBank() {
        // 由于是 JPA，我们用简单方式统计
        var packages = npaPackageRepository.findAll();
        Map<String, List<Long>> bankPackageMap = new LinkedHashMap<>();

        for (var pkg : packages) {
            if (pkg.getDeleted() || pkg.getBankName() == null) continue;
            bankPackageMap.computeIfAbsent(pkg.getBankName(), k -> new ArrayList<>())
                    .add(pkg.getId());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : bankPackageMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bankName", entry.getKey());
            item.put("packageCount", entry.getValue().size());
            // 简单汇总
            BigDecimal totalAmt = BigDecimal.ZERO;
            BigDecimal recvAmt = BigDecimal.ZERO;
            for (Long pkgId : entry.getValue()) {
                var opt = npaPackageRepository.findByIdAndDeletedFalse(pkgId);
                if (opt.isPresent()) {
                    var pkg = opt.get();
                    totalAmt = totalAmt.add(pkg.getTotalAmount() != null ? pkg.getTotalAmount() : BigDecimal.ZERO);
                    recvAmt = recvAmt.add(pkg.getRecoveredAmount() != null ? pkg.getRecoveredAmount() : BigDecimal.ZERO);
                }
            }
            item.put("totalAmount", totalAmt);
            item.put("recoveredAmount", recvAmt);
            item.put("recoveryRate", totalAmt.compareTo(BigDecimal.ZERO) > 0
                    ? recvAmt.multiply(BigDecimal.valueOf(100)).divide(totalAmt, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            result.add(item);
        }

        return result;
    }

    /**
     * 按处置方式统计（TODO: 后续完善）
     */
    private List<Map<String, Object>> getStatsByDisposalMethod() {
        // 简化版实现
        return List.of(
            Map.of("method", "诉讼", "count", 0),
            Map.of("method", "执行", "count", 0),
            Map.of("method", "和解", "count", 0),
            Map.of("method", "债权转让", "count", 0)
        );
    }

    /**
     * 月度回收趋势（近12个月）
     */
    private List<Map<String, Object>> getMonthlyRecoveryTrend() {
        // 简化版：后续可以加上真正的按月统计SQL
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month.getYear() + "-" + String.format("%02d", month.getMonthValue()));
            point.put("amount", BigDecimal.ZERO);
            trend.add(point);
        }
        return trend;
    }

    /**
     * 风险分布
     */
    private Map<String, Object> getRiskDistribution() {
        // 简化版：后续可按 NpaAsset.riskLevel 分组统计
        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("high", 0);
        dist.put("medium", 0);
        dist.put("low", 0);
        return dist;
    }
}
