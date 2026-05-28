package com.llmledger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmledger.dto.TransactionCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);
    private final RestTemplate restTemplate;

    public OllamaService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30秒连接超时
        factory.setReadTimeout(120000); // 120秒读取超时
        this.restTemplate = new RestTemplate(factory);
    }

    @Value("${app.ollama.api-url}")
    private String ollamaApiUrl;

    @Value("${app.ollama.default-model}")
    private String defaultModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TransactionCreateRequest> parseTransactions(String text, String modelName) {
        String model = modelName == null || modelName.isBlank() ? defaultModel : modelName;
        log.info("使用模型 {} 解析账单文本: {}", model, text);

        // 首先尝试正则表达式提取，这是最可靠的方法
        List<TransactionCreateRequest> regexResult = extractTransactionsByRegex(text);
        if (!regexResult.isEmpty()) {
            log.info("正则表达式成功提取出 {} 条账单记录", regexResult.size());
            return regexResult;
        }

        // 如果正则失败，再尝试 AI 提取
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("stream", false);
            body.put("format", "json");

            if (model.toLowerCase().contains("qwen3")) {
                body.put("think", false);
            }

            String prompt = buildEnhancedPrompt(text, model);
            body.put("prompt", prompt);

            log.debug("发送请求到 Ollama: {}", body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String res = restTemplate.postForObject(ollamaApiUrl, entity, String.class);

            log.debug("Ollama 原始响应: {}", res);

            JsonNode root = objectMapper.readTree(res);
            String raw = root.path("response").asText("").trim();

            log.debug("提取的响应内容: {}", raw);

            // 清理可能残留的 markdown 代码块
            if (raw.startsWith("```json"))
                raw = raw.substring(7).trim();
            if (raw.startsWith("```"))
                raw = raw.substring(3).trim();
            if (raw.endsWith("```"))
                raw = raw.substring(0, raw.length() - 3).trim();

            // 尝试多次解析
            List<TransactionCreateRequest> result = tryParseJson(raw);
            if (!result.isEmpty() && isValidResult(result)) {
                log.info("AI 成功解析出 {} 条账单记录", result.size());
                return result;
            }

            log.warn("无法从响应中解析出有效的账单信息");
            return List.of();
        } catch (Exception e) {
            log.error("调用 Ollama 失败", e);
            return List.of();
        }
    }

    public String generateAdvice(String prompt, String modelName) {
        try {
            String model = modelName == null || modelName.isBlank() ? defaultModel : modelName;

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);

            if (model.toLowerCase().contains("qwen3")) {
                body.put("think", false);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String res = restTemplate.postForObject(ollamaApiUrl, entity, String.class);

            JsonNode root = objectMapper.readTree(res);
            String response = root.path("response").asText("").trim();

            if (response == null || response.isBlank()) {
                return "暂时无法生成结果，请稍后重试。";
            }

            return response.trim();
        } catch (Exception e) {
            log.error("生成建议失败", e);
            return "诊断失败，请检查本地 Ollama 服务是否启动。错误信息：" + e.getMessage();
        }
    }

    /**
     * 使用正则表达式提取账单信息 - 更可靠的方法
     */
    private List<TransactionCreateRequest> extractTransactionsByRegex(String text) {
        List<TransactionCreateRequest> results = new java.util.ArrayList<>();

        // 首先尝试提取工资单信息
        List<TransactionCreateRequest> salaryResults = extractSalaryInfo(text);
        if (!salaryResults.isEmpty()) {
            return salaryResults;
        }

        // 匹配模式：商品名称 + 金额（支持多种格式）
        // 示例：进口零食大礼包 85.50元，海飞丝洗发水500ml 42.00元，环保购物袋 0.50元
        Pattern pattern = Pattern.compile(
                "([\\u4e00-\\u9fa5a-zA-Z0-9\\s]+?)\\s*([\\d]+(?:\\.[\\d]{1,2})?)\\s*元",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String description = matcher.group(1).trim();
            String amountStr = matcher.group(2).trim();

            // 过滤掉明显不是商品的条目（如总金额、数量等）
            if (description.length() < 2)
                continue;
            if (description.contains("总金额") || description.contains("合计") ||
                    description.contains("应付") || description.contains("总计"))
                continue;

            try {
                double amount = Double.parseDouble(amountStr);

                // 过滤掉不合理的金额（如500ml被识别为500元）
                if (amount > 0 && amount < 100000) {
                    TransactionCreateRequest tx = new TransactionCreateRequest();
                    tx.setAmount(amount);
                    tx.setDescription(cleanDescription(description));

                    // 判断是收入还是支出
                    String txType = determineTransactionType(description);
                    tx.setType(txType);

                    // 推断分类
                    String category = guessCategory(description);
                    tx.setCategory(category);

                    // 根据描述和分类推断账本
                    String ledger = guessLedgerByDescription(description);
                    if ("日常账本".equals(ledger)) {
                        ledger = guessLedger(category);
                    }
                    tx.setLedger(ledger);

                    results.add(tx);
                }
            } catch (NumberFormatException e) {
                log.debug("金额解析失败: {}", amountStr);
            }
        }

        return results;
    }

    /**
     * 提取工资单信息
     */
    private List<TransactionCreateRequest> extractSalaryInfo(String text) {
        List<TransactionCreateRequest> results = new java.util.ArrayList<>();

        // 检测是否包含工资单相关内容
        if (!text.contains("工资") && !text.contains("实发") && !text.contains("应发")) {
            return results;
        }

        // 提取实发工资（收入）
        Pattern salaryPattern = Pattern.compile("(实发工资|实发)\\s*[：:]?\\s*([\\d]+(?:\\.[\\d]{1,2})?)");
        Matcher salaryMatcher = salaryPattern.matcher(text);
        if (salaryMatcher.find()) {
            try {
                double amount = Double.parseDouble(salaryMatcher.group(2).trim());
                if (amount > 0) {
                    TransactionCreateRequest tx = new TransactionCreateRequest();
                    tx.setAmount(amount);
                    tx.setDescription("工资收入");
                    tx.setType("income");
                    tx.setCategory("工资收入");
                    results.add(tx);
                }
            } catch (NumberFormatException e) {
                log.debug("工资解析失败");
            }
        }

        // 提取应发工资（可选）
        Pattern grossPattern = Pattern.compile("(应发工资|应发)\\s*[：:]?\\s*([\\d]+(?:\\.[\\d]{1,2})?)");
        Matcher grossMatcher = grossPattern.matcher(text);
        if (grossMatcher.find()) {
            try {
                double amount = Double.parseDouble(grossMatcher.group(2).trim());
                if (amount > 0) {
                    // 只有当实发工资没提取到或者应发工资更大时才添加
                    boolean hasSalary = results.stream().anyMatch(t -> t.getDescription().equals("工资收入"));
                    if (!hasSalary || amount > results.get(0).getAmount()) {
                        TransactionCreateRequest tx = new TransactionCreateRequest();
                        tx.setAmount(amount);
                        tx.setDescription("应发工资");
                        tx.setType("income");
                        tx.setCategory("工资收入");
                        // 如果已有实发工资，替换它
                        if (!results.isEmpty()) {
                            results.set(0, tx);
                        } else {
                            results.add(tx);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                log.debug("应发工资解析失败");
            }
        }

        // 提取五险一金扣款（支出）
        Pattern insurancePattern = Pattern.compile("(五险一金|五险|社保|公积金|扣)\\s*[：:]?\\s*([\\d]+(?:\\.[\\d]{1,2})?)");
        Matcher insuranceMatcher = insurancePattern.matcher(text);
        if (insuranceMatcher.find()) {
            try {
                double amount = Double.parseDouble(insuranceMatcher.group(2).trim());
                if (amount > 0) {
                    TransactionCreateRequest tx = new TransactionCreateRequest();
                    tx.setAmount(amount);
                    tx.setDescription("五险一金");
                    tx.setType("expense");
                    tx.setCategory("其他");
                    results.add(tx);
                }
            } catch (NumberFormatException e) {
                log.debug("五险一金解析失败");
            }
        }

        // 提取津贴（收入）
        Pattern allowancePattern = Pattern.compile("(岗位津贴|通讯津贴|交通津贴|津贴)\\s*[：:]?\\s*([\\d]+(?:\\.[\\d]{1,2})?)");
        Matcher allowanceMatcher = allowancePattern.matcher(text);
        while (allowanceMatcher.find()) {
            try {
                double amount = Double.parseDouble(allowanceMatcher.group(2).trim());
                if (amount > 0 && amount < 10000) {
                    TransactionCreateRequest tx = new TransactionCreateRequest();
                    tx.setAmount(amount);
                    tx.setDescription(allowanceMatcher.group(1));
                    tx.setType("income");
                    tx.setCategory("工资收入");
                    results.add(tx);
                }
            } catch (NumberFormatException e) {
                log.debug("津贴解析失败");
            }
        }

        return results;
    }

    /**
     * 清理描述文本
     */
    private String cleanDescription(String description) {
        // 移除数量单位如 500ml, 2kg 等
        return description.replaceAll("\\s*[\\d]+\\s*ml|\\s*[\\d]+\\s*kg|\\s*[\\d]+\\s*g|\\s*[\\d]+\\s*l", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 根据描述判断交易类型（收入或支出）
     */
    private String determineTransactionType(String description) {
        description = description.toLowerCase();

        // 收入关键词
        if (description.contains("赚") || description.contains("收入") ||
                description.contains("工资") || description.contains("薪水") ||
                description.contains("薪酬") || description.contains("奖金") ||
                description.contains("提成") || description.contains("佣金") ||
                description.contains("红包") || description.contains("退款") ||
                description.contains("转账收入") || description.contains("收到")) {
            return "income";
        }

        // 默认是支出
        return "expense";
    }

    /**
     * 根据描述猜测分类
     */
    private String guessCategory(String description) {
        description = description.toLowerCase();

        if (description.contains("工资") || description.contains("薪水") ||
                description.contains("薪酬") || description.contains("实发") ||
                description.contains("应发") || description.contains("工资单") ||
                description.contains("赚") || description.contains("收入")) {
            return "工资收入";
        }
        if (description.contains("奖金") || description.contains("绩效") ||
                description.contains("提成") || description.contains("年终奖")) {
            return "奖金收入";
        }
        if (description.contains("零食") || description.contains("食品") ||
                description.contains("饮料") || description.contains("水果") ||
                description.contains("面包") || description.contains("牛奶") ||
                description.contains("早餐") || description.contains("午餐") ||
                description.contains("晚餐") || description.contains("外卖") ||
                description.contains("零食") || description.contains("蛋糕")) {
            return "食品饮料";
        }
        if (description.contains("洗发") || description.contains("沐浴") ||
                description.contains("牙膏") || description.contains("毛巾") ||
                description.contains("纸巾") || description.contains("洗涤") ||
                description.contains("洗衣液") || description.contains("肥皂") ||
                description.contains("牙刷") || description.contains("梳子")) {
            return "日用品";
        }
        if (description.contains("服装") || description.contains("鞋") ||
                description.contains("衣服") || description.contains("裤子") ||
                description.contains("裙子") || description.contains("帽子") ||
                description.contains("袜子") || description.contains("内衣")) {
            return "服装";
        }
        if (description.contains("电器") || description.contains("手机") ||
                description.contains("电脑") || description.contains("数码") ||
                description.contains("平板") || description.contains("耳机") ||
                description.contains("电视") || description.contains("冰箱")) {
            return "数码电器";
        }
        if (description.contains("打车") || description.contains("滴滴") ||
                description.contains("出租") || description.contains("公交") ||
                description.contains("地铁") || description.contains("高铁") ||
                description.contains("火车") || description.contains("机票") ||
                description.contains("加油") || description.contains("停车")) {
            return "交通";
        }
        if (description.contains("餐厅") || description.contains("吃饭") ||
                description.contains("聚餐") || description.contains("火锅") ||
                description.contains("烧烤") || description.contains("咖啡") ||
                description.contains("奶茶") || description.contains("甜点")) {
            return "餐饮";
        }
        if (description.contains("电影") || description.contains("游戏") ||
                description.contains("KTV") || description.contains("唱歌") ||
                description.contains("酒吧") || description.contains("演出") ||
                description.contains("演唱会")) {
            return "娱乐";
        }
        if (description.contains("房租") || description.contains("水电") ||
                description.contains("物业") || description.contains("暖气") ||
                description.contains("维修") || description.contains("家具") ||
                description.contains("窗帘") || description.contains("装修") ||
                description.contains("建材") || description.contains("地板") ||
                description.contains("瓷砖") || description.contains("涂料")) {
            return "住房";
        }
        if (description.contains("医院") || description.contains("看病") ||
                description.contains("买药") || description.contains("体检") ||
                description.contains("挂号") || description.contains("手术") ||
                description.contains("牙科") || description.contains("中医")) {
            return "医疗";
        }
        if (description.contains("学费") || description.contains("书本") ||
                description.contains("培训") || description.contains("课程") ||
                description.contains("考试") || description.contains("学习") ||
                description.contains("辅导") || description.contains("教材")) {
            return "教育";
        }
        if (description.contains("旅游") || description.contains("旅行") ||
                description.contains("景点") || description.contains("门票") ||
                description.contains("酒店") || description.contains("机票") ||
                description.contains("游玩") || description.contains("度假") ||
                description.contains("三台山") || description.contains("景区")) {
            return "旅行";
        }
        if (description.contains("红包") || description.contains("转账") ||
                description.contains("退款") || description.contains("还款") ||
                description.contains("利息") || description.contains("理财")) {
            return "其他";
        }

        return "其他";
    }

    /**
     * 根据分类推断账本类型
     */
    private String guessLedger(String category) {
        category = category.toLowerCase();

        if (category.contains("住房") || category.contains("装修")) {
            return "装修账本";
        }
        if (category.contains("旅行") || category.contains("旅游")) {
            return "旅行账本";
        }

        return "日常账本";
    }

    /**
     * 根据描述推断账本类型（直接根据描述关键词）
     */
    private String guessLedgerByDescription(String description) {
        description = description.toLowerCase();

        if (description.contains("装修") || description.contains("家具") ||
                description.contains("窗帘") || description.contains("热水器") ||
                description.contains("建材") || description.contains("地板") ||
                description.contains("瓷砖") || description.contains("涂料") ||
                description.contains("维修") || description.contains("水电") ||
                description.contains("物业") || description.contains("房租")) {
            return "装修账本";
        }
        if (description.contains("旅行") || description.contains("旅游") ||
                description.contains("景点") || description.contains("门票") ||
                description.contains("酒店") || description.contains("游玩") ||
                description.contains("度假") || description.contains("三台山") ||
                description.contains("景区") || description.contains("机票")) {
            return "旅行账本";
        }

        return "日常账本";
    }

    /**
     * 构建增强版提示词 - 支持多种票据类型
     */
    private String buildEnhancedPrompt(String text, String model) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的智能记账助手，能够识别多种类型的账单。\n");
        prompt.append("请从以下文本中提取所有记账条目：\n");
        prompt.append("---\n");
        prompt.append(text);
        prompt.append("\n---\n");
        prompt.append("\n请识别以下类型的账单：\n");
        prompt.append("1. 超市小票：提取商品名称和价格（如：进口零食大礼包 85.50元）\n");
        prompt.append("2. 工资单：提取收入项（实发工资、岗位津贴等）和支出项（五险一金）\n");
        prompt.append("3. 餐饮消费：餐厅消费、外卖等\n");
        prompt.append("4. 交通出行：打车、公交、地铁等\n");
        prompt.append("5. 其他支出和收入\n");
        prompt.append("\n请按照以下严格JSON格式输出：\n");
        prompt.append("{\n");
        prompt.append("  \"transactions\": [\n");
        prompt.append(
                "    {\"amount\": 85.50, \"description\": \"进口零食大礼包\", \"type\": \"expense\", \"category\": \"食品饮料\", \"ledger\": \"日常账本\"},\n");
        prompt.append(
                "    {\"amount\": 3000.00, \"description\": \"实发工资\", \"type\": \"income\", \"category\": \"工资收入\", \"ledger\": \"日常账本\"},\n");
        prompt.append(
                "    {\"amount\": 888.00, \"description\": \"买热水器\", \"type\": \"expense\", \"category\": \"住房\", \"ledger\": \"装修账本\"},\n");
        prompt.append(
                "    {\"amount\": 200.00, \"description\": \"三台山门票\", \"type\": \"expense\", \"category\": \"旅行\", \"ledger\": \"旅行账本\"}\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("\n重要规则：\n");
        prompt.append("1. 只提取商品价格，不要提取总金额或合计金额\n");
        prompt.append("2. 不要将数量如500ml、2kg当作金额\n");
        prompt.append("3. 必须输出有效的JSON格式\n");
        prompt.append("4. 只输出JSON，不要有其他文字、解释或思考过程\n");
        prompt.append("5. 金额为0或负数的不要提取\n");
        prompt.append("6. 分类必须从以下选项中选择：工资收入、奖金收入、食品饮料、日用品、服装、数码电器、交通、餐饮、娱乐、住房、医疗、教育、旅行、其他\n");
        prompt.append("7. 账本必须从以下选项中选择：日常账本、装修账本、旅行账本\n");
        prompt.append("8. 装修相关（如家具、建材、热水器、窗帘、装修）选装修账本；旅行相关（如门票、酒店、机票、旅游）选旅行账本；其他选日常账本\n");

        return prompt.toString();
    }

    private List<TransactionCreateRequest> tryParseJson(String raw) {
        try {
            JsonNode parsed = objectMapper.readTree(raw);
            JsonNode txNode = parsed.path("transactions");
            if (!txNode.isArray()) {
                return List.of();
            }
            List<TransactionCreateRequest> results = objectMapper.convertValue(txNode,
                    new TypeReference<List<TransactionCreateRequest>>() {
                    });

            // 修正每个交易的分类和账本
            for (TransactionCreateRequest tx : results) {
                // 根据描述修正分类
                String correctedCategory = guessCategory(tx.getDescription());
                tx.setCategory(correctedCategory);

                // 根据描述修正交易类型
                String correctedType = determineTransactionType(tx.getDescription());
                tx.setType(correctedType);

                // 设置账本
                if (tx.getLedger() == null || tx.getLedger().isBlank()) {
                    String ledger = guessLedgerByDescription(tx.getDescription());
                    tx.setLedger(ledger);
                }
            }

            return results;
        } catch (Exception e) {
            log.debug("解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 验证结果是否有效
     */
    private boolean isValidResult(List<TransactionCreateRequest> results) {
        for (TransactionCreateRequest tx : results) {
            if (tx.getDescription() == null || tx.getDescription().equals("?") ||
                    tx.getDescription().isEmpty()) {
                return false;
            }
            if (tx.getCategory() == null || tx.getCategory().equals("?")) {
                return false;
            }
            if (tx.getAmount() <= 0) {
                return false;
            }
            // 设置默认账本
            if (tx.getLedger() == null || tx.getLedger().isBlank()) {
                tx.setLedger("日常账本");
            }
        }
        return true;
    }
}