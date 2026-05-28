/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306_2
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : llm_ledger

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 23/05/2026 05:24:44
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for families
-- ----------------------------
DROP TABLE IF EXISTS `families`;
CREATE TABLE `families`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `creator_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `creator_id`(`creator_id` ASC) USING BTREE,
  CONSTRAINT `families_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of families
-- ----------------------------
INSERT INTO `families` VALUES (1, '张氏家庭', 6, '2026-05-23 05:15:58');

-- ----------------------------
-- Table structure for family_invitations
-- ----------------------------
DROP TABLE IF EXISTS `family_invitations`;
CREATE TABLE `family_invitations`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL,
  `invite_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `expires_at` datetime NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `invite_code`(`invite_code` ASC) USING BTREE,
  INDEX `family_id`(`family_id` ASC) USING BTREE,
  INDEX `created_by`(`created_by` ASC) USING BTREE,
  CONSTRAINT `family_invitations_ibfk_1` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `family_invitations_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of family_invitations
-- ----------------------------

-- ----------------------------
-- Table structure for family_members
-- ----------------------------
DROP TABLE IF EXISTS `family_members`;
CREATE TABLE `family_members`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEMBER',
  `joined_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_family_user`(`family_id` ASC, `user_id` ASC) USING BTREE,
  UNIQUE INDEX `UKgkutqi0x1x6tk3umvb3je4ohv`(`family_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `family_members_ibfk_1` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `family_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of family_members
-- ----------------------------
INSERT INTO `family_members` VALUES (1, 1, 6, 'OWNER', '2026-05-23 05:15:58');
INSERT INTO `family_members` VALUES (2, 1, 1, 'ADMIN', '2026-05-23 05:15:58');
INSERT INTO `family_members` VALUES (3, 1, 2, 'MEMBER', '2026-05-23 05:15:58');
INSERT INTO `family_members` VALUES (4, 1, 3, 'MEMBER', '2026-05-23 05:15:58');
INSERT INTO `family_members` VALUES (5, 1, 4, 'MEMBER', '2026-05-23 05:15:58');
INSERT INTO `family_members` VALUES (6, 1, 5, 'MEMBER', '2026-05-23 05:15:58');

-- ----------------------------
-- Table structure for ledgers
-- ----------------------------
DROP TABLE IF EXISTS `ledgers`;
CREATE TABLE `ledgers`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_id` bigint NULL DEFAULT NULL,
  `is_shared` tinyint(1) NULL DEFAULT 0,
  `family_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `owner_id`(`owner_id` ASC) USING BTREE,
  INDEX `fk_ledger_family`(`family_id` ASC) USING BTREE,
  CONSTRAINT `fk_ledger_family` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ledgers_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ledgers
-- ----------------------------
INSERT INTO `ledgers` VALUES (1, '日常账本', 1, 0, NULL);
INSERT INTO `ledgers` VALUES (2, '旅行账本', 1, 0, NULL);
INSERT INTO `ledgers` VALUES (3, '投资账本', 2, 0, NULL);

-- ----------------------------
-- Table structure for subscriptions
-- ----------------------------
DROP TABLE IF EXISTS `subscriptions`;
CREATE TABLE `subscriptions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` double NOT NULL,
  `cycle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `next_billing_date` datetime NOT NULL,
  `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `owner_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `owner_id`(`owner_id` ASC) USING BTREE,
  CONSTRAINT `subscriptions_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of subscriptions
-- ----------------------------
INSERT INTO `subscriptions` VALUES (1, 'Netflix会员', 15.99, 'monthly', '2026-05-01 00:00:00', '娱乐', 1, 1);
INSERT INTO `subscriptions` VALUES (2, 'Spotify会员', 9.99, 'monthly', '2026-05-01 00:00:00', '娱乐', 1, 1);
INSERT INTO `subscriptions` VALUES (3, '云服务器', 99, 'monthly', '2026-05-01 00:00:00', '工作', 1, 1);
INSERT INTO `subscriptions` VALUES (4, '健身房', 299, 'monthly', '2026-05-15 00:00:00', '健康', 1, 1);
INSERT INTO `subscriptions` VALUES (5, '域名续费', 68, 'yearly', '2026-12-01 00:00:00', '工作', 1, 1);
INSERT INTO `subscriptions` VALUES (6, 'Apple Music', 10, 'monthly', '2026-05-01 00:00:00', '娱乐', 1, 2);
INSERT INTO `subscriptions` VALUES (7, '百度网盘', 30, 'monthly', '2026-05-10 00:00:00', '工具', 1, 2);
INSERT INTO `subscriptions` VALUES (8, '微信读书', 19, 'monthly', '2026-05-01 00:00:00', '学习', 1, 3);

-- ----------------------------
-- Table structure for transactions
-- ----------------------------
DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` double NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tx_date` datetime NOT NULL,
  `ledger_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ocr_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `owner_id` bigint NOT NULL,
  `ledger_id` bigint NULL DEFAULT NULL,
  `family_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `owner_id`(`owner_id` ASC) USING BTREE,
  INDEX `ledger_id`(`ledger_id` ASC) USING BTREE,
  INDEX `fk_transaction_family`(`family_id` ASC) USING BTREE,
  CONSTRAINT `fk_transaction_family` FOREIGN KEY (`family_id`) REFERENCES `families` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `transactions_ibfk_2` FOREIGN KEY (`ledger_id`) REFERENCES `ledgers` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of transactions
-- ----------------------------
INSERT INTO `transactions` VALUES (1, 35.5, '午餐', 'expense', '餐饮', '2026-04-15 12:30:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (2, 28, '早餐', 'expense', '餐饮', '2026-04-15 08:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (3, 156, '超市购物', 'expense', '购物', '2026-04-14 19:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (4, 5000, '工资', 'income', '工资', '2026-04-01 09:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (5, 200, '朋友还款', 'income', '其他', '2026-04-10 15:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (6, 89, '话费充值', 'expense', '通讯', '2026-04-05 10:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (7, 1200, '房租', 'expense', '住房', '2026-04-01 10:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (8, 450, '电费', 'expense', '住房', '2026-04-03 14:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (9, 320, '水费', 'expense', '住房', '2026-04-03 14:30:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (10, 68, '电影票', 'expense', '娱乐', '2026-04-08 20:00:00', '日常账本', NULL, NULL, 1, 1, NULL);
INSERT INTO `transactions` VALUES (11, 2500, '机票', 'expense', '交通', '2026-03-20 08:00:00', '旅行账本', NULL, NULL, 1, 2, NULL);
INSERT INTO `transactions` VALUES (12, 800, '酒店', 'expense', '住宿', '2026-03-20 14:00:00', '旅行账本', NULL, NULL, 1, 2, NULL);
INSERT INTO `transactions` VALUES (13, 350, '景点门票', 'expense', '娱乐', '2026-03-21 09:00:00', '旅行账本', NULL, NULL, 1, 2, NULL);
INSERT INTO `transactions` VALUES (14, 8000, '工资', 'income', '工资', '2026-04-01 09:00:00', '日常账本', NULL, NULL, 2, NULL, NULL);
INSERT INTO `transactions` VALUES (15, 99, '视频会员', 'expense', '娱乐', '2026-04-02 20:00:00', '日常账本', NULL, NULL, 2, NULL, NULL);
INSERT INTO `transactions` VALUES (16, 45, '午餐', 'expense', '餐饮', '2026-04-15 12:00:00', '日常账本', NULL, NULL, 2, NULL, NULL);
INSERT INTO `transactions` VALUES (17, 300, '书籍', 'expense', '学习', '2026-04-10 15:00:00', '日常账本', NULL, NULL, 2, NULL, NULL);
INSERT INTO `transactions` VALUES (18, 6500, '工资', 'income', '工资', '2026-04-01 09:00:00', '日常账本', NULL, NULL, 3, NULL, NULL);
INSERT INTO `transactions` VALUES (19, 188, '聚餐', 'expense', '餐饮', '2026-04-12 18:00:00', '日常账本', NULL, NULL, 3, NULL, NULL);
INSERT INTO `transactions` VALUES (20, 599, '运动鞋', 'expense', '购物', '2026-04-08 16:00:00', '日常账本', NULL, NULL, 3, NULL, NULL);
INSERT INTO `transactions` VALUES (21, 30, '吃饭', 'expense', '', '2026-04-16 15:42:41', '日常账本', NULL, NULL, 4, NULL, NULL);
INSERT INTO `transactions` VALUES (22, 85.5, '进口零食大礼包', 'expense', '食品', '2026-04-16 15:43:00', '日常账本', NULL, NULL, 4, NULL, NULL);
INSERT INTO `transactions` VALUES (23, 42, '海飞丝洗发水500ml', 'expense', '个人护理', '2026-04-16 15:43:00', '日常账本', NULL, NULL, 4, NULL, NULL);
INSERT INTO `transactions` VALUES (24, 0.5, '环保购物袋', 'expense', '杂项', '2026-04-16 15:43:00', '日常账本', NULL, NULL, 4, NULL, NULL);
INSERT INTO `transactions` VALUES (25, 1000, '吃饭', 'expense', '', '2026-05-16 18:47:45', '日常账本', NULL, NULL, 5, NULL, NULL);
INSERT INTO `transactions` VALUES (26, 334, '打工', 'income', '', '2026-05-16 18:47:57', '日常账本', NULL, NULL, 5, NULL, NULL);
INSERT INTO `transactions` VALUES (27, 3343, '做皮肤', 'expense', '美容保健', '2026-05-16 18:48:15', '日常账本', NULL, NULL, 5, NULL, NULL);
INSERT INTO `transactions` VALUES (28, 32343, '吃饭', 'expense', '', '2026-05-16 18:49:24', '日常账本', NULL, NULL, 5, NULL, NULL);
INSERT INTO `transactions` VALUES (29, 15000, '爸爸工资收入', 'income', '工资薪酬', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (30, 8000, '妈妈工资收入', 'income', '工资薪酬', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 1, NULL, 1);
INSERT INTO `transactions` VALUES (31, 3500, '房屋出租收入', 'income', '投资理财', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (32, 1200, '理财收益', 'income', '投资理财', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 2, NULL, 1);
INSERT INTO `transactions` VALUES (33, 500, '二手物品出售', 'income', '其他收入', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 3, NULL, 1);
INSERT INTO `transactions` VALUES (34, 256.5, '家庭聚餐海底捞', 'expense', '餐饮美食', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (35, 168, '周末外卖', 'expense', '餐饮美食', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 1, NULL, 1);
INSERT INTO `transactions` VALUES (36, 45.5, '早餐豆浆油条', 'expense', '餐饮美食', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 2, NULL, 1);
INSERT INTO `transactions` VALUES (37, 328, '孩子生日蛋糕', 'expense', '餐饮美食', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 3, NULL, 1);
INSERT INTO `transactions` VALUES (38, 89, '下午茶星巴克', 'expense', '餐饮美食', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 4, NULL, 1);
INSERT INTO `transactions` VALUES (39, 1299, '儿童衣服套装', 'expense', '购物消费', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (40, 599, '化妆品护肤品', 'expense', '购物消费', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 1, NULL, 1);
INSERT INTO `transactions` VALUES (41, 2399, '家庭电视', 'expense', '购物消费', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (42, 89, '日用品洗发水', 'expense', '购物消费', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 2, NULL, 1);
INSERT INTO `transactions` VALUES (43, 199, '书籍文具', 'expense', '购物消费', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 3, NULL, 1);
INSERT INTO `transactions` VALUES (44, 400, '汽车加油', 'expense', '交通出行', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (45, 35, '地铁充值', 'expense', '交通出行', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 1, NULL, 1);
INSERT INTO `transactions` VALUES (46, 128, '打车费用', 'expense', '交通出行', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 2, NULL, 1);
INSERT INTO `transactions` VALUES (47, 66, '共享单车月卡', 'expense', '交通出行', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 3, NULL, 1);
INSERT INTO `transactions` VALUES (48, 3500, '房贷月供', 'expense', '居家生活', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (49, 180, '水费', 'expense', '居家生活', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (50, 256, '电费', 'expense', '居家生活', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (51, 198, '燃气费', 'expense', '居家生活', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (52, 150, '物业费', 'expense', '居家生活', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (53, 200, '宽带费', 'expense', '居家生活', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (54, 2500, '孩子补习班', 'expense', '教育学习', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (55, 380, '英语网课', 'expense', '教育学习', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 3, NULL, 1);
INSERT INTO `transactions` VALUES (56, 128, '付费课程', 'expense', '教育学习', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 4, NULL, 1);
INSERT INTO `transactions` VALUES (57, 286, '感冒药品', 'expense', '医疗健康', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (58, 580, '体检费用', 'expense', '医疗健康', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 1, NULL, 1);
INSERT INTO `transactions` VALUES (59, 120, '维生素保健品', 'expense', '医疗健康', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 2, NULL, 1);
INSERT INTO `transactions` VALUES (60, 299, '视频会员年费', 'expense', '娱乐休闲', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (61, 180, '电影院门票', 'expense', '娱乐休闲', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 3, NULL, 1);
INSERT INTO `transactions` VALUES (62, 58, '游戏充值', 'expense', '娱乐休闲', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 4, NULL, 1);
INSERT INTO `transactions` VALUES (63, 88, '音乐会员', 'expense', '娱乐休闲', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 5, NULL, 1);
INSERT INTO `transactions` VALUES (64, 1000, '朋友结婚礼金', 'expense', '人情往来', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);
INSERT INTO `transactions` VALUES (65, 500, '生日礼物', 'expense', '人情往来', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 1, NULL, 1);
INSERT INTO `transactions` VALUES (66, 200, '红包发给孩子', 'expense', '人情往来', '2026-05-23 05:15:58', '家庭账本', NULL, NULL, 6, NULL, 1);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hashed_password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'https://api.dicebear.com/7.x/avataaars/svg?seed=testuser&backgroundColor=ff6b35', '2026-04-16 15:40:13', 'USER');
INSERT INTO `users` VALUES (2, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan&backgroundColor=4ade80', '2026-04-16 15:40:13', 'USER');
INSERT INTO `users` VALUES (3, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi&backgroundColor=3b82f6', '2026-04-16 15:40:13', 'USER');
INSERT INTO `users` VALUES (4, 'zhangsans', '$2a$10$Rc1Bxc1oCVbMnMJcomxtTeUa7KAzU7V6x2grbkc6C7f7po0M2/LvO', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsans&backgroundColor=ff6b35', '2026-04-16 15:42:22', 'USER');
INSERT INTO `users` VALUES (5, 'lizhang', '$2a$10$LiT420tdGR1RJ9NbQNeyp.dLWbq8MnvhutceK8ylHvoBk//RXlmhm', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lizhang&backgroundColor=ff6b35', '2026-05-16 18:32:29', 'USER');
INSERT INTO `users` VALUES (6, 'admin', '$2a$10$9DOXCgUx4FRMPunxbqL6aeoTWrsmMRYlmrtOk4x/AbB1AnqhTsclK', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin&backgroundColor=ff6b35', '2026-05-23 05:10:09', 'ADMIN');
INSERT INTO `users` VALUES (7, 'zhangsan1', '$2a$10$uXkD0G.PyD0H/1fi2b9wHeCv5GLz05RBaKb3fb3ofch9yXu6ex3Wi', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan1&backgroundColor=ff6b35', '2026-05-23 05:24:15', 'USER');

SET FOREIGN_KEY_CHECKS = 1;
