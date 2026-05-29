-- Stage6 成员E：互动与治理模块基础表

-- 评价表补字段
ALTER TABLE review_record ADD COLUMN image_urls VARCHAR(1000);
ALTER TABLE review_record ADD COLUMN helpful_count INT NOT NULL DEFAULT 0;
ALTER TABLE review_record ADD COLUMN reported_count INT NOT NULL DEFAULT 0;

-- 评价"有用"
CREATE TABLE IF NOT EXISTS review_helpful (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  review_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_review_helpful (review_id, user_id)
);

-- 评价举报
CREATE TABLE IF NOT EXISTS review_report (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  review_id BIGINT NOT NULL,
  reporter_user_id BIGINT NOT NULL,
  reason VARCHAR(80) NOT NULL,
  detail VARCHAR(500),
  status VARCHAR(30) NOT NULL DEFAULT 'submitted',
  handled_by BIGINT,
  handled_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 评价审核轨迹
CREATE TABLE IF NOT EXISTS review_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  review_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  from_status VARCHAR(30) NOT NULL,
  to_status VARCHAR(30) NOT NULL,
  operator_id BIGINT NOT NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 客服会话
CREATE TABLE IF NOT EXISTS support_session (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_no VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  topic VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'open',
  related_order_id BIGINT,
  last_message_id BIGINT,
  last_message_at DATETIME,
  user_unread_count INT NOT NULL DEFAULT 0,
  merchant_unread_count INT NOT NULL DEFAULT 0,
  closed_at DATETIME,
  closed_by_type VARCHAR(20),
  closed_by_id BIGINT,
  close_reason VARCHAR(120),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_support_session_no (session_no)
);

-- 客服消息
CREATE TABLE IF NOT EXISTS support_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  sender_type VARCHAR(20) NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  message_kind VARCHAR(20) NOT NULL DEFAULT 'text',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 投诉工单
CREATE TABLE IF NOT EXISTS complaint_ticket (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ticket_no VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  order_id BIGINT,
  store_id BIGINT,
  merchant_id BIGINT,
  category VARCHAR(30) NOT NULL,
  title VARCHAR(120) NOT NULL,
  detail VARCHAR(1000) NOT NULL,
  evidence_urls VARCHAR(1000),
  status VARCHAR(30) NOT NULL DEFAULT 'pending',
  accepted_by BIGINT,
  accepted_at DATETIME,
  resolved_by BIGINT,
  resolved_at DATETIME,
  closed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_complaint_ticket_no (ticket_no)
);

-- 投诉处理日志
CREATE TABLE IF NOT EXISTS complaint_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ticket_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  operator_type VARCHAR(20) NOT NULL,
  operator_id BIGINT,
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_review_status ON review_record (status);
CREATE INDEX idx_review_report_review ON review_report (review_id);
CREATE INDEX idx_review_report_status ON review_report (status);
CREATE INDEX idx_review_audit_review ON review_audit_log (review_id);
CREATE INDEX idx_support_session_user ON support_session (user_id, status);
CREATE INDEX idx_support_session_merchant ON support_session (merchant_id, status);
CREATE INDEX idx_support_session_store ON support_session (store_id);
CREATE INDEX idx_support_message_session ON support_message (session_id, id);
CREATE INDEX idx_complaint_user ON complaint_ticket (user_id, status);
CREATE INDEX idx_complaint_store ON complaint_ticket (store_id, status);
CREATE INDEX idx_complaint_status ON complaint_ticket (status);
CREATE INDEX idx_complaint_log_ticket ON complaint_log (ticket_id, id);

-- 字典：投诉分类
INSERT INTO sys_dict (dict_type, dict_key, dict_value, sort_order) VALUES
('complaint_category', 'service', '服务态度', 1),
('complaint_category', 'quality', '商品质量', 2),
('complaint_category', 'delivery', '配送问题', 3),
('complaint_category', 'other', '其他', 9)
ON DUPLICATE KEY UPDATE dict_value = VALUES(dict_value), sort_order = VALUES(sort_order);

-- 字典：客服快捷回复模板
INSERT INTO sys_dict (dict_type, dict_key, dict_value, sort_order) VALUES
('support_template', 't1', '您好，请问有什么可以帮您？', 1),
('support_template', 't2', '感谢您的反馈，我们会立即核实', 2),
('support_template', 't3', '已为您加急处理，请稍候', 3),
('support_template', 't4', '问题已经处理完成，欢迎再次反馈', 4),
('support_template', 't5', '由于您未在 5 分钟内回复，本次会话自动关闭', 5)
ON DUPLICATE KEY UPDATE dict_value = VALUES(dict_value), sort_order = VALUES(sort_order);
