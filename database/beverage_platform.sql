/*
 Navicat Premium Data Transfer

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : beverage_platform

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 01/01/2026 04:37:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for announcement
-- ----------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seller_id` bigint NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `event_date` timestamp NULL DEFAULT NULL,
  `cover_image_id` bigint NULL DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING',
  `reviewed_by` bigint NULL DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NULL DEFAULT 1,
  `view_count` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `cover_image_id`(`cover_image_id` ASC) USING BTREE,
  INDEX `reviewed_by`(`reviewed_by` ASC) USING BTREE,
  INDEX `idx_seller_id`(`seller_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_event_date`(`event_date` ASC) USING BTREE,
  CONSTRAINT `announcement_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `announcement_ibfk_2` FOREIGN KEY (`cover_image_id`) REFERENCES `image` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `announcement_ibfk_3` FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of announcement
-- ----------------------------

-- ----------------------------
-- Table structure for avatar_image_data
-- ----------------------------
DROP TABLE IF EXISTS `avatar_image_data`;
CREATE TABLE `avatar_image_data`  (
  `image_id` bigint NOT NULL,
  `image_data` longblob NOT NULL,
  PRIMARY KEY (`image_id`) USING BTREE,
  CONSTRAINT `avatar_image_data_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '头像图片数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of avatar_image_data
-- ----------------------------

-- ----------------------------
-- Table structure for bar
-- ----------------------------
DROP TABLE IF EXISTS `bar`;
CREATE TABLE `bar`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `application_id` bigint NULL DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `latitude` decimal(10, 7) NULL DEFAULT NULL,
  `longitude` decimal(10, 7) NULL DEFAULT NULL,
  `opening_time` time NULL DEFAULT NULL,
  `closing_time` time NULL DEFAULT NULL,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `main_beverages` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `avg_rating` decimal(3, 2) NULL DEFAULT 0.00,
  `review_count` int NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `application_id`(`application_id` ASC) USING BTREE,
  INDEX `owner_id`(`owner_id` ASC) USING BTREE,
  INDEX `idx_bar_city`(`city` ASC) USING BTREE,
  INDEX `idx_bar_name`(`name` ASC) USING BTREE,
  CONSTRAINT `bar_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `bar_application` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `bar_ibfk_2` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '酒吧表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bar
-- ----------------------------
INSERT INTO `bar` VALUES (1, NULL, 'Speak Low 秘密客', '上海市黄浦区复兴中路579号', '上海市', '上海市', '黄浦区', 31.2231000, 121.4737000, '18:00:00', '02:00:00', '021-64012399', '隐藏在理发店后的秘密酒吧，以经典鸡尾酒和创意调酒闻名。', '鸡尾酒、威士忌', 1, 4.67, 3, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (2, NULL, 'Union Trading Company', '上海市黄浦区圆明园路169号协进大楼', '上海市', '上海市', '黄浦区', 31.2425000, 121.4897000, '17:00:00', '02:00:00', '021-60723428', '上海经典鸡尾酒吧，氛围优雅，适合商务聚会。', '鸡尾酒、金酒', 1, 4.50, 2, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (3, NULL, 'The Nest 巢', '上海市静安区铜仁路90弄4号', '上海市', '上海市', '静安区', 31.2304000, 121.4520000, '19:00:00', '03:00:00', '021-52376677', '位于老洋房的屋顶酒吧，视野开阔，调酒师技艺精湛。', '鸡尾酒、朗姆酒', 1, 4.50, 2, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (4, NULL, 'Janes & Hooch', '北京市朝阳区工人体育场北路4号', '北京市', '北京市', '朝阳区', 39.9289000, 116.4473000, '18:00:00', '02:00:00', '010-64159871', '工体附近的时尚酒吧，经常有DJ表演。', '鸡尾酒、伏特加', 1, 3.50, 2, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (5, NULL, 'Modo Urban Deli', '北京市东城区五道营胡同19号', '北京市', '北京市', '东城区', 39.9456000, 116.4106000, '11:00:00', '23:00:00', '010-64025805', '胡同里的创意餐酒吧，白天是咖啡馆，晚上是酒吧。', '精酿啤酒、葡萄酒', 1, 4.40, 0, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (6, NULL, 'Hope & Sesame 希望与芝麻', '广州市天河区天河路228号正佳广场', '广东省', '广州市', '天河区', 23.1367000, 113.3230000, '17:00:00', '02:00:00', '020-38732288', '屡获殊荣的鸡尾酒吧，以中式元素融合西方调酒技艺著称。', '鸡尾酒、白酒', 1, 5.00, 3, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (7, NULL, 'The Happy Monk', '广州市天河区天河北路都市华庭天怡阁', '广东省', '广州市', '天河区', 23.1486000, 113.3267000, '12:00:00', '02:00:00', '020-38731535', '比利时风格酒吧，提供丰富的精酿啤酒选择。', '精酿啤酒、比利时啤酒', 1, 4.10, 0, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (8, NULL, 'Jing Bar 廊桥', '成都市锦江区下东大街段166号', '四川省', '成都市', '锦江区', 30.6598000, 104.0861000, '18:00:00', '02:00:00', '028-86259999', '现代中式风格的鸡尾酒吧，环境优雅。', '鸡尾酒、中国烈酒', 1, 4.00, 2, '2026-01-01 00:21:04', '2026-01-01 00:36:17', 1);
INSERT INTO `bar` VALUES (9, NULL, 'Tipsy 微醺', '成都市武侯区玉林西路', '四川省', '成都市', '武侯区', 30.6409000, 104.0431000, '19:00:00', '03:00:00', '028-85555678', '玉林路上的小酒馆，氛围轻松，价格亲民。', '精酿啤酒、鸡尾酒', 1, 4.00, 0, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar` VALUES (10, NULL, 'Chez Pop', '杭州市上城区南山路200号', '浙江省', '杭州市', '上城区', 30.2489000, 120.1363000, '17:00:00', '01:00:00', '0571-87065890', '西湖边的小资酒吧，适合约会和聚会。', '葡萄酒、鸡尾酒', 1, 4.00, 1, '2026-01-01 00:21:04', '2026-01-01 00:21:04', 1);

-- ----------------------------
-- Table structure for bar_application
-- ----------------------------
DROP TABLE IF EXISTS `bar_application`;
CREATE TABLE `bar_application`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `opening_time` time NULL DEFAULT NULL,
  `closing_time` time NULL DEFAULT NULL,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `main_beverages` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `applicant_id` bigint NOT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING',
  `review_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `reviewed_by` bigint NULL DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `reviewed_by`(`reviewed_by` ASC) USING BTREE,
  INDEX `idx_application_status`(`status` ASC) USING BTREE,
  INDEX `idx_application_applicant`(`applicant_id` ASC) USING BTREE,
  CONSTRAINT `bar_application_ibfk_1` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `bar_application_ibfk_2` FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '酒吧申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bar_application
-- ----------------------------

-- ----------------------------
-- Table structure for bar_review
-- ----------------------------
DROP TABLE IF EXISTS `bar_review`;
CREATE TABLE `bar_review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bar_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `rating` int NOT NULL COMMENT '评分 1-5',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_bar_user_review`(`bar_id` ASC, `user_id` ASC, `is_active` ASC) USING BTREE,
  INDEX `idx_bar_review_bar`(`bar_id` ASC) USING BTREE,
  INDEX `idx_bar_review_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `bar_review_ibfk_1` FOREIGN KEY (`bar_id`) REFERENCES `bar` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `bar_review_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '酒吧评价表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bar_review
-- ----------------------------
INSERT INTO `bar_review` VALUES (1, 1, 1, 5, '调酒师技艺精湛，Old Fashioned做得非常正宗！环境也很有格调，推荐！', '2025-12-27 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (2, 1, 2, 4, '鸡尾酒味道不错，就是位置有点难找，要从理发店进去。', '2025-12-22 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (3, 1, 3, 5, '隐藏酒吧的氛围太棒了，每杯酒都是艺术品！', '2025-12-17 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (4, 2, 1, 4, '商务聚会的好地方，酒单选择丰富。', '2025-12-29 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (5, 2, 2, 5, '金酒特调非常出色，服务也很专业。', '2025-12-24 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (6, 3, 1, 5, '屋顶酒吧的景色绝了！配上一杯鸡尾酒，完美的夜晚。', '2025-12-30 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (7, 3, 3, 4, '环境很好，但价格偏贵。', '2025-12-25 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (8, 4, 2, 4, '工体附近难得的好酒吧，DJ放的音乐很对味。', '2025-12-28 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (9, 4, 3, 3, '人太多了，有点吵，但酒还不错。', '2025-12-20 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (10, 6, 1, 5, '中西融合的调酒理念太棒了！白酒调的鸡尾酒别有风味。', '2025-12-31 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (11, 6, 2, 5, '广州最好的鸡尾酒吧，没有之一！', '2025-12-26 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (12, 6, 3, 5, '每一杯酒都有故事，调酒师很专业。', '2025-12-21 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (13, 8, 1, 4, '成都难得的高品质鸡尾酒吧，环境优雅。', '2025-12-23 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (14, 8, 2, 4, '中式元素融入得很好，推荐！', '2025-12-18 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (15, 10, 3, 4, '西湖边的约会圣地，红酒选择不错。', '2025-12-19 00:21:04', '2026-01-01 00:21:04', 1);
INSERT INTO `bar_review` VALUES (16, 8, 11, 1, '**', '2026-01-01 00:35:34', '2026-01-01 00:36:17', 0);

-- ----------------------------
-- Table structure for beverage
-- ----------------------------
DROP TABLE IF EXISTS `beverage`;
CREATE TABLE `beverage`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_en` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `origin` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `alcohol_content` decimal(4, 2) NULL DEFAULT NULL,
  `volume` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `ingredients` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `taste_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `cover_image_id` bigint NULL DEFAULT NULL,
  `rating` decimal(3, 2) NULL DEFAULT 0.00,
  `rating_count` int NULL DEFAULT 0,
  `view_count` int NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `cover_image_id`(`cover_image_id` ASC) USING BTREE,
  INDEX `created_by`(`created_by` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_rating`(`rating` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  CONSTRAINT `beverage_ibfk_1` FOREIGN KEY (`cover_image_id`) REFERENCES `image` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `beverage_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '酒类饮品表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of beverage
-- ----------------------------

-- ----------------------------
-- Table structure for beverage_image
-- ----------------------------
DROP TABLE IF EXISTS `beverage_image`;
CREATE TABLE `beverage_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `beverage_id` bigint NOT NULL,
  `image_id` bigint NOT NULL,
  `image_order` int NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `image_id`(`image_id` ASC) USING BTREE,
  INDEX `idx_beverage_id`(`beverage_id` ASC) USING BTREE,
  CONSTRAINT `beverage_image_ibfk_1` FOREIGN KEY (`beverage_id`) REFERENCES `beverage` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `beverage_image_ibfk_2` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '饮品图片关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of beverage_image
-- ----------------------------

-- ----------------------------
-- Table structure for beverage_tag
-- ----------------------------
DROP TABLE IF EXISTS `beverage_tag`;
CREATE TABLE `beverage_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `beverage_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_beverage_tag`(`beverage_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `tag_id`(`tag_id` ASC) USING BTREE,
  CONSTRAINT `beverage_tag_ibfk_1` FOREIGN KEY (`beverage_id`) REFERENCES `beverage` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `beverage_tag_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '饮品标签关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of beverage_tag
-- ----------------------------

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `beverage_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `rating` decimal(2, 1) NULL DEFAULT NULL,
  `like_count` int NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NULL DEFAULT 0,
  `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'APPROVED',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_beverage_id`(`beverage_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`beverage_id`) REFERENCES `beverage` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `comment_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of comment
-- ----------------------------

-- ----------------------------
-- Table structure for comment_like
-- ----------------------------
DROP TABLE IF EXISTS `comment_like`;
CREATE TABLE `comment_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_comment_user`(`comment_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `comment_like_ibfk_1` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `comment_like_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of comment_like
-- ----------------------------

-- ----------------------------
-- Table structure for content_report
-- ----------------------------
DROP TABLE IF EXISTS `content_report`;
CREATE TABLE `content_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint NOT NULL COMMENT '举报人ID',
  `content_type` enum('POST_COMMENT','BAR_REVIEW','WIKI_DISCUSSION','BEVERAGE_COMMENT','POST','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被举报内容类型',
  `content_id` bigint NOT NULL COMMENT '被举报内容ID',
  `content_author_id` bigint NULL DEFAULT NULL COMMENT '被举报内容作者ID',
  `content_snapshot` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '被举报内容快照',
  `reason` enum('SPAM','ABUSE','PORNOGRAPHY','ILLEGAL','FRAUD','MISINFORMATION','HARASSMENT','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报原因',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '详细说明',
  `status` enum('PENDING','UNDER_REVIEW','CONFIRMED','DISMISSED','PROCESSED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '举报状态',
  `auto_moderation_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自动审核结果(JSON)',
  `risk_level` int NULL DEFAULT 0 COMMENT '风险等级(0-100)',
  `handler_id` bigint NULL DEFAULT NULL COMMENT '处理人ID',
  `handle_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理备注',
  `handled_at` timestamp NULL DEFAULT NULL COMMENT '处理时间',
  `handle_action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理动作',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `content_author_id`(`content_author_id` ASC) USING BTREE,
  INDEX `handler_id`(`handler_id` ASC) USING BTREE,
  INDEX `idx_report_reporter`(`reporter_id` ASC) USING BTREE,
  INDEX `idx_report_content`(`content_type` ASC, `content_id` ASC) USING BTREE,
  INDEX `idx_report_status`(`status` ASC) USING BTREE,
  INDEX `idx_report_risk`(`risk_level` DESC) USING BTREE,
  INDEX `idx_report_created`(`created_at` ASC) USING BTREE,
  CONSTRAINT `content_report_ibfk_1` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `content_report_ibfk_2` FOREIGN KEY (`content_author_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `content_report_ibfk_3` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '内容举报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of content_report
-- ----------------------------
INSERT INTO `content_report` VALUES (1, 2, 'BAR_REVIEW', 9, 3, '人太多了，有点吵，但酒还不错。', 'OTHER', '评价内容不够详细', 'PENDING', '{\"level\":\"APPROVED\",\"violations\":[]}', 15, NULL, NULL, NULL, NULL, '2025-12-30 00:21:04', '2026-01-01 00:21:04');
INSERT INTO `content_report` VALUES (2, 1, 'POST', 8, 1, '今天尝试了传说中的\"失身酒\"Long Island Iced Tea...', 'MISINFORMATION', '标题可能误导用户', 'UNDER_REVIEW', '{\"level\":\"PENDING_REVIEW\",\"violations\":[\"可能的争议性内容\"]}', 65, NULL, NULL, NULL, NULL, '2025-12-31 00:21:04', '2026-01-01 00:21:04');
INSERT INTO `content_report` VALUES (3, 3, 'BAR_REVIEW', 4, 1, '商务聚会的好地方，酒单选择丰富。', 'SPAM', '怀疑是商家自己刷的好评', 'DISMISSED', '{\"level\":\"APPROVED\",\"violations\":[]}', 25, NULL, NULL, NULL, NULL, '2025-12-27 00:21:04', '2026-01-01 00:21:04');
INSERT INTO `content_report` VALUES (4, 12, 'POST', 1, 1, '今晚在Speak Low品尝了一杯经典的Old Fashioned，调酒师的手法真是炉火纯青！威士忌的醇厚配上橙皮的清香，完美！🥃✨ #鸡尾酒 #上海酒吧', 'SPAM', '2232', 'PENDING', '{\"violations\":[],\"level\":\"APPROVED\"}', 30, NULL, NULL, NULL, NULL, '2026-01-01 01:20:48', '2026-01-01 01:20:48');
INSERT INTO `content_report` VALUES (5, 12, 'POST', 7, 1, '科普时间｜为什么鸡尾酒要用冰块摇匀而不是搅拌？其实这跟酒的成分有关系。含有果汁、奶油等不易混合的材料需要摇，而纯烈酒类则适合搅拌。涨知识了！📚 #鸡尾酒知识 #调酒技巧', 'OTHER', '123', 'DISMISSED', '{\"violations\":[],\"level\":\"APPROVED\"}', 20, 11, '111', '2026-01-01 04:32:20', 'DELETE', '2026-01-01 04:09:59', '2026-01-01 04:32:19');
INSERT INTO `content_report` VALUES (6, 12, 'POST', 6, 1, '杭州西湖边的这家酒吧真的绝了！坐在露台上，一边欣赏湖景，一边品着红酒，人生惬意不过如此。强烈推荐日落时分来，景色美到窒息！🌅🍷 #杭州 #西湖 #红酒', 'ABUSE', '11', 'CONFIRMED', '{\"level\":\"APPROVED\",\"violations\":[]}', 50, 11, '', '2026-01-01 04:32:14', 'MUTE_3', '2026-01-01 04:24:28', '2026-01-01 04:32:13');

-- ----------------------------
-- Table structure for daily_question
-- ----------------------------
DROP TABLE IF EXISTS `daily_question`;
CREATE TABLE `daily_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_date` date NOT NULL,
  `question` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_a` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_b` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_c` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_d` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `correct_option` tinyint NOT NULL,
  `count_a` int NULL DEFAULT 0,
  `count_b` int NULL DEFAULT 0,
  `count_c` int NULL DEFAULT 0,
  `count_d` int NULL DEFAULT 0,
  `explanation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `wiki_link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `question_date`(`question_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '每日一题表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of daily_question
-- ----------------------------
INSERT INTO `daily_question` VALUES (5, '2026-01-01', '今天你想探索哪一种饮品知识？', '葡萄酒的酿造工艺', '威士忌的熟成秘诀', '精酿啤酒的风味', '无酒精饮品的调配技巧', 0, 0, 0, 0, 0, '葡萄酒酿造涵盖葡萄采摘、发酵、陈酿等步骤，是理解饮品风味的基础。', '/wiki/classic-wine', '2026-01-01 00:16:20');

-- ----------------------------
-- Table structure for daily_question_answer
-- ----------------------------
DROP TABLE IF EXISTS `daily_question_answer`;
CREATE TABLE `daily_question_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `selected_option` tinyint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_daily_answer`(`question_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `daily_question_answer_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `daily_question` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `daily_question_answer_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '每日一题答案表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of daily_question_answer
-- ----------------------------

-- ----------------------------
-- Table structure for external_link
-- ----------------------------
DROP TABLE IF EXISTS `external_link`;
CREATE TABLE `external_link`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `beverage_id` bigint NOT NULL,
  `seller_id` bigint NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `price` decimal(10, 2) NULL DEFAULT NULL,
  `platform` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING',
  `reviewed_by` bigint NULL DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `reviewed_by`(`reviewed_by` ASC) USING BTREE,
  INDEX `idx_beverage_id`(`beverage_id` ASC) USING BTREE,
  INDEX `idx_seller_id`(`seller_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `external_link_ibfk_1` FOREIGN KEY (`beverage_id`) REFERENCES `beverage` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `external_link_ibfk_2` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `external_link_ibfk_3` FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '外部链接表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of external_link
-- ----------------------------

-- ----------------------------
-- Table structure for favorite
-- ----------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `beverage_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_beverage`(`user_id` ASC, `beverage_id` ASC) USING BTREE,
  INDEX `beverage_id`(`beverage_id` ASC) USING BTREE,
  CONSTRAINT `favorite_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `favorite_ibfk_2` FOREIGN KEY (`beverage_id`) REFERENCES `beverage` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收藏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of favorite
-- ----------------------------

-- ----------------------------
-- Table structure for general_image_data
-- ----------------------------
DROP TABLE IF EXISTS `general_image_data`;
CREATE TABLE `general_image_data`  (
  `image_id` bigint NOT NULL,
  `image_data` longblob NOT NULL,
  PRIMARY KEY (`image_id`) USING BTREE,
  CONSTRAINT `general_image_data_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通用图片数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of general_image_data
-- ----------------------------

-- ----------------------------
-- Table structure for image
-- ----------------------------
DROP TABLE IF EXISTS `image`;
CREATE TABLE `image`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `file_size` int NULL DEFAULT NULL,
  `uploaded_by` bigint NULL DEFAULT NULL,
  `category` enum('GENERAL','POST','AVATAR','WIKI') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'GENERAL',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uuid`(`uuid` ASC) USING BTREE,
  INDEX `idx_uuid`(`uuid` ASC) USING BTREE,
  INDEX `idx_uploaded_by`(`uploaded_by` ASC) USING BTREE,
  CONSTRAINT `fk_image_uploaded_by` FOREIGN KEY (`uploaded_by`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片元数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of image
-- ----------------------------

-- ----------------------------
-- Table structure for post_image_data
-- ----------------------------
DROP TABLE IF EXISTS `post_image_data`;
CREATE TABLE `post_image_data`  (
  `image_id` bigint NOT NULL,
  `image_data` longblob NOT NULL,
  PRIMARY KEY (`image_id`) USING BTREE,
  CONSTRAINT `post_image_data_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态图片数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of post_image_data
-- ----------------------------

-- ----------------------------
-- Table structure for post_tag
-- ----------------------------
DROP TABLE IF EXISTS `post_tag`;
CREATE TABLE `post_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag_category` enum('BEVERAGE_TYPE','TASTE','SCENE','LOCATION','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER',
  `source` enum('USER','AUTO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AUTO' COMMENT '标签来源：用户输入或自动提取',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_tag`(`post_id` ASC, `tag_name` ASC) USING BTREE,
  INDEX `idx_post_tag_post`(`post_id` ASC) USING BTREE,
  INDEX `idx_post_tag_name`(`tag_name` ASC) USING BTREE,
  INDEX `idx_post_tag_category`(`tag_category` ASC) USING BTREE,
  CONSTRAINT `post_tag_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `share_post` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 445 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of post_tag
-- ----------------------------
INSERT INTO `post_tag` VALUES (1, 1, '威士忌', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (2, 1, '鸡尾酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (3, 1, '醇厚', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (4, 1, '酒吧', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (5, 2, '啤酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (6, 2, '清爽', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (7, 2, '酒吧', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (8, 2, '聚会', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (9, 3, '鸡尾酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (10, 3, '朗姆酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (11, 3, '清爽', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (12, 3, '家中', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (13, 4, '啤酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (14, 4, '鸡尾酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (15, 4, '酒吧', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (16, 4, '聚会', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (17, 5, '威士忌', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (18, 5, '醇厚', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (19, 5, '烟熏', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (20, 5, '果香', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (21, 5, '品鉴', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (22, 6, '红酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (23, 6, '醇厚', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (24, 6, '约会', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (25, 6, '酒吧', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (28, 8, '鸡尾酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (29, 8, '伏特加', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (30, 8, '金酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (31, 8, '朗姆酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (32, 8, '聚会', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (33, 9, '啤酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (34, 9, '清爽', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (35, 9, '聚会', 'SCENE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (36, 9, '酒吧', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (37, 10, '鸡尾酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (38, 10, '酒吧', 'LOCATION', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (39, 10, '独酌', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (40, 11, '红酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (41, 11, '醇厚', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (42, 11, '品鉴', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (43, 12, '鸡尾酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (44, 12, '清爽', 'TASTE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (45, 12, '家中', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (46, 13, '威士忌', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (47, 13, '醇厚', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (48, 13, '品鉴', 'SCENE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (49, 14, '鸡尾酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (50, 14, '龙舌兰', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (51, 14, '酸爽', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (52, 14, '家中', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (53, 15, '鸡尾酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (54, 15, '酒吧', 'LOCATION', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (55, 15, '约会', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (56, 16, '鸡尾酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (57, 16, '品鉴', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (58, 16, '家中', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (59, 17, '鸡尾酒', 'BEVERAGE_TYPE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (60, 17, '甘甜', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (61, 17, '花香', 'TASTE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (62, 17, '约会', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (63, 18, '红酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (64, 18, '聚会', 'SCENE', 'USER', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (65, 18, '家中', 'LOCATION', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (66, 19, '金酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (67, 19, '伏特加', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (68, 19, '朗姆酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (69, 19, '威士忌', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (70, 19, '白兰地', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (71, 19, '品鉴', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (72, 20, '鸡尾酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (73, 20, '品鉴', 'SCENE', 'AUTO', '2025-12-31 23:12:52');
INSERT INTO `post_tag` VALUES (147, 22, '威士忌', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:48:16');
INSERT INTO `post_tag` VALUES (148, 22, '酒吧', 'LOCATION', 'AUTO', '2025-12-31 23:48:16');
INSERT INTO `post_tag` VALUES (149, 23, '威士忌', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:48:16');
INSERT INTO `post_tag` VALUES (150, 23, '白酒', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:48:16');
INSERT INTO `post_tag` VALUES (151, 23, '白兰地', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:48:16');
INSERT INTO `post_tag` VALUES (152, 23, '伏特加', 'BEVERAGE_TYPE', 'AUTO', '2025-12-31 23:48:16');

-- ----------------------------
-- Table structure for private_message
-- ----------------------------
DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_read` tinyint(1) NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_sender`(`sender_id` ASC) USING BTREE,
  INDEX `idx_message_receiver`(`receiver_id` ASC) USING BTREE,
  INDEX `idx_message_created_at`(`created_at` ASC) USING BTREE,
  CONSTRAINT `private_message_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `private_message_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of private_message
-- ----------------------------

-- ----------------------------
-- Table structure for share_post
-- ----------------------------
DROP TABLE IF EXISTS `share_post`;
CREATE TABLE `share_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ip_region` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `view_count` int NULL DEFAULT 0,
  `like_count` int NULL DEFAULT 0,
  `favorite_count` int NULL DEFAULT 0,
  `comment_count` int NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_share_post_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_share_post_created_at`(`created_at` ASC) USING BTREE,
  CONSTRAINT `share_post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态分享表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of share_post
-- ----------------------------
INSERT INTO `share_post` VALUES (1, 1, '今晚在Speak Low品尝了一杯经典的Old Fashioned，调酒师的手法真是炉火纯青！威士忌的醇厚配上橙皮的清香，完美！🥃✨ #鸡尾酒 #上海酒吧', '上海', '鸡尾酒,威士忌,酒吧打卡', '180.168.1.1', '上海', 1598, 245, 89, 67, '2025-12-31 16:21:04');
INSERT INTO `share_post` VALUES (2, 1, '周末探店｜发现了一家宝藏精酿啤酒吧！十几种进口精酿随便选，老板还会根据你的口味推荐，服务超级贴心～强烈推荐IPA爱好者来试试！🍺 #精酿啤酒 #周末好去处', '北京', '精酿啤酒,探店,周末', '123.125.1.1', '北京', 2343, 312, 156, 98, '2025-12-31 09:21:04');
INSERT INTO `share_post` VALUES (3, 1, '第一次尝试自己在家调制Mojito，虽然卖相不太好，但味道还不错！薄荷叶一定要用新鲜的，差别真的很大。分享一下配方给大家～🍹 #在家调酒 #莫吉托', '广州', '鸡尾酒,DIY,生活分享', '113.108.1.1', '广州', 1920, 278, 134, 82, '2025-12-31 04:21:04');
INSERT INTO `share_post` VALUES (4, 1, '成都的酒吧文化真的太棒了！昨晚在玉林路的小酒馆听了一场live，氛围感拉满！配上几杯特调，这才是生活该有的样子～🎸🍻 #成都夜生活 #音乐酒吧', '成都', '酒吧,音乐,夜生活', '171.208.1.1', '四川', 1661, 198, 77, 54, '2025-12-30 00:21:04');
INSERT INTO `share_post` VALUES (5, 1, '威士忌品鉴笔记｜今天品尝了三款单一麦芽威士忌，从艾雷岛的泥煤味到斯佩塞的果香，每一款都有独特的风味。最喜欢的还是Highland Park 12年，平衡感极佳！📝 #威士忌 #品鉴笔记', '上海', '威士忌,品鉴,学习', '180.168.2.1', '上海', 1421, 167, 92, 45, '2025-12-29 00:21:04');
INSERT INTO `share_post` VALUES (6, 1, '杭州西湖边的这家酒吧真的绝了！坐在露台上，一边欣赏湖景，一边品着红酒，人生惬意不过如此。强烈推荐日落时分来，景色美到窒息！🌅🍷 #杭州 #西湖 #红酒', '杭州', '红酒,旅行,风景', '115.236.1.1', '浙江', 2182, 289, 145, 71, '2025-12-29 00:21:04');
INSERT INTO `share_post` VALUES (8, 1, '今天尝试了传说中的\"失身酒\"Long Island Iced Tea，四种基酒混合居然喝起来像冰茶？！后劲真的很大，大家喝的时候要注意哦～😵 #长岛冰茶 #鸡尾酒', '广州', '鸡尾酒,体验分享', '113.108.2.1', '广州', 1569, 187, 67, 49, '2025-12-26 00:21:04');
INSERT INTO `share_post` VALUES (9, 1, '酒吧氛围组｜昨晚和朋友们在酒吧玩桌游，配上几杯特调啤酒，笑到肚子疼。这家店的桌游种类超多，适合聚会！🎲🍺 #桌游 #聚会 #啤酒', '成都', '聚会,桌游,啤酒', '171.208.2.1', '四川', 1230, 156, 45, 38, '2025-12-25 00:21:04');
INSERT INTO `share_post` VALUES (10, 1, '分享一个小众酒吧，藏在老街区的深处，没有招牌，只有知道的人才会来。老板是个调酒大师，每杯酒都是艺术品！💎 #小众酒吧 #隐藏好店', '上海', '探店,小众,酒吧', '180.168.3.1', '上海', 890, 98, 34, 21, '2025-12-22 00:21:04');
INSERT INTO `share_post` VALUES (11, 1, '葡萄酒入门指南｜新手如何选择适合自己的葡萄酒？从产区、品种、年份三个维度来看，其实并不复杂。今天先讲讲法国波尔多～🍇 #葡萄酒 #入门指南', '杭州', '葡萄酒,知识,入门', '115.236.2.1', '浙江', 1120, 142, 67, 28, '2025-12-20 00:21:04');
INSERT INTO `share_post` VALUES (12, 1, '夏日特调推荐｜薄荷柠檬苏打，清爽解暑！做法超简单：新鲜薄荷+柠檬汁+苏打水+冰块，完美！🍋🌿 #夏日饮品 #清爽 #DIY', '广州', '饮品,夏日,DIY', '113.108.3.1', '广州', 1341, 165, 78, 35, '2025-12-19 00:21:04');
INSERT INTO `share_post` VALUES (13, 1, '参加了一场威士忌品鉴会，学到了很多专业知识。原来威士忌的颜色深浅和年份、橡木桶类型都有关系！受益匪浅～ #威士忌 #品鉴会', '北京', '威士忌,学习,活动', '123.125.3.1', '北京', 760, 89, 28, 18, '2025-12-12 00:21:04');
INSERT INTO `share_post` VALUES (14, 1, '今天学会了调制Margarita！盐边杯口的处理是个技术活，试了好几次才成功。龙舌兰的味道真的很特别！🍹 #玛格丽特 #学习调酒', '成都', '鸡尾酒,学习,龙舌兰', '171.208.3.1', '四川', 920, 112, 41, 24, '2025-12-07 00:21:04');
INSERT INTO `share_post` VALUES (15, 1, '酒吧探店｜这家新开的酒吧装修风格很复古，放的都是老歌，很有感觉。酒单也很有特色，推荐他们的招牌鸡尾酒！🎵 #探店 #复古酒吧', '上海', '探店,酒吧,复古', '180.168.4.1', '上海', 1050, 128, 52, 31, '2025-12-04 00:21:04');
INSERT INTO `share_post` VALUES (16, 1, '记录一下我的调酒学习之路，从最基础的Six Basic开始，慢慢进步。希望有一天能成为专业调酒师！💪 #调酒 #学习记录 #梦想', '杭州', '调酒,学习,梦想', '115.236.3.1', '浙江', 680, 76, 23, 15, '2025-11-27 00:21:04');
INSERT INTO `share_post` VALUES (17, 1, '春日限定｜樱花鸡尾酒，颜值和口感都在线！粉粉嫩嫩的超级适合拍照～这个季节一定要试试！🌸 #樱花 #春日限定 #鸡尾酒', '广州', '鸡尾酒,樱花,春日', '113.108.4.1', '广州', 1450, 198, 89, 42, '2025-11-22 00:21:04');
INSERT INTO `share_post` VALUES (18, 1, '周末和朋友们组织了一场家庭酒会，每个人都带了自己喜欢的酒，交流品鉴心得。这种聚会形式真的很棒！🥂 #聚会 #品酒 #周末', '北京', '聚会,品酒,社交', '123.125.4.1', '北京', 1180, 145, 67, 38, '2025-11-12 00:21:04');
INSERT INTO `share_post` VALUES (19, 1, '终于收集齐了六大基酒！金酒、伏特加、朗姆酒、龙舌兰、威士忌、白兰地。接下来要系统学习每种酒的特点和经典调配～📚 #基酒 #收藏 #学习', '成都', '基酒,学习,收藏', '171.208.4.1', '四川', 840, 102, 45, 26, '2025-11-02 00:21:04');
INSERT INTO `share_post` VALUES (20, 1, '分享一个调酒小技巧：冰块的大小和形状会影响酒的口感。大冰块融化慢，适合需要慢慢品味的酒；碎冰融化快，适合需要快速冷却的饮品。细节决定成败！❄️ #调酒技巧 #小知识', '上海', '技巧,知识,分享', '180.168.5.1', '上海', 1290, 167, 78, 44, '2025-10-23 00:21:04');
INSERT INTO `share_post` VALUES (21, 6, '清酒入门｜獭祭二割三分，精米步合23%，果香浓郁，入口绵柔。虽然价格不菲，但品质确实出众。适合清酒入门者的第一瓶大吟酿！🍶 #清酒 #獭祭', '上海', '清酒,日本酒,品鉴', '180.168.6.1', '上海', 893, 112, 56, 28, '2025-12-27 00:27:24');
INSERT INTO `share_post` VALUES (22, 7, '茅台品鉴｜飞天茅台2019年，酱香突出，回味悠长。存放三年后，口感更加圆润。好酒需要时间沉淀！🥃 #茅台 #酱香型白酒', '贵州', '白酒,茅台,收藏', '117.135.1.1', '贵州', 1560, 234, 123, 67, '2025-12-24 00:27:24');
INSERT INTO `share_post` VALUES (23, 6, '日本酒藏之旅｜参观了新潟的八海山酒藏，了解了清酒的酿造过程。从精米到发酵，每一步都充满匠心。回来带了几瓶限定酒！✈️🍶 #日本旅行 #清酒', '日本', '清酒,旅行,酒藏', '180.168.7.1', '日本', 2102, 287, 156, 89, '2025-12-17 00:27:24');
INSERT INTO `share_post` VALUES (24, 7, '中国白酒香型科普｜酱香、浓香、清香、米香...你知道它们的区别吗？今天来聊聊各大香型的特点和代表品牌！📖 #白酒知识 #香型', '北京', '白酒,知识,科普', '123.125.5.1', '北京', 1780, 198, 87, 45, '2025-12-10 00:27:24');

-- ----------------------------
-- Table structure for share_post_comment
-- ----------------------------
DROP TABLE IF EXISTS `share_post_comment`;
CREATE TABLE `share_post_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `like_count` int NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `share_post_comment_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `share_post` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `share_post_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `share_post_comment_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `share_post_comment` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态评论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of share_post_comment
-- ----------------------------
INSERT INTO `share_post_comment` VALUES (1, 1, 3, NULL, 'Old Fashioned是我最爱的鸡尾酒之一！Speak Low的调酒师确实很厉害', 12, '2025-12-31 17:27:24');
INSERT INTO `share_post_comment` VALUES (2, 1, 5, NULL, '作为调酒师，我觉得他们的威士忌选品很棒，推荐尝试他们的单一麦芽系列', 8, '2025-12-31 18:27:24');
INSERT INTO `share_post_comment` VALUES (3, 1, 8, 1, '请问Old Fashioned一般用什么威士忌调比较好？', 3, '2025-12-31 19:27:24');
INSERT INTO `share_post_comment` VALUES (4, 1, 5, 3, '推荐用波本威士忌，比如Maker\'s Mark或者Buffalo Trace，口感更柔和', 6, '2025-12-31 20:27:24');
INSERT INTO `share_post_comment` VALUES (5, 2, 2, NULL, 'IPA确实是精酿入门的好选择，苦度适中，香气丰富', 15, '2025-12-31 10:27:24');
INSERT INTO `share_post_comment` VALUES (6, 2, 9, NULL, '这家店我也去过！老板很热情，还会讲解每款啤酒的特点', 9, '2025-12-31 11:27:24');
INSERT INTO `share_post_comment` VALUES (7, 3, 5, NULL, '新手调Mojito的话，建议薄荷叶不要捣太碎，轻轻按压就好，否则会有苦味', 18, '2025-12-31 05:27:24');
INSERT INTO `share_post_comment` VALUES (8, 3, 10, NULL, '看起来很清爽！夏天就应该喝这种', 5, '2025-12-31 06:27:24');
INSERT INTO `share_post_comment` VALUES (9, 5, 7, NULL, 'Highland Park确实是入门单麦的好选择，泥煤味不重但层次丰富', 11, '2025-12-30 00:27:24');
INSERT INTO `share_post_comment` VALUES (10, 5, 6, NULL, '我更喜欢日本威士忌，山崎12年也很不错', 7, '2025-12-30 00:27:24');
INSERT INTO `share_post_comment` VALUES (11, 6, 10, NULL, '西湖边看日落喝红酒，太浪漫了！下次约会要去', 14, '2025-12-30 00:27:24');
INSERT INTO `share_post_comment` VALUES (12, 6, 9, NULL, '这家我也打卡过，露台位置需要提前预约哦', 8, '2025-12-30 00:27:24');
INSERT INTO `share_post_comment` VALUES (15, 21, 7, NULL, '獭祭系列都很不错，23%精米步合的确实是顶级', 9, '2025-12-28 00:27:24');
INSERT INTO `share_post_comment` VALUES (16, 22, 2, NULL, '茅台存放确实需要时间，新酒和老酒差别很大', 13, '2025-12-25 00:27:24');

-- ----------------------------
-- Table structure for share_post_image
-- ----------------------------
DROP TABLE IF EXISTS `share_post_image`;
CREATE TABLE `share_post_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `share_post_id` bigint NOT NULL,
  `image_id` bigint NOT NULL,
  `image_order` int NULL DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `image_id`(`image_id` ASC) USING BTREE,
  INDEX `idx_share_post_id`(`share_post_id` ASC) USING BTREE,
  CONSTRAINT `share_post_image_ibfk_1` FOREIGN KEY (`share_post_id`) REFERENCES `share_post` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `share_post_image_ibfk_2` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态图片关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of share_post_image
-- ----------------------------

-- ----------------------------
-- Table structure for share_post_like
-- ----------------------------
DROP TABLE IF EXISTS `share_post_like`;
CREATE TABLE `share_post_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_like`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `share_post_like_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `share_post` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `share_post_like_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of share_post_like
-- ----------------------------

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标签表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tag
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('USER','SELLER','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `avatar_image_id` bigint NULL DEFAULT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `gender` enum('MALE','FEMALE','SECRET') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'SECRET',
  `birthday` date NULL DEFAULT NULL,
  `level` int NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NULL DEFAULT 1,
  `mute_until` timestamp NULL DEFAULT NULL COMMENT '禁言截止时间',
  `message_policy` enum('ALL','FOLLOWERS_ONLY','NONE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ALL' COMMENT '私信接收策略',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE,
  INDEX `avatar_image_id`(`avatar_image_id` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE,
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`avatar_image_id`) REFERENCES `image` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'admin@chunyin.com', 'ADMIN', NULL, NULL, '醇饮平台管理员，负责内容审核和平台运营。', 'SECRET', '1990-01-01', 10, '2025-01-01 00:21:04', '2026-01-01 04:32:13', 1, '2026-01-04 04:32:14', 'ALL');
INSERT INTO `user` VALUES (2, 'demo', '$2a$10$cm7X8Jxxo2A3InK2Fk2qDuvl6UoT39h1Uhax0DPdLQesMH60tdCx.', 'demo@example.com', 'ADMIN', NULL, 'https://api.dicebear.com/7.x/thumbs/svg?seed=demo', '示例账号，方便开发调试。', 'SECRET', NULL, 1, '2026-01-01 00:16:10', '2026-01-01 00:16:10', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (3, '精酿达人小王', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'craft_beer@test.com', 'USER', NULL, NULL, '精酿啤酒爱好者，IPA是我的最爱！周末常去各种精酿酒吧打卡。', 'MALE', '1995-08-20', 4, '2025-09-03 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (4, '红酒小姐姐', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'wine_lover@test.com', 'USER', NULL, NULL, '葡萄酒品鉴师在读，喜欢法国波尔多和勃艮第。偶尔也会调一杯鸡尾酒放松。', 'FEMALE', '1994-03-10', 6, '2025-06-15 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (5, '调酒师阿杰', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'bartender_jie@test.com', 'SELLER', NULL, NULL, '专业调酒师，从业8年。擅长经典鸡尾酒和创意特调，欢迎交流调酒技巧！', 'MALE', '1988-11-25', 8, '2025-03-07 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (6, '清酒控', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'sake_fan@test.com', 'USER', NULL, NULL, '日本清酒爱好者，每年都会去日本酒藏朝圣。獭祭、久保田、八海山都是我的心头好。', 'FEMALE', '1993-07-08', 4, '2025-10-03 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (7, '白酒文化传承人', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'baijiu@test.com', 'USER', NULL, NULL, '中国白酒文化研究者，专注于酱香型白酒的品鉴与收藏。茅台、郎酒、习酒都有涉猎。', 'MALE', '1985-12-01', 7, '2025-04-26 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (8, '鸡尾酒新手', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'cocktail_newbie@test.com', 'USER', NULL, NULL, '刚入坑的鸡尾酒爱好者，正在学习调酒基础知识。欢迎大佬们指点！', 'SECRET', '1998-04-18', 2, '2025-12-02 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (9, '酒吧探店博主', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'bar_explorer@test.com', 'USER', NULL, NULL, '全国酒吧探店中，已打卡200+家酒吧。分享真实体验，不恰饭！', 'FEMALE', '1996-09-30', 6, '2025-08-04 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (10, '微醺生活家', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh7kYKS', 'tipsy_life@test.com', 'USER', NULL, NULL, '享受微醺的感觉，记录生活中的小确幸。偶尔喝点小酒，人生惬意。', 'SECRET', '1991-06-22', 3, '2025-11-02 00:21:04', '2026-01-01 00:21:04', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (11, 'demo1', '$2a$10$ZAzuO8GyWtW8.B9mRniVvuBALtVuTEmnlTsP2xs0EdqWnnm0Mb2mG', '2350283@tongji.edu.cn', 'ADMIN', NULL, 'https://api.dicebear.com/7.x/thumbs/svg?seed=demo1', '欢迎来到饮品圈，我正在持续探索新的佳酿。', 'SECRET', NULL, 1, '2026-01-01 00:32:33', '2026-01-01 00:36:26', 1, NULL, 'ALL');
INSERT INTO `user` VALUES (12, 'demo2', '$2a$10$IgDaRw1EAkqXr3MvX1KdOeB6QY3PJkYgy/bPXRQwzcPnOZDDYU2wi', '2350283@tongji.edu.cnx', 'USER', NULL, 'https://api.dicebear.com/7.x/thumbs/svg?seed=demo2', '欢迎来到饮品圈，我正在持续探索新的佳酿。', 'SECRET', NULL, 1, '2026-01-01 01:20:38', '2026-01-01 01:20:38', 1, NULL, 'ALL');

-- ----------------------------
-- Table structure for user_behavior
-- ----------------------------
DROP TABLE IF EXISTS `user_behavior`;
CREATE TABLE `user_behavior`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_type` enum('POST','BEVERAGE','WIKI','BAR') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `behavior_type` enum('VIEW','LIKE','FAVORITE','COMMENT','SHARE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight` int NOT NULL DEFAULT 1 COMMENT '行为权重: VIEW=1, LIKE=3, FAVORITE=5, COMMENT=4, SHARE=2',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_behavior_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_behavior_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_user_behavior_type`(`behavior_type` ASC) USING BTREE,
  INDEX `idx_user_behavior_created`(`created_at` ASC) USING BTREE,
  CONSTRAINT `user_behavior_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 282 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户行为记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behavior
-- ----------------------------
INSERT INTO `user_behavior` VALUES (1, 1, 'POST', 4, 'VIEW', 1, '2025-12-31 23:06:00');
INSERT INTO `user_behavior` VALUES (2, 2, 'POST', 21, 'VIEW', 1, '2025-12-31 23:08:15');
INSERT INTO `user_behavior` VALUES (3, 2, 'POST', 21, 'LIKE', 3, '2025-12-31 23:08:17');
INSERT INTO `user_behavior` VALUES (4, 2, 'POST', 21, 'COMMENT', 4, '2025-12-31 23:08:22');
INSERT INTO `user_behavior` VALUES (5, 2, 'POST', 21, 'FAVORITE', 5, '2025-12-31 23:08:23');
INSERT INTO `user_behavior` VALUES (6, 2, 'POST', 3, 'VIEW', 1, '2025-12-31 23:08:39');
INSERT INTO `user_behavior` VALUES (7, 1, 'POST', 1, 'VIEW', 1, '2025-12-30 23:12:52');
INSERT INTO `user_behavior` VALUES (8, 1, 'POST', 1, 'LIKE', 3, '2025-12-30 23:12:52');
INSERT INTO `user_behavior` VALUES (9, 1, 'POST', 1, 'FAVORITE', 5, '2025-12-30 23:12:52');
INSERT INTO `user_behavior` VALUES (10, 1, 'POST', 5, 'VIEW', 1, '2025-12-29 23:12:52');
INSERT INTO `user_behavior` VALUES (11, 1, 'POST', 5, 'LIKE', 3, '2025-12-29 23:12:52');
INSERT INTO `user_behavior` VALUES (12, 1, 'POST', 5, 'COMMENT', 4, '2025-12-29 23:12:52');
INSERT INTO `user_behavior` VALUES (13, 1, 'POST', 13, 'VIEW', 1, '2025-12-28 23:12:52');
INSERT INTO `user_behavior` VALUES (14, 1, 'POST', 13, 'LIKE', 3, '2025-12-28 23:12:52');
INSERT INTO `user_behavior` VALUES (15, 1, 'POST', 7, 'VIEW', 1, '2025-12-27 23:12:52');
INSERT INTO `user_behavior` VALUES (16, 1, 'POST', 3, 'VIEW', 1, '2025-12-26 23:12:52');
INSERT INTO `user_behavior` VALUES (17, 1, 'POST', 3, 'LIKE', 3, '2025-12-26 23:12:52');
INSERT INTO `user_behavior` VALUES (18, 2, 'POST', 2, 'VIEW', 1, '2025-12-30 23:12:52');
INSERT INTO `user_behavior` VALUES (19, 2, 'POST', 2, 'LIKE', 3, '2025-12-30 23:12:52');
INSERT INTO `user_behavior` VALUES (20, 2, 'POST', 2, 'FAVORITE', 5, '2025-12-30 23:12:52');
INSERT INTO `user_behavior` VALUES (21, 2, 'POST', 4, 'VIEW', 1, '2025-12-29 23:12:52');
INSERT INTO `user_behavior` VALUES (22, 2, 'POST', 4, 'LIKE', 3, '2025-12-29 23:12:52');
INSERT INTO `user_behavior` VALUES (23, 2, 'POST', 9, 'VIEW', 1, '2025-12-28 23:12:52');
INSERT INTO `user_behavior` VALUES (24, 2, 'POST', 9, 'LIKE', 3, '2025-12-28 23:12:52');
INSERT INTO `user_behavior` VALUES (25, 2, 'POST', 9, 'FAVORITE', 5, '2025-12-28 23:12:52');
INSERT INTO `user_behavior` VALUES (26, 2, 'POST', 18, 'VIEW', 1, '2025-12-27 23:12:52');
INSERT INTO `user_behavior` VALUES (27, 2, 'POST', 18, 'COMMENT', 4, '2025-12-27 23:12:52');
INSERT INTO `user_behavior` VALUES (37, 1, 'POST', 1, 'VIEW', 1, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (38, 1, 'POST', 1, 'LIKE', 3, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (39, 1, 'POST', 1, 'FAVORITE', 5, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (40, 1, 'POST', 5, 'VIEW', 1, '2025-12-29 23:13:42');
INSERT INTO `user_behavior` VALUES (41, 1, 'POST', 5, 'LIKE', 3, '2025-12-29 23:13:42');
INSERT INTO `user_behavior` VALUES (42, 1, 'POST', 5, 'COMMENT', 4, '2025-12-29 23:13:42');
INSERT INTO `user_behavior` VALUES (43, 1, 'POST', 13, 'VIEW', 1, '2025-12-28 23:13:42');
INSERT INTO `user_behavior` VALUES (44, 1, 'POST', 13, 'LIKE', 3, '2025-12-28 23:13:42');
INSERT INTO `user_behavior` VALUES (45, 1, 'POST', 7, 'VIEW', 1, '2025-12-27 23:13:42');
INSERT INTO `user_behavior` VALUES (46, 1, 'POST', 3, 'VIEW', 1, '2025-12-26 23:13:42');
INSERT INTO `user_behavior` VALUES (47, 1, 'POST', 3, 'LIKE', 3, '2025-12-26 23:13:42');
INSERT INTO `user_behavior` VALUES (48, 2, 'POST', 2, 'VIEW', 1, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (49, 2, 'POST', 2, 'LIKE', 3, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (50, 2, 'POST', 2, 'FAVORITE', 5, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (51, 2, 'POST', 4, 'VIEW', 1, '2025-12-29 23:13:42');
INSERT INTO `user_behavior` VALUES (52, 2, 'POST', 4, 'LIKE', 3, '2025-12-29 23:13:42');
INSERT INTO `user_behavior` VALUES (53, 2, 'POST', 9, 'VIEW', 1, '2025-12-28 23:13:42');
INSERT INTO `user_behavior` VALUES (54, 2, 'POST', 9, 'LIKE', 3, '2025-12-28 23:13:42');
INSERT INTO `user_behavior` VALUES (55, 2, 'POST', 9, 'FAVORITE', 5, '2025-12-28 23:13:42');
INSERT INTO `user_behavior` VALUES (56, 2, 'POST', 18, 'VIEW', 1, '2025-12-27 23:13:42');
INSERT INTO `user_behavior` VALUES (57, 2, 'POST', 18, 'COMMENT', 4, '2025-12-27 23:13:42');
INSERT INTO `user_behavior` VALUES (58, 3, 'POST', 6, 'VIEW', 1, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (59, 3, 'POST', 6, 'LIKE', 3, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (60, 3, 'POST', 6, 'FAVORITE', 5, '2025-12-30 23:13:42');
INSERT INTO `user_behavior` VALUES (61, 3, 'POST', 11, 'VIEW', 1, '2025-12-29 23:13:42');
INSERT INTO `user_behavior` VALUES (62, 3, 'POST', 11, 'LIKE', 3, '2025-12-29 23:13:42');
INSERT INTO `user_behavior` VALUES (63, 3, 'POST', 15, 'VIEW', 1, '2025-12-28 23:13:42');
INSERT INTO `user_behavior` VALUES (64, 3, 'POST', 17, 'VIEW', 1, '2025-12-27 23:13:42');
INSERT INTO `user_behavior` VALUES (65, 3, 'POST', 17, 'LIKE', 3, '2025-12-27 23:13:42');
INSERT INTO `user_behavior` VALUES (66, 3, 'POST', 17, 'FAVORITE', 5, '2025-12-27 23:13:42');
INSERT INTO `user_behavior` VALUES (67, 2, 'POST', 12, 'VIEW', 1, '2025-12-31 23:14:22');
INSERT INTO `user_behavior` VALUES (98, 2, 'POST', 2, 'VIEW', 1, '2025-12-31 00:17:02');
INSERT INTO `user_behavior` VALUES (99, 2, 'POST', 2, 'LIKE', 3, '2025-12-31 00:17:02');
INSERT INTO `user_behavior` VALUES (100, 2, 'POST', 2, 'FAVORITE', 5, '2025-12-31 00:17:02');
INSERT INTO `user_behavior` VALUES (101, 2, 'POST', 4, 'VIEW', 1, '2025-12-30 00:17:02');
INSERT INTO `user_behavior` VALUES (102, 2, 'POST', 4, 'LIKE', 3, '2025-12-30 00:17:02');
INSERT INTO `user_behavior` VALUES (103, 2, 'POST', 9, 'VIEW', 1, '2025-12-29 00:17:02');
INSERT INTO `user_behavior` VALUES (104, 2, 'POST', 9, 'LIKE', 3, '2025-12-29 00:17:02');
INSERT INTO `user_behavior` VALUES (105, 2, 'POST', 9, 'FAVORITE', 5, '2025-12-29 00:17:02');
INSERT INTO `user_behavior` VALUES (106, 2, 'POST', 18, 'VIEW', 1, '2025-12-28 00:17:02');
INSERT INTO `user_behavior` VALUES (107, 2, 'POST', 18, 'COMMENT', 4, '2025-12-28 00:17:02');
INSERT INTO `user_behavior` VALUES (128, 1, 'POST', 1, 'VIEW', 1, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (129, 1, 'POST', 1, 'LIKE', 3, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (130, 1, 'POST', 1, 'FAVORITE', 5, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (131, 1, 'POST', 5, 'VIEW', 1, '2025-12-30 00:21:04');
INSERT INTO `user_behavior` VALUES (132, 1, 'POST', 5, 'LIKE', 3, '2025-12-30 00:21:04');
INSERT INTO `user_behavior` VALUES (133, 1, 'POST', 5, 'COMMENT', 4, '2025-12-30 00:21:04');
INSERT INTO `user_behavior` VALUES (134, 1, 'POST', 13, 'VIEW', 1, '2025-12-29 00:21:04');
INSERT INTO `user_behavior` VALUES (135, 1, 'POST', 13, 'LIKE', 3, '2025-12-29 00:21:04');
INSERT INTO `user_behavior` VALUES (136, 1, 'POST', 7, 'VIEW', 1, '2025-12-28 00:21:04');
INSERT INTO `user_behavior` VALUES (137, 1, 'POST', 3, 'VIEW', 1, '2025-12-27 00:21:04');
INSERT INTO `user_behavior` VALUES (138, 1, 'POST', 3, 'LIKE', 3, '2025-12-27 00:21:04');
INSERT INTO `user_behavior` VALUES (139, 2, 'POST', 2, 'VIEW', 1, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (140, 2, 'POST', 2, 'LIKE', 3, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (141, 2, 'POST', 2, 'FAVORITE', 5, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (142, 2, 'POST', 4, 'VIEW', 1, '2025-12-30 00:21:04');
INSERT INTO `user_behavior` VALUES (143, 2, 'POST', 4, 'LIKE', 3, '2025-12-30 00:21:04');
INSERT INTO `user_behavior` VALUES (144, 2, 'POST', 9, 'VIEW', 1, '2025-12-29 00:21:04');
INSERT INTO `user_behavior` VALUES (145, 2, 'POST', 9, 'LIKE', 3, '2025-12-29 00:21:04');
INSERT INTO `user_behavior` VALUES (146, 2, 'POST', 9, 'FAVORITE', 5, '2025-12-29 00:21:04');
INSERT INTO `user_behavior` VALUES (147, 2, 'POST', 18, 'VIEW', 1, '2025-12-28 00:21:04');
INSERT INTO `user_behavior` VALUES (148, 2, 'POST', 18, 'COMMENT', 4, '2025-12-28 00:21:04');
INSERT INTO `user_behavior` VALUES (149, 3, 'POST', 6, 'VIEW', 1, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (150, 3, 'POST', 6, 'LIKE', 3, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (151, 3, 'POST', 6, 'FAVORITE', 5, '2025-12-31 00:21:04');
INSERT INTO `user_behavior` VALUES (152, 3, 'POST', 11, 'VIEW', 1, '2025-12-30 00:21:04');
INSERT INTO `user_behavior` VALUES (153, 3, 'POST', 11, 'LIKE', 3, '2025-12-30 00:21:04');
INSERT INTO `user_behavior` VALUES (154, 3, 'POST', 15, 'VIEW', 1, '2025-12-29 00:21:04');
INSERT INTO `user_behavior` VALUES (155, 3, 'POST', 17, 'VIEW', 1, '2025-12-28 00:21:04');
INSERT INTO `user_behavior` VALUES (156, 3, 'POST', 17, 'LIKE', 3, '2025-12-28 00:21:04');
INSERT INTO `user_behavior` VALUES (157, 3, 'POST', 17, 'FAVORITE', 5, '2025-12-28 00:21:04');
INSERT INTO `user_behavior` VALUES (158, 2, 'POST', 1, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (159, 2, 'POST', 1, 'LIKE', 3, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (160, 2, 'POST', 5, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (161, 2, 'POST', 5, 'LIKE', 3, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (162, 2, 'POST', 5, 'FAVORITE', 5, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (163, 2, 'POST', 13, 'VIEW', 1, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (164, 2, 'POST', 13, 'LIKE', 3, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (165, 2, 'POST', 22, 'VIEW', 1, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (166, 2, 'POST', 22, 'LIKE', 3, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (167, 2, 'POST', 22, 'COMMENT', 4, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (168, 3, 'POST', 2, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (169, 3, 'POST', 2, 'LIKE', 3, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (170, 3, 'POST', 2, 'FAVORITE', 5, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (171, 3, 'POST', 4, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (172, 3, 'POST', 4, 'LIKE', 3, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (173, 3, 'POST', 9, 'VIEW', 1, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (174, 3, 'POST', 9, 'LIKE', 3, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (175, 3, 'POST', 9, 'FAVORITE', 5, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (176, 3, 'POST', 18, 'VIEW', 1, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (177, 3, 'POST', 18, 'COMMENT', 4, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (178, 4, 'POST', 6, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (179, 4, 'POST', 6, 'LIKE', 3, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (180, 4, 'POST', 6, 'FAVORITE', 5, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (181, 4, 'POST', 11, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (182, 4, 'POST', 11, 'LIKE', 3, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (183, 4, 'POST', 15, 'VIEW', 1, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (184, 4, 'POST', 17, 'VIEW', 1, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (185, 4, 'POST', 17, 'LIKE', 3, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (186, 4, 'POST', 17, 'FAVORITE', 5, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (187, 6, 'POST', 21, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (188, 6, 'POST', 21, 'FAVORITE', 5, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (189, 6, 'POST', 23, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (190, 6, 'POST', 23, 'LIKE', 3, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (191, 6, 'POST', 23, 'FAVORITE', 5, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (192, 7, 'POST', 22, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (193, 7, 'POST', 22, 'FAVORITE', 5, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (194, 7, 'POST', 24, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (195, 7, 'POST', 24, 'LIKE', 3, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (196, 8, 'POST', 3, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (197, 8, 'POST', 3, 'FAVORITE', 5, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (198, 8, 'POST', 7, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (199, 8, 'POST', 7, 'LIKE', 3, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (200, 8, 'POST', 8, 'VIEW', 1, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (201, 8, 'POST', 14, 'VIEW', 1, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (202, 8, 'POST', 14, 'LIKE', 3, '2025-12-28 00:27:24');
INSERT INTO `user_behavior` VALUES (203, 9, 'POST', 4, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (204, 9, 'POST', 4, 'LIKE', 3, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (205, 9, 'POST', 10, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (206, 9, 'POST', 10, 'FAVORITE', 5, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (207, 9, 'POST', 15, 'VIEW', 1, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (208, 9, 'POST', 15, 'FAVORITE', 5, '2025-12-29 00:27:24');
INSERT INTO `user_behavior` VALUES (209, 10, 'POST', 12, 'VIEW', 1, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (210, 10, 'POST', 12, 'LIKE', 3, '2025-12-31 00:27:24');
INSERT INTO `user_behavior` VALUES (211, 10, 'POST', 17, 'VIEW', 1, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (212, 10, 'POST', 17, 'LIKE', 3, '2025-12-30 00:27:24');
INSERT INTO `user_behavior` VALUES (213, 11, 'POST', 2, 'VIEW', 1, '2026-01-01 00:33:25');
INSERT INTO `user_behavior` VALUES (214, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:35:07');
INSERT INTO `user_behavior` VALUES (215, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:36:39');
INSERT INTO `user_behavior` VALUES (216, 11, 'POST', 2, 'VIEW', 1, '2026-01-01 00:37:00');
INSERT INTO `user_behavior` VALUES (217, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:37:55');
INSERT INTO `user_behavior` VALUES (218, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:38:30');
INSERT INTO `user_behavior` VALUES (219, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:38:44');
INSERT INTO `user_behavior` VALUES (220, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:38:54');
INSERT INTO `user_behavior` VALUES (221, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:39:03');
INSERT INTO `user_behavior` VALUES (222, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:41:46');
INSERT INTO `user_behavior` VALUES (223, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:41:52');
INSERT INTO `user_behavior` VALUES (224, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:42:37');
INSERT INTO `user_behavior` VALUES (225, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:42:53');
INSERT INTO `user_behavior` VALUES (226, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:42:58');
INSERT INTO `user_behavior` VALUES (227, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 00:43:05');
INSERT INTO `user_behavior` VALUES (228, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:43:36');
INSERT INTO `user_behavior` VALUES (229, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 00:43:39');
INSERT INTO `user_behavior` VALUES (230, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 00:44:15');
INSERT INTO `user_behavior` VALUES (231, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 00:44:28');
INSERT INTO `user_behavior` VALUES (232, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 00:44:39');
INSERT INTO `user_behavior` VALUES (233, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 00:49:06');
INSERT INTO `user_behavior` VALUES (234, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 00:49:20');
INSERT INTO `user_behavior` VALUES (235, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:49:23');
INSERT INTO `user_behavior` VALUES (236, 11, 'POST', 25, 'VIEW', 1, '2026-01-01 00:49:26');
INSERT INTO `user_behavior` VALUES (237, 11, 'POST', 4, 'VIEW', 1, '2026-01-01 00:49:31');
INSERT INTO `user_behavior` VALUES (238, 11, 'POST', 23, 'VIEW', 1, '2026-01-01 00:51:14');
INSERT INTO `user_behavior` VALUES (239, 11, 'POST', 23, 'VIEW', 1, '2026-01-01 01:10:29');
INSERT INTO `user_behavior` VALUES (240, 11, 'POST', 7, 'VIEW', 1, '2026-01-01 01:10:33');
INSERT INTO `user_behavior` VALUES (241, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:11:24');
INSERT INTO `user_behavior` VALUES (242, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:12:38');
INSERT INTO `user_behavior` VALUES (243, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:12:51');
INSERT INTO `user_behavior` VALUES (244, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:13:05');
INSERT INTO `user_behavior` VALUES (245, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:13:15');
INSERT INTO `user_behavior` VALUES (246, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:13:25');
INSERT INTO `user_behavior` VALUES (247, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:13:40');
INSERT INTO `user_behavior` VALUES (248, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:14:05');
INSERT INTO `user_behavior` VALUES (249, 11, 'POST', 8, 'VIEW', 1, '2026-01-01 01:16:36');
INSERT INTO `user_behavior` VALUES (250, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 01:16:41');
INSERT INTO `user_behavior` VALUES (251, 11, 'POST', 12, 'VIEW', 1, '2026-01-01 01:18:07');
INSERT INTO `user_behavior` VALUES (252, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 01:18:15');
INSERT INTO `user_behavior` VALUES (253, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 01:18:38');
INSERT INTO `user_behavior` VALUES (254, 12, 'POST', 1, 'VIEW', 1, '2026-01-01 01:20:40');
INSERT INTO `user_behavior` VALUES (255, 12, 'POST', 1, 'VIEW', 1, '2026-01-01 01:42:29');
INSERT INTO `user_behavior` VALUES (256, 11, 'POST', 21, 'VIEW', 1, '2026-01-01 03:48:54');
INSERT INTO `user_behavior` VALUES (257, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 03:49:05');
INSERT INTO `user_behavior` VALUES (258, 11, 'POST', 4, 'VIEW', 1, '2026-01-01 03:49:47');
INSERT INTO `user_behavior` VALUES (259, 11, 'POST', 4, 'FAVORITE', 5, '2026-01-01 03:49:49');
INSERT INTO `user_behavior` VALUES (260, 11, 'POST', 21, 'VIEW', 1, '2026-01-01 03:50:40');
INSERT INTO `user_behavior` VALUES (261, 11, 'POST', 21, 'VIEW', 1, '2026-01-01 03:53:05');
INSERT INTO `user_behavior` VALUES (262, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 04:06:40');
INSERT INTO `user_behavior` VALUES (263, 12, 'POST', 1, 'VIEW', 1, '2026-01-01 04:06:49');
INSERT INTO `user_behavior` VALUES (264, 12, 'POST', 1, 'VIEW', 1, '2026-01-01 04:07:50');
INSERT INTO `user_behavior` VALUES (265, 12, 'POST', 7, 'VIEW', 1, '2026-01-01 04:08:05');
INSERT INTO `user_behavior` VALUES (266, 12, 'POST', 7, 'VIEW', 1, '2026-01-01 04:09:55');
INSERT INTO `user_behavior` VALUES (267, 12, 'POST', 6, 'VIEW', 1, '2026-01-01 04:24:05');
INSERT INTO `user_behavior` VALUES (268, 12, 'POST', 4, 'VIEW', 1, '2026-01-01 04:25:57');
INSERT INTO `user_behavior` VALUES (269, 12, 'POST', 4, 'VIEW', 1, '2026-01-01 04:26:33');
INSERT INTO `user_behavior` VALUES (270, 12, 'POST', 4, 'VIEW', 1, '2026-01-01 04:29:38');
INSERT INTO `user_behavior` VALUES (271, 12, 'POST', 4, 'VIEW', 1, '2026-01-01 04:29:42');
INSERT INTO `user_behavior` VALUES (272, 11, 'POST', 4, 'VIEW', 1, '2026-01-01 04:31:27');
INSERT INTO `user_behavior` VALUES (273, 11, 'POST', 4, 'VIEW', 1, '2026-01-01 04:31:34');
INSERT INTO `user_behavior` VALUES (274, 11, 'POST', 4, 'VIEW', 1, '2026-01-01 04:32:38');
INSERT INTO `user_behavior` VALUES (275, 12, 'POST', 4, 'VIEW', 1, '2026-01-01 04:35:07');
INSERT INTO `user_behavior` VALUES (276, 12, 'POST', 5, 'VIEW', 1, '2026-01-01 04:35:15');
INSERT INTO `user_behavior` VALUES (277, 12, 'POST', 4, 'VIEW', 1, '2026-01-01 04:35:25');
INSERT INTO `user_behavior` VALUES (278, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 04:36:05');
INSERT INTO `user_behavior` VALUES (279, 11, 'POST', 6, 'VIEW', 1, '2026-01-01 04:36:08');
INSERT INTO `user_behavior` VALUES (280, 11, 'POST', 2, 'VIEW', 1, '2026-01-01 04:36:10');
INSERT INTO `user_behavior` VALUES (281, 11, 'POST', 1, 'VIEW', 1, '2026-01-01 04:36:12');

-- ----------------------------
-- Table structure for user_collection
-- ----------------------------
DROP TABLE IF EXISTS `user_collection`;
CREATE TABLE `user_collection`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_type` enum('POST','WIKI') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_collection`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_collection_type`(`target_type` ASC) USING BTREE,
  CONSTRAINT `user_collection_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户收藏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_collection
-- ----------------------------
INSERT INTO `user_collection` VALUES (1, 11, 'POST', 4, '2026-01-01 03:49:49');

-- ----------------------------
-- Table structure for user_follow
-- ----------------------------
DROP TABLE IF EXISTS `user_follow`;
CREATE TABLE `user_follow`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL,
  `followee_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_follow_pair`(`follower_id` ASC, `followee_id` ASC) USING BTREE,
  INDEX `idx_follow_follower`(`follower_id` ASC) USING BTREE,
  INDEX `idx_follow_followee`(`followee_id` ASC) USING BTREE,
  CONSTRAINT `user_follow_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `user_follow_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户关注表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_follow
-- ----------------------------

-- ----------------------------
-- Table structure for user_block
-- ----------------------------
DROP TABLE IF EXISTS `user_block`;
CREATE TABLE `user_block`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blocker_id` bigint NOT NULL COMMENT '屏蔽者ID',
  `blocked_id` bigint NOT NULL COMMENT '被屏蔽者ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_block_pair`(`blocker_id` ASC, `blocked_id` ASC) USING BTREE,
  INDEX `idx_block_blocker`(`blocker_id` ASC) USING BTREE,
  INDEX `idx_block_blocked`(`blocked_id` ASC) USING BTREE,
  CONSTRAINT `user_block_ibfk_1` FOREIGN KEY (`blocker_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `user_block_ibfk_2` FOREIGN KEY (`blocked_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户屏蔽表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_block
-- ----------------------------

-- ----------------------------
-- Table structure for user_footprint
-- ----------------------------
DROP TABLE IF EXISTS `user_footprint`;
CREATE TABLE `user_footprint`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_type` enum('POST','WIKI') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `visited_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_footprint`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_footprint_visited`(`visited_at` ASC) USING BTREE,
  CONSTRAINT `user_footprint_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户足迹表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_footprint
-- ----------------------------
INSERT INTO `user_footprint` VALUES (3, 11, 'POST', 2, 'admin', '周末探店｜发现了一家宝藏精酿啤酒吧！十几种进口精酿随便选，老板还会根据你的口味推荐，服务超级贴心～强烈推荐IPA爱好者来试试！🍺 #精酿啤酒 #周末好去处', NULL, '2026-01-01 04:36:10');
INSERT INTO `user_footprint` VALUES (4, 11, 'POST', 25, 'demo1', '加微信', NULL, '2026-01-01 00:49:26');
INSERT INTO `user_footprint` VALUES (5, 11, 'POST', 1, 'admin', '今晚在Speak Low品尝了一杯经典的Old Fashioned，调酒师的手法真是炉火纯青！威士忌的醇厚配上橙皮的清香，完美！🥃✨ #鸡尾酒 #上海酒吧', NULL, '2026-01-01 04:36:12');
INSERT INTO `user_footprint` VALUES (6, 11, 'POST', 4, 'admin', '成都的酒吧文化真的太棒了！昨晚在玉林路的小酒馆听了一场live，氛围感拉满！配上几杯特调，这才是生活该有的样子～🎸🍻 #成都夜生活 #音乐酒吧', NULL, '2026-01-01 04:32:38');
INSERT INTO `user_footprint` VALUES (7, 11, 'POST', 23, '清酒控', '日本酒藏之旅｜参观了新潟的八海山酒藏，了解了清酒的酿造过程。从精米到发酵，每一步都充满匠心。回来带了几瓶限定酒！✈️🍶 #日本旅行 #清酒', NULL, '2026-01-01 01:10:30');
INSERT INTO `user_footprint` VALUES (8, 11, 'POST', 7, 'admin', '科普时间｜为什么鸡尾酒要用冰块摇匀而不是搅拌？其实这跟酒的成分有关系。含有果汁、奶油等不易混合的材料需要摇，而纯烈酒类则适合搅拌。涨知识了！📚 #鸡尾酒知识 ', NULL, NULL);
INSERT INTO `user_footprint` VALUES (9, 11, 'POST', 8, 'admin', '今天尝试了传说中的\"失身酒\"Long Island Iced Tea，四种基酒混合居然喝起来像冰茶？！后劲真的很大，大家喝的时候要注意哦～😵 #长岛冰茶 #鸡', NULL, '2026-01-01 01:16:37');
INSERT INTO `user_footprint` VALUES (10, 11, 'POST', 12, 'admin', '夏日特调推荐｜薄荷柠檬苏打，清爽解暑！做法超简单：新鲜薄荷+柠檬汁+苏打水+冰块，完美！🍋🌿 #夏日饮品 #清爽 #DIY', NULL, NULL);
INSERT INTO `user_footprint` VALUES (11, 12, 'POST', 1, 'admin', '今晚在Speak Low品尝了一杯经典的Old Fashioned，调酒师的手法真是炉火纯青！威士忌的醇厚配上橙皮的清香，完美！🥃✨ #鸡尾酒 #上海酒吧', NULL, '2026-01-01 04:07:50');
INSERT INTO `user_footprint` VALUES (12, 11, 'POST', 21, '清酒控', '清酒入门｜獭祭二割三分，精米步合23%，果香浓郁，入口绵柔。虽然价格不菲，但品质确实出众。适合清酒入门者的第一瓶大吟酿！🍶 #清酒 #獭祭', NULL, '2026-01-01 03:53:05');
INSERT INTO `user_footprint` VALUES (13, 12, 'POST', 7, 'admin', '科普时间｜为什么鸡尾酒要用冰块摇匀而不是搅拌？其实这跟酒的成分有关系。含有果汁、奶油等不易混合的材料需要摇，而纯烈酒类则适合搅拌。涨知识了！📚 #鸡尾酒知识 ', NULL, '2026-01-01 04:09:56');
INSERT INTO `user_footprint` VALUES (14, 12, 'POST', 6, 'admin', '杭州西湖边的这家酒吧真的绝了！坐在露台上，一边欣赏湖景，一边品着红酒，人生惬意不过如此。强烈推荐日落时分来，景色美到窒息！🌅🍷 #杭州 #西湖 #红酒', NULL, NULL);
INSERT INTO `user_footprint` VALUES (15, 12, 'POST', 4, 'admin', '成都的酒吧文化真的太棒了！昨晚在玉林路的小酒馆听了一场live，氛围感拉满！配上几杯特调，这才是生活该有的样子～🎸🍻 #成都夜生活 #音乐酒吧', NULL, '2026-01-01 04:35:25');
INSERT INTO `user_footprint` VALUES (16, 12, 'POST', 5, 'admin', '威士忌品鉴笔记｜今天品尝了三款单一麦芽威士忌，从艾雷岛的泥煤味到斯佩塞的果香，每一款都有独特的风味。最喜欢的还是Highland Park 12年，平衡感极佳！', NULL, NULL);
INSERT INTO `user_footprint` VALUES (17, 11, 'POST', 6, 'admin', '杭州西湖边的这家酒吧真的绝了！坐在露台上，一边欣赏湖景，一边品着红酒，人生惬意不过如此。强烈推荐日落时分来，景色美到窒息！🌅🍷 #杭州 #西湖 #红酒', NULL, NULL);

-- ----------------------------
-- Table structure for user_preference
-- ----------------------------
DROP TABLE IF EXISTS `user_preference`;
CREATE TABLE `user_preference`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag_category` enum('BEVERAGE_TYPE','TASTE','SCENE','LOCATION','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER',
  `weight` decimal(10, 4) NOT NULL DEFAULT 0.0000 COMMENT '偏好权重，根据行为累计',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_preference`(`user_id` ASC, `tag_name` ASC) USING BTREE,
  INDEX `idx_user_preference_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_preference_tag`(`tag_name` ASC) USING BTREE,
  INDEX `idx_user_preference_weight`(`weight` DESC) USING BTREE,
  CONSTRAINT `user_preference_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 163 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户偏好表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_preference
-- ----------------------------
INSERT INTO `user_preference` VALUES (1, 1, '威士忌', 'BEVERAGE_TYPE', 2.5000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (2, 1, '鸡尾酒', 'BEVERAGE_TYPE', 1.8000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (3, 1, '醇厚', 'TASTE', 1.5000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (4, 1, '烟熏', 'TASTE', 0.8000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (5, 1, '品鉴', 'SCENE', 1.2000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (6, 1, '酒吧', 'LOCATION', 0.9000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (7, 1, '朗姆酒', 'BEVERAGE_TYPE', 0.6000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (8, 1, '清爽', 'TASTE', 0.5000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (9, 2, '啤酒', 'BEVERAGE_TYPE', 2.8000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (10, 2, '聚会', 'SCENE', 2.2000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (11, 2, '清爽', 'TASTE', 1.5250, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (12, 2, '酒吧', 'LOCATION', 1.2000, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (13, 2, '鸡尾酒', 'BEVERAGE_TYPE', 0.8600, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (14, 2, '家中', 'LOCATION', 0.6700, '2025-12-31 23:12:52');
INSERT INTO `user_preference` VALUES (22, 3, '红酒', 'BEVERAGE_TYPE', 2.6000, '2025-12-31 23:13:42');
INSERT INTO `user_preference` VALUES (23, 3, '约会', 'SCENE', 2.0000, '2025-12-31 23:13:42');
INSERT INTO `user_preference` VALUES (24, 3, '醇厚', 'TASTE', 1.4000, '2025-12-31 23:13:42');
INSERT INTO `user_preference` VALUES (25, 3, '花香', 'TASTE', 1.0000, '2025-12-31 23:13:42');
INSERT INTO `user_preference` VALUES (26, 3, '甘甜', 'TASTE', 0.9000, '2025-12-31 23:13:42');
INSERT INTO `user_preference` VALUES (27, 3, '酒吧', 'LOCATION', 0.8000, '2025-12-31 23:13:42');
INSERT INTO `user_preference` VALUES (28, 3, '鸡尾酒', 'BEVERAGE_TYPE', 0.5000, '2025-12-31 23:13:42');
INSERT INTO `user_preference` VALUES (106, 2, '威士忌', 'BEVERAGE_TYPE', 2.8000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (107, 2, '醇厚', 'TASTE', 2.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (108, 2, '烟熏', 'TASTE', 1.2000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (109, 2, '品鉴', 'SCENE', 1.8000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (110, 3, '啤酒', 'BEVERAGE_TYPE', 2.8000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (111, 3, '聚会', 'SCENE', 2.2000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (112, 3, '清爽', 'TASTE', 1.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (113, 3, '家中', 'LOCATION', 0.6000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (114, 4, '红酒', 'BEVERAGE_TYPE', 2.6000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (115, 4, '约会', 'SCENE', 2.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (116, 4, '醇厚', 'TASTE', 1.4000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (117, 4, '花香', 'TASTE', 1.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (118, 4, '甘甜', 'TASTE', 0.9000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (119, 4, '酒吧', 'LOCATION', 0.8000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (120, 4, '鸡尾酒', 'BEVERAGE_TYPE', 0.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (121, 6, '清酒', 'BEVERAGE_TYPE', 3.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (122, 6, '甘甜', 'TASTE', 1.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (123, 6, '果香', 'TASTE', 1.2000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (124, 6, '品鉴', 'SCENE', 1.8000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (125, 7, '白酒', 'BEVERAGE_TYPE', 3.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (126, 7, '酱香', 'TASTE', 2.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (127, 7, '醇厚', 'TASTE', 2.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (128, 7, '品鉴', 'SCENE', 1.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (129, 8, '鸡尾酒', 'BEVERAGE_TYPE', 2.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (130, 8, '清爽', 'TASTE', 1.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (131, 8, '甘甜', 'TASTE', 1.2000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (132, 8, '家中', 'LOCATION', 1.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (133, 9, '酒吧', 'LOCATION', 3.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (134, 9, '鸡尾酒', 'BEVERAGE_TYPE', 2.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (135, 9, '聚会', 'SCENE', 1.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (136, 9, '约会', 'SCENE', 1.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (137, 10, '鸡尾酒', 'BEVERAGE_TYPE', 1.8000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (138, 10, '清爽', 'TASTE', 1.5000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (139, 10, '花香', 'TASTE', 1.2000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (140, 10, '家中', 'LOCATION', 1.0000, '2026-01-01 00:27:24');
INSERT INTO `user_preference` VALUES (145, 11, '啤酒', 'BEVERAGE_TYPE', 0.2853, '2026-01-01 00:33:25');
INSERT INTO `user_preference` VALUES (146, 11, '清爽', 'TASTE', 0.2853, '2026-01-01 00:33:25');
INSERT INTO `user_preference` VALUES (147, 11, '聚会', 'SCENE', 0.9193, '2026-01-01 00:33:25');
INSERT INTO `user_preference` VALUES (148, 11, '酒吧', 'LOCATION', 0.9733, '2026-01-01 00:33:25');
INSERT INTO `user_preference` VALUES (149, 11, '威士忌', 'BEVERAGE_TYPE', 0.9193, '2026-01-01 00:43:05');
INSERT INTO `user_preference` VALUES (150, 11, '醇厚', 'TASTE', 0.8025, '2026-01-01 00:43:05');
INSERT INTO `user_preference` VALUES (151, 11, '鸡尾酒', 'BEVERAGE_TYPE', 1.3529, '2026-01-01 00:43:05');
INSERT INTO `user_preference` VALUES (152, 11, '伏特加', 'BEVERAGE_TYPE', 0.8624, '2026-01-01 00:51:14');
INSERT INTO `user_preference` VALUES (153, 11, '白兰地', 'BEVERAGE_TYPE', 0.1950, '2026-01-01 00:51:14');
INSERT INTO `user_preference` VALUES (154, 11, '白酒', 'BEVERAGE_TYPE', 0.1950, '2026-01-01 00:51:14');
INSERT INTO `user_preference` VALUES (155, 11, '品鉴', 'SCENE', 0.1000, '2026-01-01 01:10:33');
INSERT INTO `user_preference` VALUES (156, 11, '朗姆酒', 'BEVERAGE_TYPE', 0.7395, '2026-01-01 01:11:24');
INSERT INTO `user_preference` VALUES (157, 11, '金酒', 'BEVERAGE_TYPE', 0.7395, '2026-01-01 01:11:24');
INSERT INTO `user_preference` VALUES (158, 11, '家中', 'LOCATION', 0.1000, '2026-01-01 01:18:07');
INSERT INTO `user_preference` VALUES (159, 12, '威士忌', 'BEVERAGE_TYPE', 0.1950, '2026-01-01 01:20:40');
INSERT INTO `user_preference` VALUES (160, 12, '酒吧', 'LOCATION', 0.1950, '2026-01-01 01:20:40');
INSERT INTO `user_preference` VALUES (161, 12, '醇厚', 'TASTE', 0.1950, '2026-01-01 01:20:40');
INSERT INTO `user_preference` VALUES (162, 12, '鸡尾酒', 'BEVERAGE_TYPE', 0.1950, '2026-01-01 01:20:40');

-- ----------------------------
-- Table structure for wiki_discussion
-- ----------------------------
DROP TABLE IF EXISTS `wiki_discussion`;
CREATE TABLE `wiki_discussion`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `page_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_discussion_page`(`page_id` ASC) USING BTREE,
  CONSTRAINT `wiki_discussion_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `wiki_page` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `wiki_discussion_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维基讨论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wiki_discussion
-- ----------------------------

-- ----------------------------
-- Table structure for wiki_image_data
-- ----------------------------
DROP TABLE IF EXISTS `wiki_image_data`;
CREATE TABLE `wiki_image_data`  (
  `image_id` bigint NOT NULL,
  `image_data` longblob NOT NULL,
  PRIMARY KEY (`image_id`) USING BTREE,
  CONSTRAINT `wiki_image_data_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维基图片数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wiki_image_data
-- ----------------------------

-- ----------------------------
-- Table structure for wiki_page
-- ----------------------------
DROP TABLE IF EXISTS `wiki_page`;
CREATE TABLE `wiki_page`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `slug` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('DRAFT','UNDER_REVIEW','PUBLISHED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'UNDER_REVIEW',
  `last_editor_id` bigint NULL DEFAULT NULL,
  `last_editor_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `slug`(`slug` ASC) USING BTREE,
  INDEX `idx_wiki_title`(`title` ASC) USING BTREE,
  INDEX `last_editor_id`(`last_editor_id` ASC) USING BTREE,
  CONSTRAINT `wiki_page_ibfk_1` FOREIGN KEY (`last_editor_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维基页面表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wiki_page
-- ----------------------------

-- ----------------------------
-- Table structure for wiki_revision
-- ----------------------------
DROP TABLE IF EXISTS `wiki_revision`;
CREATE TABLE `wiki_revision`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `page_id` bigint NOT NULL,
  `editor_id` bigint NULL DEFAULT NULL,
  `editor_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `editor_id`(`editor_id` ASC) USING BTREE,
  INDEX `idx_revision_page`(`page_id` ASC) USING BTREE,
  CONSTRAINT `wiki_revision_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `wiki_page` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `wiki_revision_ibfk_2` FOREIGN KEY (`editor_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维基修订历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of wiki_revision
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
