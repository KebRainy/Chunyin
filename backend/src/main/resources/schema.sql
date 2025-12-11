CREATE TABLE IF NOT EXISTS image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    image_data LONGBLOB NOT NULL,
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    file_size INT,
    uploaded_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_uploaded_by (uploaded_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
