CREATE TABLE IF NOT EXISTS iam_account (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  account_no VARCHAR(32) NOT NULL,
  account_type VARCHAR(20) NOT NULL,
  login_name VARCHAR(80),
  phone VARCHAR(20),
  email VARCHAR(120),
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  last_login_at DATETIME,
  last_login_ip VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_iam_account_no (account_no),
  UNIQUE KEY uk_iam_account_phone (phone),
  UNIQUE KEY uk_iam_account_email (email)
);

CREATE TABLE IF NOT EXISTS iam_verification_code (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  target VARCHAR(120) NOT NULL,
  scene VARCHAR(40) NOT NULL,
  code VARCHAR(12) NOT NULL,
  expire_at DATETIME NOT NULL,
  used_at DATETIME,
  status VARCHAR(20) NOT NULL DEFAULT 'unused',
  send_channel VARCHAR(30) NOT NULL DEFAULT 'mock_console',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS iam_role (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(40) NOT NULL,
  role_name VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_iam_role_code (role_code)
);

CREATE TABLE IF NOT EXISTS iam_permission (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  permission_code VARCHAR(80) NOT NULL,
  permission_name VARCHAR(120) NOT NULL,
  permission_type VARCHAR(30) NOT NULL DEFAULT 'api',
  path VARCHAR(255),
  method VARCHAR(20),
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_iam_permission_code (permission_code)
);

CREATE TABLE IF NOT EXISTS iam_account_role (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_iam_account_role (account_id, role_id)
);

CREATE TABLE IF NOT EXISTS iam_role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_iam_role_permission (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS user_profile (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  nickname VARCHAR(80) NOT NULL,
  avatar_url VARCHAR(255),
  gender VARCHAR(20),
  birthday DATE,
  register_source VARCHAR(40) NOT NULL DEFAULT 'app',
  member_level_name VARCHAR(40) NOT NULL DEFAULT '普通会员',
  growth_value INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_profile_account (account_id)
);

CREATE TABLE IF NOT EXISTS user_address (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  contact_name VARCHAR(60) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  province VARCHAR(40) NOT NULL,
  city VARCHAR(40) NOT NULL,
  district VARCHAR(40) NOT NULL,
  detail_address VARCHAR(255) NOT NULL,
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  tag_name VARCHAR(30),
  is_default TINYINT NOT NULL DEFAULT 0,
  delivery_note VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  favorite_type VARCHAR(30) NOT NULL,
  target_id BIGINT NOT NULL,
  target_name VARCHAR(120) NOT NULL,
  cover_url VARCHAR(255),
  subtitle VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_favorite_target (user_id, favorite_type, target_id)
);
