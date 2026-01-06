-- 增量补丁：新增维基历史版本表（wiki_revision）
-- 适用场景：你的数据库是旧版本，缺失 wiki_revision，导致维基编辑/统计接口 500

DROP TABLE IF EXISTS `wiki_revision`;
CREATE TABLE `wiki_revision`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `page_id` bigint(20) NOT NULL COMMENT '维基页面ID',
  `editor_id` bigint(20) NULL DEFAULT NULL COMMENT '编辑者ID（系统为NULL）',
  `editor_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '编辑者名称',
  `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '编辑摘要',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本内容快照',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wiki_revision_page`(`page_id` ASC) USING BTREE,
  INDEX `idx_wiki_revision_editor`(`editor_id` ASC) USING BTREE,
  CONSTRAINT `wiki_revision_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `wiki_page` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `wiki_revision_ibfk_2` FOREIGN KEY (`editor_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '维基历史版本表' ROW_FORMAT = DYNAMIC;

