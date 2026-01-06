-- Upgrade message_policy enum options for DM privacy settings
ALTER TABLE `user`
MODIFY COLUMN `message_policy` enum('ALL','FOLLOWEES_ONLY','LIMIT_ONE_BEFORE_REPLY_OR_FOLLOW','FOLLOWERS_ONLY','NONE')
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'LIMIT_ONE_BEFORE_REPLY_OR_FOLLOW' COMMENT '私信接收策略';

UPDATE `user`
SET `message_policy` = 'LIMIT_ONE_BEFORE_REPLY_OR_FOLLOW'
WHERE `message_policy` IS NULL OR `message_policy` = 'ALL';
