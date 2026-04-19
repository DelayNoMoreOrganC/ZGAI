package com.lawfirm.init;

import com.lawfirm.entity.StageTodoTemplate;
import com.lawfirm.repository.StageTodoTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 待办事项模板初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoTemplateInitializer implements CommandLineRunner {

    private final StageTodoTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        // 检查是否已经初始化
        if (templateRepository.count() > 0) {
            log.info("待办模板已存在，跳过初始化");
            return;
        }

        log.info("开始初始化案件阶段待办模板...");

        // 民事案件模板
        List<StageTodoTemplate> civilTemplates = Arrays.asList(
                // 咨询阶段
                createTemplate("咨询", "民事", "整理案件基本材料", "收集当事人身份证明、合同等基础材料", 2, 0, 1),
                createTemplate("咨询", "民事", "初步法律分析", "分析案件法律关系和可能的诉讼策略", 1, 1, 2),
                createTemplate("咨询", "民事", "制作咨询笔录", "记录当事人陈述和律师建议", 2, 1, 3),

                // 签约阶段
                createTemplate("签约", "民事", "起草委托代理合同", "准备法律服务合同文本", 1, 0, 1),
                createTemplate("签约", "民事", "办理委托手续", "签署授权委托书等文件", 1, 1, 2),
                createTemplate("签约", "民事", "收取律师费", "按照约定收取首批律师费", 2, 2, 3),

                // 起草文书阶段
                createTemplate("起草文书", "民事", "起草起诉状", "根据案件事实起草起诉状", 1, 0, 1),
                createTemplate("起草文书", "民事", "准备证据清单", "整理证据材料并制作清单", 1, 1, 2),
                createTemplate("起草文书", "民事", "校对文书材料", "检查文书格式和内容", 2, 2, 3),

                // 立案阶段
                createTemplate("立案", "民事", "网上立案", "通过法院网上立案系统提交材料", 1, 0, 1),
                createTemplate("立案", "民事", "提交纸质材料", "向法院提交纸质立案材料", 1, 1, 2),
                createTemplate("立案", "民事", "缴纳诉讼费", "按照通知缴纳诉讼费用", 1, 2, 3),
                createTemplate("立案", "民事", "领取受理通知书", "领取案件受理通知书等材料", 2, 3, 4),

                // 一审阶段
                createTemplate("一审审理中", "民事", "关注举证期限", "注意举证期限届满时间", 1, 0, 1),
                createTemplate("一审审理中", "民事", "准备代理词", "根据庭审情况准备代理词", 1, 7, 2),
                createTemplate("一审审理中", "民事", "跟进庭审安排", "与法院联系确定开庭时间", 2, 3, 3),

                // 执行阶段
                createTemplate("执行", "民事", "申请执行立案", "向法院提交强制执行申请", 1, 0, 1),
                createTemplate("执行", "民事", "提供财产线索", "向法院提供被执行人财产线索", 1, 7, 2),
                createTemplate("执行", "民事", "跟进执行进度", "定期与执行法官沟通", 2, 15, 3)
        );

        // 刑事案件模板
        List<StageTodoTemplate> criminalTemplates = Arrays.asList(
                createTemplate("咨询", "刑事", "了解案情", "详细向家属了解案件情况", 1, 0, 1),
                createTemplate("签约", "刑事", "办理委托手续", "签署刑事辩护委托合同", 1, 0, 1),
                createTemplate("会见", "刑事", "安排看守所会见", "向办案机关申请会见嫌疑人", 1, 0, 1),
                createTemplate("审查起诉", "刑事", "阅卷", "到检察院查阅复制案卷材料", 1, 3, 1),
                createTemplate("审查起诉", "刑事", "提交法律意见书", "向检察院提交辩护意见", 1, 10, 2)
        );

        templateRepository.saveAll(civilTemplates);
        templateRepository.saveAll(criminalTemplates);

        log.info("待办模板初始化完成，共创建 {} 个模板",
                civilTemplates.size() + criminalTemplates.size());
    }

    private StageTodoTemplate createTemplate(String stageName, String caseType,
                                             String title, String description,
                                             int priority, int relativeDays, int sortOrder) {
        StageTodoTemplate template = new StageTodoTemplate();
        template.setStageName(stageName);
        template.setCaseType(caseType);
        template.setTodoTitle(title);
        template.setTodoDescription(description);
        template.setPriority(priority);
        template.setRelativeDays(relativeDays);
        template.setSortOrder(sortOrder);
        template.setIsEnabled(true);
        return template;
    }
}
