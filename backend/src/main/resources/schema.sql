CREATE TABLE IF NOT EXISTS image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    file_size INT,
    uploaded_by BIGINT,
    category ENUM('GENERAL','POST','AVATAR','WIKI') DEFAULT 'GENERAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_uploaded_by (uploaded_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @image_uuid_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'uuid'
);

SET @add_image_uuid = IF(
    @image_uuid_exists = 0,
    'ALTER TABLE image ADD COLUMN uuid VARCHAR(36) AFTER id',
    'SELECT 1'
);

PREPARE stmt FROM @add_image_uuid;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE image SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';

SET @uuid_nullable = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'uuid'
      AND IS_NULLABLE = 'YES'
);

SET @set_uuid_not_null = IF(
    @uuid_nullable = 0,
    'SELECT 1',
    'ALTER TABLE image MODIFY COLUMN uuid VARCHAR(36) NOT NULL'
);

PREPARE stmt FROM @set_uuid_not_null;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uuid_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'uuid'
      AND NON_UNIQUE = 0
);

SET @add_uuid_unique = IF(
    @uuid_unique_exists = 0,
    'ALTER TABLE image ADD UNIQUE KEY uk_image_uuid (uuid)',
    'SELECT 1'
);

PREPARE stmt FROM @add_uuid_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @image_filename_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'filename'
);

SET @rename_image_filename = IF(
    @image_filename_exists = 0,
    'SELECT 1',
    'ALTER TABLE image CHANGE COLUMN filename file_name VARCHAR(255)'
);

PREPARE stmt FROM @rename_image_filename;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @image_content_type_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'content_type'
);

SET @rename_image_content_type = IF(
    @image_content_type_exists = 0,
    'SELECT 1',
    'ALTER TABLE image CHANGE COLUMN content_type mime_type VARCHAR(100)'
);

PREPARE stmt FROM @rename_image_content_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @image_mime_type_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'mime_type'
);

SET @add_image_mime_type = IF(
    @image_mime_type_exists = 0,
    'ALTER TABLE image ADD COLUMN mime_type VARCHAR(100) AFTER file_name',
    'SELECT 1'
);

PREPARE stmt FROM @add_image_mime_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @image_category_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'category'
);

SET @add_image_category = IF(
    @image_category_exists = 0,
    'ALTER TABLE image ADD COLUMN category ENUM(''GENERAL'',''POST'',''AVATAR'',''WIKI'') DEFAULT ''GENERAL'' AFTER uploaded_by',
    'SELECT 1'
);

PREPARE stmt FROM @add_image_category;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE image SET category = 'GENERAL' WHERE category IS NULL;

CREATE TABLE IF NOT EXISTS general_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS avatar_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wiki_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @image_data_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND COLUMN_NAME = 'image_data'
);

SET @migrate_image_data = IF(
    @image_data_column_exists = 0,
    'SELECT 1',
    'INSERT INTO general_image_data (image_id, image_data) SELECT id, image_data FROM image WHERE image_data IS NOT NULL ON DUPLICATE KEY UPDATE image_data = VALUES(image_data)'
);

PREPARE stmt FROM @migrate_image_data;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_image_data_column = IF(
    @image_data_column_exists = 0,
    'SELECT 1',
    'ALTER TABLE image DROP COLUMN image_data'
);

PREPARE stmt FROM @drop_image_data_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('USER', 'SELLER', 'ADMIN') NOT NULL DEFAULT 'USER',
    avatar_image_id BIGINT,
    avatar_url VARCHAR(255),
    bio TEXT,
    gender ENUM('MALE', 'FEMALE', 'SECRET') DEFAULT 'SECRET',
    birthday DATE NULL,
    level INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (avatar_image_id) REFERENCES image(id) ON DELETE SET NULL,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @avatar_url_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'avatar_url'
);

SET @add_avatar_url = IF(
    @avatar_url_exists = 0,
    'ALTER TABLE user ADD COLUMN avatar_url VARCHAR(255) AFTER avatar_image_id',
    'SELECT 1'
);

PREPARE stmt FROM @add_avatar_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @level_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'level'
);

SET @add_level = IF(
    @level_exists = 0,
    'ALTER TABLE user ADD COLUMN level INT DEFAULT 1 AFTER birthday',
    'SELECT 1'
);

PREPARE stmt FROM @add_level;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @needs_birthday_conversion = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'birthday'
      AND DATA_TYPE <> 'date'
);

