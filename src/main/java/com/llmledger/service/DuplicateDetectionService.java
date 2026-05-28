package com.llmledger.service;

import com.llmledger.entity.Transaction;
import com.llmledger.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DuplicateDetectionService {

    private final TransactionRepository transactionRepository;

    // 相似度阈值
    private static final double SIMILARITY_THRESHOLD = 0.6;

    // 检测时间范围（天）
    private static final int DETECTION_RANGE_DAYS = 30;

    public DuplicateDetectionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * 检测重复账单
     * 
     * @param userId      用户ID
     * @param amount      金额
     * @param description 描述
     * @return 重复账单列表
     */
    public List<Transaction> detectDuplicates(Long userId, Double amount, String description) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(DETECTION_RANGE_DAYS);

        // 获取最近30天的所有交易
        List<Transaction> recentTransactions = transactionRepository.findByOwnerIdOrderByDateDesc(userId)
                .stream()
                .filter(tx -> tx.getDate().isAfter(startDate))
                .collect(Collectors.toList());

        // 检测重复
        return recentTransactions.stream()
                .filter(tx -> isDuplicate(tx, amount, description))
                .collect(Collectors.toList());
    }

    /**
     * 检测重复账单（包含家庭账本）
     * 
     * @param userId      用户ID
     * @param amount      金额
     * @param description 描述
     * @param familyId    家庭账本ID（可选）
     * @return 重复账单列表
     */
    public List<Transaction> detectDuplicatesWithFamily(Long userId, Double amount, String description, Long familyId) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(DETECTION_RANGE_DAYS);

        List<Transaction> transactions;

        if (familyId != null) {
            // 家庭账本：只检测同一家庭内的交易
            transactions = transactionRepository.findByFamilyIdOrderByDateDesc(familyId)
                    .stream()
                    .filter(tx -> tx.getDate().isAfter(startDate))
                    .collect(Collectors.toList());
        } else {
            // 个人账本
            transactions = transactionRepository.findByOwnerIdOrderByDateDesc(userId)
                    .stream()
                    .filter(tx -> tx.getDate().isAfter(startDate) && tx.getFamilyId() == null)
                    .collect(Collectors.toList());
        }

        return transactions.stream()
                .filter(tx -> isDuplicate(tx, amount, description))
                .collect(Collectors.toList());
    }

    /**
     * 判断是否是重复账单
     */
    private boolean isDuplicate(Transaction transaction, Double amount, String description) {
        // 1. 金额相似度检查（允许0.01的误差）
        boolean amountMatch = Math.abs(transaction.getAmount() - amount) < 0.01;

        // 2. 描述相似度检查
        boolean descriptionMatch = calculateSimilarity(
                transaction.getDescription(),
                description) >= SIMILARITY_THRESHOLD;

        return amountMatch && descriptionMatch;
    }

    /**
     * 计算两个字符串的相似度（简单的余弦相似度）
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        str1 = str1.toLowerCase().trim();
        str2 = str2.toLowerCase().trim();

        // 完全相同
        if (str1.equals(str2)) {
            return 1.0;
        }

        // 完全不包含
        if (!str1.contains(str2) && !str2.contains(str1)) {
            // 计算编辑距离相似度
            double levenshteinSimilarity = 1.0 - (double) levenshteinDistance(str1, str2) /
                    Math.max(str1.length(), str2.length());
            return Math.max(0, levenshteinSimilarity);
        }

        // 一个包含另一个
        if (str1.contains(str2)) {
            return (double) str2.length() / str1.length();
        } else {
            return (double) str1.length() / str2.length();
        }
    }

    /**
     * 计算编辑距离（Levenshtein Distance）
     */
    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                            Math.min(dp[i - 1][j], dp[i][j - 1]),
                            dp[i - 1][j - 1]);
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }

    /**
     * 获取检测结果的消息
     */
    public String getDetectionMessage(List<Transaction> duplicates) {
        if (duplicates.isEmpty()) {
            return null;
        }

        if (duplicates.size() == 1) {
            Transaction dup = duplicates.get(0);
            return String.format("检测到1笔可能重复的账单：%s ¥%.2f (记录于 %s)",
                    dup.getDescription(), dup.getAmount(), dup.getDate());
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format("检测到%d笔可能重复的账单：\n", duplicates.size()));
        for (Transaction dup : duplicates) {
            message.append(String.format("- %s ¥%.2f (记录于 %s)\n",
                    dup.getDescription(), dup.getAmount(), dup.getDate()));
        }
        message.append("请确认是否继续添加？");

        return message.toString();
    }
}
