package com.llmledger.controller;

import com.llmledger.dto.*;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import com.llmledger.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class AiAndAnalyticsController {

    private final TransactionService transactionService;
    private final SubscriptionService subscriptionService;
    private final AnalyticsService analyticsService;
    private final OllamaService ollamaService;
    private final DtoMapper mapper;

    public AiAndAnalyticsController(TransactionService transactionService,
                                    SubscriptionService subscriptionService,
                                    AnalyticsService analyticsService,
                                    OllamaService ollamaService,
                                    DtoMapper mapper) {
        this.transactionService = transactionService;
        this.subscriptionService = subscriptionService;
        this.analyticsService = analyticsService;
        this.ollamaService = ollamaService;
        this.mapper = mapper;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<TransactionCreateRequest> parsed = ollamaService.parseTransactions(request.getText(), request.getModel());
        if (parsed.isEmpty()) {
            return new ChatResponse("抱歉，我没能从你的话中提取出账单信息，能换个说法吗？", List.of());
        }

        List<TransactionCreateRequest> saved = new ArrayList<>();
        for (TransactionCreateRequest item : parsed) {
            try {
                if (item.getAmount() == null || item.getDescription() == null || item.getType() == null || item.getCategory() == null) {
                    continue;
                }
                item.setAmount(Math.abs(item.getAmount()));
                transactionService.create(item, user);
                saved.add(item);
            } catch (Exception ignored) {
            }
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
                                  Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        List<Transaction> expenses;
        List<Transaction> incomes;

        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            expenses = transactionService.findByTypeAndDateRange(user, "expense", start, end);
            incomes = transactionService.findByTypeAndDateRange(user, "income", start, end);
        } else {
            expenses = transactionService.findByType(user, "expense");
            incomes = transactionService.findByType(user, "income");
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

        String summary = "时间段: " + periodLabel + "。总收入: ¥" + totalIncome + ", 总支出: ¥" + totalExpense + "。各项支出详情: " + categoryExpenses + "。";
        String promptPersona;
        if ("roast".equals(persona)) {
            promptPersona = "毒舌老妈，说话一针见血，毫不留情地批评乱花钱的行为";
        } else if ("pro".equals(persona)) {
            promptPersona = "专业理财师，用词严谨，提供专业的财务分析和投资建议";
        } else {
            promptPersona = "温柔鼓励的朋友，总是看到积极的一面，给予鼓励和温和的建议";
        }

        String fullPrompt = "你是一个" + promptPersona + "的财务顾问。根据以下用户在【" + periodLabel + "】期间的账单数据：" + summary + "，写一份财务诊断报告。报告标题中请明确标注时间段【" + periodLabel + "】。";
        String report = ollamaService.generateAdvice(fullPrompt, null);
        return new ReportResponse(report);
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
            prompt = "你是一个" + promptPersona + "。用户向你提出了一个关于理财、财务管理或者记账的问题。请用符合你设定的口吻，给出专业、易懂、字数在 150 字左右的回答。用户的问题是：\"" + request.getQuestion() + "\"";
            String answer = ollamaService.generateAdvice(prompt, null);
            return new AnswerResponse(answer, false);
        }
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return analyticsService.dashboard(user);
    }

    @GetMapping("/api/assets")
    public AssetsResponse assets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return analyticsService.assets(user);
    }
}