SET @normalize_birthday = IF(
    @needs_birthday_conversion = 0,
    'SELECT 1',
    'UPDATE user SET birthday = NULL WHERE birthday IS NOT NULL AND birthday NOT REGEXP ''^[0-9]{4}-[0-9]{2}-[0-9]{2}$'''
);

PREPARE stmt FROM @normalize_birthday;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @convert_birthday = IF(
    @needs_birthday_conversion = 0,
    'SELECT 1',
    'ALTER TABLE user MODIFY COLUMN birthday DATE NULL'
);

PREPARE stmt FROM @convert_birthday;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE user SET level = 1 WHERE level IS NULL;
UPDATE user SET gender = UPPER(gender) WHERE gender IS NOT NULL;

SET @gender_is_enum = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'gender'
      AND DATA_TYPE = 'enum'
);

SET @alter_gender = IF(
    @gender_is_enum = 1,
    'SELECT 1',
    'ALTER TABLE user MODIFY COLUMN gender ENUM(''MALE'',''FEMALE'',''SECRET'') DEFAULT ''SECRET'''
);

PREPARE stmt FROM @alter_gender;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_image_uploaded_by_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'image'
      AND CONSTRAINT_NAME = 'fk_image_uploaded_by'
);

SET @add_fk_image_uploaded_by = IF(
    @fk_image_uploaded_by_exists = 0,
    'ALTER TABLE image ADD CONSTRAINT fk_image_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES user(id) ON DELETE SET NULL',
    'SELECT 1'
);

PREPARE stmt FROM @add_fk_image_uploaded_by;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS beverage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    type VARCHAR(50) NOT NULL,
    origin VARCHAR(100),
    alcohol_content DECIMAL(4,2),
    volume VARCHAR(50),
    description TEXT,
    ingredients TEXT,
    taste_notes TEXT,
    cover_image_id BIGINT,
    rating DECIMAL(3,2) DEFAULT 0.00,
    rating_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (cover_image_id) REFERENCES image(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_name (name),
    INDEX idx_type (type),
    INDEX idx_rating (rating),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS beverage_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    beverage_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    image_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE,
    INDEX idx_beverage_id (beverage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    beverage_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    rating DECIMAL(2,1),
    like_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'APPROVED',
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE,
    INDEX idx_beverage_id (beverage_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_comment_user (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS external_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    beverage_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    platform VARCHAR(50),
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP NULL,
    reject_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_beverage_id (beverage_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    event_type VARCHAR(50),
    location VARCHAR(200),
    event_date TIMESTAMP NULL,
    cover_image_id BIGINT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP NULL,
    reject_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    view_count INT DEFAULT 0,
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (cover_image_id) REFERENCES image(id) ON DELETE SET NULL,
    FOREIGN KEY (reviewed_by) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_event_date (event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    beverage_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_beverage (user_id, beverage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS beverage_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    beverage_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    UNIQUE KEY uk_beverage_tag (beverage_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS share_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    location VARCHAR(255),
    tags VARCHAR(255),
    ip_address VARCHAR(64),
    ip_region VARCHAR(128),
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_share_post_user (user_id),
    INDEX idx_share_post_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @share_post_ip_region_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'ip_region'
);

SET @add_share_post_ip_region = IF(
    @share_post_ip_region_exists = 0,
    'ALTER TABLE share_post ADD COLUMN ip_region VARCHAR(128) AFTER ip_address',
    'SELECT 1'
);

PREPARE stmt FROM @add_share_post_ip_region;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @share_post_tags_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'tags'
);

SET @add_share_post_tags = IF(
    @share_post_tags_exists = 0,
    'ALTER TABLE share_post ADD COLUMN tags VARCHAR(255) AFTER location',
    'SELECT 1'
);

PREPARE stmt FROM @add_share_post_tags;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @share_post_view_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'view_count'
);

SET @add_share_post_view_count = IF(
    @share_post_view_count_exists = 0,
    'ALTER TABLE share_post ADD COLUMN view_count INT DEFAULT 0 AFTER tags',
    'SELECT 1'
);

PREPARE stmt FROM @add_share_post_view_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @share_post_like_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'like_count'
);

SET @add_share_post_like_count = IF(
    @share_post_like_count_exists = 0,
    'ALTER TABLE share_post ADD COLUMN like_count INT DEFAULT 0 AFTER view_count',
    'SELECT 1'
);

PREPARE stmt FROM @add_share_post_like_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @share_post_favorite_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'favorite_count'
);

SET @add_share_post_favorite_count = IF(
    @share_post_favorite_count_exists = 0,
    'ALTER TABLE share_post ADD COLUMN favorite_count INT DEFAULT 0 AFTER like_count',
    'SELECT 1'
);

PREPARE stmt FROM @add_share_post_favorite_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @share_post_comment_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'comment_count'
);

SET @add_share_post_comment_count = IF(
    @share_post_comment_count_exists = 0,
    'ALTER TABLE share_post ADD COLUMN comment_count INT DEFAULT 0 AFTER favorite_count',
    'SELECT 1'
);

PREPARE stmt FROM @add_share_post_comment_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @share_post_image_urls_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'image_urls'
);

SET @drop_share_post_image_urls = IF(
    @share_post_image_urls_exists = 0,
    'SELECT 1',
    'ALTER TABLE share_post DROP COLUMN image_urls'
);

PREPARE stmt FROM @drop_share_post_image_urls;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @share_post_image_ids_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'share_post'
      AND COLUMN_NAME = 'image_ids'
);

