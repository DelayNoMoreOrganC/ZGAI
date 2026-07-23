package com.lawfirm.init;

import com.lawfirm.entity.StageTodoTemplate;
import com.lawfirm.repository.StageTodoTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 待办事项模板初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(20)
public class TodoTemplateInitializer implements CommandLineRunner {

    private final StageTodoTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        int created = 0;
        for (StageTodoTemplate template : systemTemplates()) {
            boolean exists = templateRepository.existsByCaseTypeAndStageNameAndTodoTitleAndIsDeletedFalse(
                    template.getCaseType(), template.getStageName(), template.getTodoTitle());
            if (!exists) {
                templateRepository.save(template);
                created++;
            }
        }
        log.info("案件阶段待办模板同步完成，本次新增 {} 个模板", created);
    }

    private List<StageTodoTemplate> systemTemplates() {
        List<StageTodoTemplate> templates = new ArrayList<>();

        add(templates, "CIVIL", "接洽利冲", "完成主体核验和利益冲突检查", "核对客户、相对方、关联主体及诉讼时效基础信息", 1, 0);
        add(templates, "CIVIL", "签约立案", "完成民事委托和立案材料检查", "核对合同、授权、主体证明、管辖依据及收费安排", 1, 2);
        add(templates, "CIVIL", "诉前准备", "形成诉讼方案和证据清单", "确认请求、抗辩、保全方案、证据缺口和责任分工", 1, 3);
        add(templates, "CIVIL", "立案或应诉", "登记受理及送达信息", "记录法院案号、承办法官、送达日期和答辩期限", 1, 1);
        add(templates, "CIVIL", "举证答辩", "完成举证答辩期限核验", "维护证据目录、质证意见和补充证据计划", 1, 1);
        add(templates, "CIVIL", "庭审", "完成庭前检查", "核对开庭时间地点、出庭人员、原件和庭审提纲", 1, 1);
        add(templates, "CIVIL", "裁判", "登记裁判结果和送达期限", "提取裁判结果、履行内容及上诉期限并与客户确认", 1, 1);
        add(templates, "CIVIL", "后续程序", "确认履行、上诉或执行方案", "记录客户决定及下一程序的法定期限", 1, 2);
        add(templates, "CIVIL", "结案归档", "完成民事案件结案检查", "核对结案报告、收费、文件目录和客户交付记录", 2, 5);

        add(templates, "ARBITRATION", "接洽利冲", "完成仲裁主体和利益冲突检查", "核对申请人、被申请人及关联主体", 1, 0);
        add(templates, "ARBITRATION", "仲裁条款审查", "核验仲裁协议和适用规则", "确认仲裁事项、机构、仲裁地、语言和送达约定", 1, 1);
        add(templates, "ARBITRATION", "申请或答辩", "完成仲裁申请或答辩材料", "核对请求、反请求、证据和仲裁费用", 1, 3);
        add(templates, "ARBITRATION", "组庭", "登记仲裁员选定和组庭期限", "记录仲裁员意见、回避事项和组庭通知", 1, 1);
        add(templates, "ARBITRATION", "举证", "完成仲裁举证期限核验", "维护证据目录、证人和专家意见", 1, 1);
        add(templates, "ARBITRATION", "开庭", "完成仲裁庭前检查", "核对开庭安排、出席人员、原件和陈述提纲", 1, 1);
        add(templates, "ARBITRATION", "裁决", "登记仲裁裁决结果", "核对裁决金额、履行期限和送达日期", 1, 1);
        add(templates, "ARBITRATION", "执行衔接", "评估仲裁裁决后续程序", "确认履行、撤裁、不予执行或执行安排", 1, 2);
        add(templates, "ARBITRATION", "结案归档", "完成仲裁案件结案检查", "整理仲裁文书、证据、送达和客户交付记录", 2, 5);

        add(templates, "CRIMINAL", "接洽利冲", "核实刑事委托关系和案件阶段", "确认委托人关系、办案机关、强制措施及利益冲突", 1, 0);
        add(templates, "CRIMINAL", "签约", "完成刑事委托手续", "核对委托合同、授权文件、身份及亲属关系材料", 1, 1);
        add(templates, "CRIMINAL", "侦查与会见", "安排会见并更新强制措施", "记录羁押地点、会见情况、办案机关和期限", 1, 1);
        add(templates, "CRIMINAL", "审查起诉", "跟踪移送和审查起诉期限", "登记检察机关、承办人、退补侦查和法律意见", 1, 1);
        add(templates, "CRIMINAL", "阅卷", "完成阅卷目录和证据分析", "核对卷宗完整性、证据问题和辩护方向", 1, 3);
        add(templates, "CRIMINAL", "一审", "完成刑事庭前检查", "准备发问、质证、举证和辩护提纲", 1, 1);
        add(templates, "CRIMINAL", "二审或申诉", "确认上诉或申诉方案", "核对判决送达、上诉期限和当事人意见", 1, 1);
        add(templates, "CRIMINAL", "结案归档", "完成刑事案件结案检查", "整理会见、阅卷、法律意见、庭审和裁判材料", 2, 5);

        add(templates, "ADMINISTRATIVE", "接洽利冲", "完成行政案件主体和利冲检查", "核对行政相对人、行政机关及关联主体", 1, 0);
        add(templates, "ADMINISTRATIVE", "行政行为审查", "核验行政行为及送达日期", "确认行为内容、法律依据、送达方式和救济告知", 1, 1);
        add(templates, "ADMINISTRATIVE", "复议或起诉", "确认复议前置、管辖和期限", "形成复议申请或起诉材料清单", 1, 1);
        add(templates, "ADMINISTRATIVE", "举证", "跟踪行政机关举证期限", "维护证据目录并审查行政行为事实和程序依据", 1, 1);
        add(templates, "ADMINISTRATIVE", "庭审", "完成行政庭前检查", "准备争议焦点、质证意见和代理提纲", 1, 1);
        add(templates, "ADMINISTRATIVE", "裁判", "登记行政裁判结果", "记录送达日期、履行事项和上诉期限", 1, 1);
        add(templates, "ADMINISTRATIVE", "后续程序", "确认行政案件后续救济", "评估履行、上诉、申诉或行政赔偿事项", 1, 2);
        add(templates, "ADMINISTRATIVE", "结案归档", "完成行政案件结案检查", "整理行政文书、证据、庭审及客户交付记录", 2, 5);

        add(templates, "NON_LITIGATION", "接洽利冲", "完成非诉项目主体和利冲检查", "核对委托方、目标公司、交易相对方及关联主体", 1, 0);
        add(templates, "NON_LITIGATION", "签约立项", "确认项目范围和交付计划", "登记交付物、时间表、责任分工和收费安排", 1, 2);
        add(templates, "NON_LITIGATION", "资料收集", "建立项目资料清单", "记录资料来源、版本、缺失项及保密要求", 1, 2);
        add(templates, "NON_LITIGATION", "调查核验", "完成重点事实和合规核验", "记录核验依据、异常事项和待确认问题", 1, 3);
        add(templates, "NON_LITIGATION", "起草或谈判", "形成成果文件工作稿", "记录主要条款、谈判变化和风险处理意见", 1, 3);
        add(templates, "NON_LITIGATION", "内部复核", "完成项目成果内部复核", "由承办人和复核人检查事实、法律及交付边界", 1, 2);
        add(templates, "NON_LITIGATION", "成果交付", "登记正式成果交付", "保存定稿、交付对象、时间和签收反馈", 1, 1);
        add(templates, "NON_LITIGATION", "整改跟踪", "跟踪客户反馈和整改事项", "记录反馈、补充工作及未解决风险", 2, 5);
        add(templates, "NON_LITIGATION", "项目归档", "完成非诉项目归档检查", "核对资料来源、版本、复核及交付记录", 2, 5);

        add(templates, "CONSULTANT", "顾问建档", "完成顾问单位和服务边界建档", "核对合同期限、联系人、服务范围和除外事项", 1, 0);
        add(templates, "CONSULTANT", "服务计划", "制定顾问服务计划", "登记重点需求、责任分工、报告周期和响应约定", 2, 3);
        add(templates, "CONSULTANT", "需求受理", "登记顾问单位服务需求", "记录提出人、事项、相对方、期限和所需材料", 1, 0);
        add(templates, "CONSULTANT", "分派办理", "完成顾问事项分派", "确认承办人、复核人、优先级和计划交付时间", 1, 0);
        add(templates, "CONSULTANT", "审核交付", "完成顾问成果审核与交付", "保存工作稿、复核意见、定稿和交付记录", 1, 1);
        add(templates, "CONSULTANT", "定期报告", "形成顾问服务阶段报告", "汇总咨询、合同、函件、争议和风险建议", 2, 5);
        add(templates, "CONSULTANT", "续签评估", "完成顾问合同续签评估", "核对工作量、服务成效、未结事项和续签条件", 2, 7);
        add(templates, "CONSULTANT", "终止或归档", "完成顾问服务终止或归档", "确认未结事项移交、文件目录和客户签收", 2, 5);

        return templates;
    }

    private void add(List<StageTodoTemplate> templates, String caseType, String stageName,
                     String title, String description, int priority, int relativeDays) {
        templates.add(createTemplate(stageName, caseType, title, description,
                priority, relativeDays, templates.size() + 1));
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
