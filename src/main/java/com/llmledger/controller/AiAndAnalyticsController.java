package com.llmledger.controller;

import com.llmledger.dto.*;
import com.llmledger.entity.FamilyMember;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import com.llmledger.repository.FamilyMemberRepository;
import com.llmledger.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class AiAndAnalyticsController {

    private final TransactionService transactionService;
    private final SubscriptionService subscriptionService;
    private final AnalyticsService analyticsService;
    private final OllamaService ollamaService;
    private final DtoMapper mapper;
    private final DuplicateDetectionService duplicateDetectionService;
    private final FamilyMemberRepository familyMemberRepository;

    public AiAndAnalyticsController(TransactionService transactionService,
            SubscriptionService subscriptionService,
            AnalyticsService analyticsService,
            OllamaService ollamaService,
            DtoMapper mapper,
            DuplicateDetectionService duplicateDetectionService,
            FamilyMemberRepository familyMemberRepository) {
        this.transactionService = transactionService;
        this.subscriptionService = subscriptionService;
        this.analyticsService = analyticsService;
        this.ollamaService = ollamaService;
        this.mapper = mapper;
        this.duplicateDetectionService = duplicateDetectionService;
        this.familyMemberRepository = familyMemberRepository;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<TransactionCreateRequest> parsed = ollamaService.parseTransactions(request.getText(), request.getModel());
        if (parsed.isEmpty()) {
            return new ChatResponse("抱歉，我没能从你的话中提取出账单信息，能换个说法吗？", List.of());
        }

        List<TransactionCreateRequest> saved = new ArrayList<>();
        List<Transaction> allDuplicates = new ArrayList<>();

        for (TransactionCreateRequest item : parsed) {
            try {
                if (item.getAmount() == null || item.getDescription() == null || item.getType() == null
                        || item.getCategory() == null) {
                    continue;
                }

                double amount = Math.abs(item.getAmount());
                item.setAmount(amount);

                // 设置familyId
                item.setFamilyId(request.getFamilyId());

                List<Transaction> duplicates = duplicateDetectionService.detectDuplicatesWithFamily(
                        user.getId(), amount, item.getDescription(), request.getFamilyId());

                if (!duplicates.isEmpty()) {
                    allDuplicates.addAll(duplicates);
                    continue;
                }

                transactionService.create(item, user);
                saved.add(item);
            } catch (Exception ignored) {
            }
        }

        if (!allDuplicates.isEmpty()) {
            List<TransactionResponse> duplicateResponses = allDuplicates.stream()
                    .map(mapper::toTransactionResponse)
                    .toList();
            String duplicateMsg = duplicateDetectionService.getDetectionMessage(allDuplicates);

            StringBuilder msg = new StringBuilder();
            if (saved.size() > 0) {
                msg.append("成功记录 ").append(saved.size()).append(" 笔账单！\n");
                for (TransactionCreateRequest tx : saved) {
                    String action = "expense".equals(tx.getType()) ? "支出" : "收入";
                    msg.append("- ").append(tx.getDescription()).append(": ").append(action)
                            .append(" ¥").append(tx.getAmount()).append(" (").append(tx.getCategory()).append(")\n");
                }
                msg.append("\n");
            }
            msg.append("检测到重复账单，以下记录已跳过：");

            return new ChatResponse(msg.toString(), saved, true, duplicateMsg, duplicateResponses);
        }

        StringBuilder msg = new StringBuilder("我帮你记好了 " + saved.size() + " 笔账单！\n");
        for (TransactionCreateRequest tx : saved) {
            String action = "expense".equals(tx.getType()) ? "支出" : "收入";
            msg.append("- ").append(tx.getDescription()).append(": ").append(action)
                    .append(" ¥").append(tx.getAmount()).append(" (").append(tx.getCategory()).append(")\n");
        }
        return new ChatResponse(msg.toString(), saved);
    }

    @GetMapping("/api/advisor")
    public ReportResponse advisor(@RequestParam(defaultValue = "roast") String persona,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long familyId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            List<Transaction> expenses;
            List<Transaction> incomes;
            List<Transaction> allTransactions;

            String scopeLabel;

            if (familyId != null) {
                // 家庭账本模式：获取所有家庭成员的账单
                allTransactions = transactionService.allByFamily(familyId, user);
                expenses = allTransactions.stream()
                        .filter(tx -> "expense".equals(tx.getType()))
                        .collect(java.util.stream.Collectors.toList());
                incomes = allTransactions.stream()
                        .filter(tx -> "income".equals(tx.getType()))
                        .collect(java.util.stream.Collectors.toList());
                scopeLabel = "家庭账本";
            } else {
                // 个人账本模式
                allTransactions = new java.util.ArrayList<>();
                if (startDate != null && endDate != null) {
                    LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                    LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
                    expenses = transactionService.findByTypeAndDateRange(user, "expense", start, end);
                    incomes = transactionService.findByTypeAndDateRange(user, "income", start, end);
                    allTransactions.addAll(expenses);
                    allTransactions.addAll(incomes);
                } else {
                    expenses = transactionService.findByType(user, "expense");
                    incomes = transactionService.findByType(user, "income");
                    allTransactions.addAll(expenses);
                    allTransactions.addAll(incomes);
                }
                scopeLabel = "个人账本";
            }

            double totalExpense = expenses.stream().mapToDouble(Transaction::getAmount).sum();
            double totalIncome = incomes.stream().mapToDouble(Transaction::getAmount).sum();

            java.util.Map<String, Double> categoryExpenses = new java.util.HashMap<>();
            for (Transaction tx : expenses) {
                categoryExpenses.merge(tx.getCategory(), tx.getAmount(), Double::sum);
            }

            String periodLabel;
            if (startDate != null && endDate != null) {
                periodLabel = startDate + " 至 " + endDate;
            } else {
                periodLabel = "全部时间";
            }

            // 按家庭成员分组统计（仅家庭账本模式）
            StringBuilder memberAnalysis = new StringBuilder();
            if (familyId != null && !allTransactions.isEmpty()) {
                java.util.Map<String, java.util.List<Transaction>> transactionsByMember = allTransactions.stream()
                        .collect(java.util.stream.Collectors.groupingBy(tx -> tx.getOwner().getUsername()));

                // 获取家庭成员角色映射
                Map<String, String> memberRoles = new HashMap<>();
                List<FamilyMember> familyMembers = familyMemberRepository.findByFamilyId(familyId);
                for (FamilyMember member : familyMembers) {
                    String roleName = switch (member.getRole()) {
                        case OWNER -> "家主";
                        case ADMIN -> "管理员";
                        case MEMBER -> "成员";
                        case VIEWER -> "访客";
                    };
                    memberRoles.put(member.getUser().getUsername(), roleName);
                }

                memberAnalysis.append("家庭成员明细：\n");
                for (java.util.Map.Entry<String, java.util.List<Transaction>> entry : transactionsByMember.entrySet()) {
                    String memberName = entry.getKey();
                    java.util.List<Transaction> memberTx = entry.getValue();

                    double memberIncome = memberTx.stream()
                            .filter(t -> "income".equals(t.getType()))
                            .mapToDouble(Transaction::getAmount).sum();
                    double memberExpense = memberTx.stream()
                            .filter(t -> "expense".equals(t.getType()))
                            .mapToDouble(Transaction::getAmount).sum();

                    String role = memberRoles.getOrDefault(memberName, "成员");
                    memberAnalysis.append("- ").append(memberName).append("(").append(role).append(")");
                    memberAnalysis.append("：收入 ¥").append(String.format("%.2f", memberIncome));
                    memberAnalysis.append("，支出 ¥").append(String.format("%.2f", memberExpense));

                    if (memberIncome > 0) {
                        double savingsRate = ((memberIncome - memberExpense) / memberIncome) * 100;
                        memberAnalysis.append("，储蓄率 ").append(String.format("%.1f", savingsRate)).append("%");
                    }
                    memberAnalysis.append("\n");
                }
            }

            String summary = "账本范围: " + scopeLabel + "。时间段: " + periodLabel + "。";
            summary += "总收入: ¥" + String.format("%.2f", totalIncome) + ", 总支出: ¥" + String.format("%.2f", totalExpense)
                    + "。";
            summary += "结余: ¥" + String.format("%.2f", totalIncome - totalExpense) + "。";
            summary += "各项支出详情: " + categoryExpenses + "。";
            if (memberAnalysis.length() > 0) {
                summary += "\n" + memberAnalysis;
            }

            String promptPersona;
            if ("roast".equals(persona)) {
                promptPersona = "毒舌老妈，说话一针见血，毫不留情地批评乱花钱的行为";
            } else if ("pro".equals(persona)) {
                promptPersona = "专业理财师，用词严谨，提供专业的财务分析和投资建议";
            } else {
                promptPersona = "温柔鼓励的朋友，总是看到积极的一面，给予鼓励和温和的建议";
            }

            String fullPrompt = "根据以下";
            if (familyId != null) {
                fullPrompt += "家庭";
            }
            fullPrompt += "财务数据【" + scopeLabel + "】【" + periodLabel + "】：\n" + summary + "\n\n";
            fullPrompt += "财务诊断报告\n\n";
            fullPrompt += "## 一、财务概览\n";
            fullPrompt += "- 总收入：\n";
            fullPrompt += "- 总支出：\n";
            fullPrompt += "- 结余：\n\n";
            if (familyId != null) {
                fullPrompt += "## 二、家庭成员明细\n";
                fullPrompt += "成员|角色|收入|支出|储蓄率\n";
                fullPrompt += "----|----|----|----|----\n\n";
                fullPrompt += "## 三、成员对比分析\n\n";
            }
            fullPrompt += "## 四、支出分类分析\n";
            fullPrompt += "类别|金额|占比\n";
            fullPrompt += "----|----|----\n\n";
            fullPrompt += "## 五、理财建议\n";
            fullPrompt += "- \n\n";
            fullPrompt += "只填数据和分析，不要任何格式说明、指导文字或解释性内容。";

            String report = ollamaService.generateAdvice(fullPrompt, null);
            return new ReportResponse(report);
        } catch (Exception e) {
            log.error("生成财务诊断报告失败", e);
            return new ReportResponse("生成报告失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/advisor/chat")
    public AnswerResponse advisorChat(@RequestBody AdvisorChatRequest request, Authentication authentication) {
        String promptPersona;
        if ("roast".equals(request.getPersona())) {
            promptPersona = "毒舌老妈，说话一针见血，毫不留情";
        } else if ("pro".equals(request.getPersona())) {
            promptPersona = "专业理财师，用词严谨，提供专业的财务分析和投资建议";
        } else {
            promptPersona = "温柔鼓励的朋友，总是看到积极的一面，给予鼓励和温和的建议";
        }

        String prompt;
        String currentReport = request.getCurrentReport();

        if (currentReport != null && !currentReport.isBlank()) {
            // 局部修改模式：用户对报告某部分不满意，基于当前报告内容进行针对性修改
            prompt = "你是一个" + promptPersona + "的财务顾问。以下是之前为用户生成的财务诊断报告全文：\n\n" + currentReport
                    + "\n\n用户对报告中的某部分不满意，提出了修改要求：\"" + request.getQuestion() + "\""
                    + "\n\n请根据用户的修改要求，只修改报告中相关的部分，保留其余部分不变，输出修改后的完整报告。不要从头重新生成，而是在原有报告基础上进行局部调整。";
            String modifiedReport = ollamaService.generateAdvice(prompt, null);
            return new AnswerResponse(modifiedReport, true);
        } else {
            // 独立问答模式：用户提出一般性理财问题
            prompt = "你是一个" + promptPersona + "。用户向你提出了一个关于理财、财务管理或者记账的问题。请用符合你设定的口吻，给出专业、易懂、字数在 150 字左右的回答。用户的问题是：\""
                    + request.getQuestion() + "\"";
            String answer = ollamaService.generateAdvice(prompt, null);
            return new AnswerResponse(answer, false);
        }
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard(@RequestParam(required = false) Long familyId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return analyticsService.dashboard(user, familyId);
    }

    @GetMapping("/api/assets")
    public AssetsResponse assets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return analyticsService.assets(user);
    }
}
