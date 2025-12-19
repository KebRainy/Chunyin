-- ====================================
-- 数据库表结构定义
-- ====================================
-- 项目：醇饮平台 (Beverage Platform)
-- 说明：此文件仅包含表结构定义，不包含测试数据
-- 使用方法：mysql -u root -p beverage_platform < database/schema.sql
-- ====================================

-- 使用UTF-8编码
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ====================================
-- 图片管理相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片元数据表';

CREATE TABLE IF NOT EXISTS general_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用图片数据表';

CREATE TABLE IF NOT EXISTS post_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态图片数据表';

CREATE TABLE IF NOT EXISTS avatar_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='头像图片数据表';

CREATE TABLE IF NOT EXISTS wiki_image_data (
    image_id BIGINT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维基图片数据表';

-- ====================================
-- 用户管理相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 添加外键约束（在user表创建后）
ALTER TABLE image ADD CONSTRAINT fk_image_uploaded_by 
    FOREIGN KEY (uploaded_by) REFERENCES user(id) ON DELETE SET NULL;

-- ====================================
-- 酒类饮品相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='酒类饮品表';

CREATE TABLE IF NOT EXISTS beverage_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    beverage_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    image_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE,
    INDEX idx_beverage_id (beverage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮品图片关联表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE IF NOT EXISTS comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_comment_user (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    beverage_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_beverage (user_id, beverage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

CREATE TABLE IF NOT EXISTS beverage_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    beverage_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    FOREIGN KEY (beverage_id) REFERENCES beverage(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    UNIQUE KEY uk_beverage_tag (beverage_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮品标签关联表';

-- ====================================
-- 外部链接和公告相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部链接表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- ====================================
-- 动态分享相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态分享表';

CREATE TABLE IF NOT EXISTS share_post_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_post_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    image_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (share_post_id) REFERENCES share_post(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE,
    INDEX idx_share_post_id (share_post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态图片关联表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态评论表';

CREATE TABLE IF NOT EXISTS share_post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES share_post(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_post_like (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态点赞表';

-- ====================================
-- 维基相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维基页面表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维基修订历史表';

CREATE TABLE IF NOT EXISTS wiki_discussion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    page_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (page_id) REFERENCES wiki_page(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_discussion_page (page_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维基讨论表';

-- ====================================
-- 社交功能相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信表';

CREATE TABLE IF NOT EXISTS user_collection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type ENUM('POST','WIKI') NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_collection (user_id, target_type, target_id),
    INDEX idx_collection_type (target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户足迹表';

-- ====================================
-- 每日一题相关表
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日一题表';

CREATE TABLE IF NOT EXISTS daily_question_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    selected_option TINYINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES daily_question(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_daily_answer (question_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日一题答案表';

-- ====================================
-- 酒吧相关表 (SF-7)
-- ====================================

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='酒吧申请表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='酒吧表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='酒吧评价表';

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================
-- 数据库初始化完成
-- ====================================

