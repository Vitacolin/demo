package com.llmledger.service;

import com.llmledger.dto.AssetsResponse;
import com.llmledger.dto.DashboardResponse;
import com.llmledger.entity.Transaction;
import com.llmledger.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final TransactionService transactionService;

    public AnalyticsService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public DashboardResponse dashboard(User user) {
        List<Transaction> expenses = transactionService.findByType(user, "expense");
        List<Transaction> incomes = transactionService.findByType(user, "income");

        double totalExpense = expenses.stream().mapToDouble(t -> Math.abs(t.getAmount())).sum();
        double totalIncome = incomes.stream().mapToDouble(t -> Math.abs(t.getAmount())).sum();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        nodes.add(Map.of("name", "我的钱包", "itemStyle", Map.of("color", "#e0e0e0")));

        Map<String, Double> incomeCategories = new HashMap<>();
        for (Transaction tx : incomes) {
            incomeCategories.merge(tx.getCategory(), Math.abs(tx.getAmount()), Double::sum);
        }
        for (Map.Entry<String, Double> entry : incomeCategories.entrySet()) {
            String cat = entry.getKey();
            double amount = entry.getValue();
            if (amount > 0) {
                if (nodes.stream().noneMatch(n -> cat.equals(n.get("name")))) {
                    nodes.add(Map.of("name", cat, "itemStyle", Map.of("color", "#4ade80")));
                }
                links.add(Map.of("source", cat, "target", "我的钱包", "value", amount));
            }
        }

        Map<String, Double> expenseCategories = new HashMap<>();
        for (Transaction tx : expenses) {
            expenseCategories.merge(tx.getCategory(), Math.abs(tx.getAmount()), Double::sum);
        }
        for (Map.Entry<String, Double> entry : expenseCategories.entrySet()) {
            String cat = entry.getKey();
            double amount = entry.getValue();
            if (amount > 0) {
                if (nodes.stream().noneMatch(n -> cat.equals(n.get("name")))) {
                    nodes.add(Map.of("name", cat, "itemStyle", Map.of("color", "#ff6b35")));
                }
                links.add(Map.of("source", "我的钱包", "target", cat, "value", amount));
            }
        }

        double balance = totalIncome - totalExpense;
        if (balance > 0) {
            nodes.add(Map.of("name", "结余(存款)", "itemStyle", Map.of("color", "#3b82f6")));
            links.add(Map.of("source", "我的钱包", "target", "结余(存款)", "value", balance));
        }

        if (totalIncome == 0 && totalExpense == 0) {
            nodes.add(Map.of("name", "工资收入", "itemStyle", Map.of("color", "#4ade80")));
            nodes.add(Map.of("name", "餐饮美食", "itemStyle", Map.of("color", "#ff6b35")));
            links.add(Map.of("source", "工资收入", "target", "我的钱包", "value", 0));
        }

        Map<String, Object> sankey = Map.of("nodes", nodes, "links", links);
        int healthScore = totalIncome > totalExpense ? 85 : 50;
        return new DashboardResponse(totalExpense, totalIncome, balance, healthScore, sankey);
    }

    public AssetsResponse assets(User user) {
        List<Transaction> incomes = transactionService.findByType(user, "income");
        List<Transaction> expenses = transactionService.findByType(user, "expense");

        double income = incomes.stream().mapToDouble(Transaction::getAmount).sum();
        double expense = expenses.stream().mapToDouble(Transaction::getAmount).sum();
        double netWorth = income - expense;

        List<Map<String, Object>> assets = List.of(Map.of(
                "name", "系统计算资产",
                "amount", netWorth > 0 ? netWorth : 0,
                "icon", "💰"
        ));

        List<Map<String, Object>> debts = List.of(Map.of(
                "name", "系统计算负债",
                "amount", netWorth < 0 ? Math.abs(netWorth) : 0,
                "icon", "💳"
        ));

        return new AssetsResponse(netWorth, assets, debts);
    }
}