SET @drop_share_post_image_ids = IF(
    @share_post_image_ids_exists = 0,
    'SELECT 1',
    'ALTER TABLE share_post DROP COLUMN image_ids'
);

PREPARE stmt FROM @drop_share_post_image_ids;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE share_post SET view_count = 0 WHERE view_count IS NULL;
UPDATE share_post SET like_count = 0 WHERE like_count IS NULL;
UPDATE share_post SET favorite_count = 0 WHERE favorite_count IS NULL;
UPDATE share_post SET comment_count = 0 WHERE comment_count IS NULL;

CREATE TABLE IF NOT EXISTS share_post_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_post_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    image_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (share_post_id) REFERENCES share_post(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE,
    INDEX idx_share_post_id (share_post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS share_post_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    like_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES share_post(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES share_post_comment(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS share_post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES share_post(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_post_like (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wiki_page (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slug VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content LONGTEXT NOT NULL,
    status ENUM('DRAFT','UNDER_REVIEW','PUBLISHED') DEFAULT 'UNDER_REVIEW',
    last_editor_id BIGINT,
    last_editor_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_wiki_title (title),
    FOREIGN KEY (last_editor_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wiki_revision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    page_id BIGINT NOT NULL,
    editor_id BIGINT,
    editor_name VARCHAR(100),
    summary VARCHAR(500),
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (page_id) REFERENCES wiki_page(id) ON DELETE CASCADE,
    FOREIGN KEY (editor_id) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_revision_page (page_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wiki_discussion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    page_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (page_id) REFERENCES wiki_page(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_discussion_page (page_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (follower_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_follow_pair (follower_id, followee_id),
    INDEX idx_follow_follower (follower_id),
    INDEX idx_follow_followee (followee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS private_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_message_sender (sender_id),
    INDEX idx_message_receiver (receiver_id),
    INDEX idx_message_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_collection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type ENUM('POST','WIKI') NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_collection (user_id, target_type, target_id),
    INDEX idx_collection_type (target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_footprint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type ENUM('POST','WIKI') NOT NULL,
    target_id BIGINT NOT NULL,
    title VARCHAR(255),
    summary VARCHAR(500),
    cover_url VARCHAR(255),
    visited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_footprint (user_id, target_type, target_id),
    INDEX idx_footprint_visited (visited_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS daily_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_date DATE NOT NULL UNIQUE,
    question VARCHAR(255) NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_option TINYINT NOT NULL,
    count_a INT DEFAULT 0,
    count_b INT DEFAULT 0,
    count_c INT DEFAULT 0,
    count_d INT DEFAULT 0,
    explanation TEXT,
    wiki_link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS daily_question_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    selected_option TINYINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES daily_question(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_daily_answer (question_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 检查并删除旧的 bar 相关表，重新创建正确的表结构
-- 删除顺序：bar_review -> bar -> bar_application（因为外键依赖）
DROP TABLE IF EXISTS bar_review;
DROP TABLE IF EXISTS bar;
DROP TABLE IF EXISTS bar_application;

-- 创建酒吧申请表（新表）
CREATE TABLE IF NOT EXISTS bar_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50),
    opening_time TIME,
    closing_time TIME,
    contact_phone VARCHAR(20) NOT NULL,
    description TEXT,
    main_beverages VARCHAR(200),
    applicant_id BIGINT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    review_note TEXT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (applicant_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES user(id) ON DELETE SET NULL,
    INDEX idx_application_status (status),
    INDEX idx_application_applicant (applicant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建正式酒吧表（只包含审核通过的酒吧）
CREATE TABLE IF NOT EXISTS bar (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT UNIQUE,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    opening_time TIME,
    closing_time TIME,
    contact_phone VARCHAR(20) NOT NULL,
    description TEXT,
    main_beverages VARCHAR(200),
    owner_id BIGINT NOT NULL,
    avg_rating DECIMAL(3,2) DEFAULT 0.00,
    review_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (application_id) REFERENCES bar_application(id) ON DELETE SET NULL,
    FOREIGN KEY (owner_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_bar_city (city),
    INDEX idx_bar_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS bar_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bar_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL COMMENT '评分 1-5',
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (bar_id) REFERENCES bar(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_bar_user_review (bar_id, user_id, is_active),
    INDEX idx_bar_review_bar (bar_id),
    INDEX idx_bar_review_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试数据：酒吧（仅在表为空时插入）
INSERT IGNORE INTO bar (id, name, address, province, city, district, latitude, longitude, opening_time, closing_time, contact_phone, description, main_beverages, owner_id, avg_rating, review_count, created_at)
VALUES 
-- 上海酒吧
(1, 'Speak Low 秘密客', '上海市黄浦区复兴中路579号', '上海市', '上海市', '黄浦区', 31.2231, 121.4737, '18:00:00', '02:00:00', '021-64012399', '隐藏在理发店后的秘密酒吧，以经典鸡尾酒和创意调酒闻名。', '鸡尾酒、威士忌', 1, 4.5, 0, NOW()),
(2, 'Union Trading Company', '上海市黄浦区圆明园路169号协进大楼', '上海市', '上海市', '黄浦区', 31.2425, 121.4897, '17:00:00', '02:00:00', '021-60723428', '上海经典鸡尾酒吧，氛围优雅，适合商务聚会。', '鸡尾酒、金酒', 1, 4.3, 0, NOW()),
(3, 'The Nest 巢', '上海市静安区铜仁路90弄4号', '上海市', '上海市', '静安区', 31.2304, 121.4520, '19:00:00', '03:00:00', '021-52376677', '位于老洋房的屋顶酒吧，视野开阔，调酒师技艺精湛。', '鸡尾酒、朗姆酒', 1, 4.6, 0, NOW()),

-- 北京酒吧
(4, 'Janes & Hooch', '北京市朝阳区工人体育场北路4号', '北京市', '北京市', '朝阳区', 39.9289, 116.4473, '18:00:00', '02:00:00', '010-64159871', '工体附近的时尚酒吧，经常有DJ表演。', '鸡尾酒、伏特加', 1, 4.2, 0, NOW()),
(5, 'Modo Urban Deli', '北京市东城区五道营胡同19号', '北京市', '北京市', '东城区', 39.9456, 116.4106, '11:00:00', '23:00:00', '010-64025805', '胡同里的创意餐酒吧，白天是咖啡馆，晚上是酒吧。', '精酿啤酒、葡萄酒', 1, 4.4, 0, NOW()),

-- 广州酒吧
(6, 'Hope & Sesame 希望与芝麻', '广州市天河区天河路228号正佳广场', '广东省', '广州市', '天河区', 23.1367, 113.3230, '17:00:00', '02:00:00', '020-38732288', '屡获殊荣的鸡尾酒吧，以中式元素融合西方调酒技艺著称。', '鸡尾酒、白酒', 1, 4.7, 0, NOW()),
(7, 'The Happy Monk', '广州市天河区天河北路都市华庭天怡阁', '广东省', '广州市', '天河区', 23.1486, 113.3267, '12:00:00', '02:00:00', '020-38731535', '比利时风格酒吧，提供丰富的精酿啤酒选择。', '精酿啤酒、比利时啤酒', 1, 4.1, 0, NOW()),

-- 成都酒吧
(8, 'Jing Bar 廊桥', '成都市锦江区下东大街段166号', '四川省', '成都市', '锦江区', 30.6598, 104.0861, '18:00:00', '02:00:00', '028-86259999', '现代中式风格的鸡尾酒吧，环境优雅。', '鸡尾酒、中国烈酒', 1, 4.3, 0, NOW()),
(9, 'Tipsy 微醺', '成都市武侯区玉林西路', '四川省', '成都市', '武侯区', 30.6409, 104.0431, '19:00:00', '03:00:00', '028-85555678', '玉林路上的小酒馆，氛围轻松，价格亲民。', '精酿啤酒、鸡尾酒', 1, 4.0, 0, NOW()),

-- 杭州酒吧
(10, 'Chez Pop', '杭州市上城区南山路200号', '浙江省', '杭州市', '上城区', 30.2489, 120.1363, '17:00:00', '01:00:00', '0571-87065890', '西湖边的小资酒吧，适合约会和聚会。', '葡萄酒、鸡尾酒', 1, 4.2, 0, NOW());