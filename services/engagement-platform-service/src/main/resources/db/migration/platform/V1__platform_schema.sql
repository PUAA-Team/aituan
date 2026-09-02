CREATE TABLE IF NOT EXISTS review_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, order_no VARCHAR(40) NOT NULL,
  order_title VARCHAR(255) NOT NULL, store_id BIGINT NOT NULL, merchant_id BIGINT NOT NULL,
  store_name VARCHAR(120) NOT NULL, user_id BIGINT NOT NULL, user_nickname VARCHAR(120),
  rating INT NOT NULL, content VARCHAR(500) NOT NULL, labels VARCHAR(255), image_urls VARCHAR(1000),
  status VARCHAR(30) NOT NULL DEFAULT 'published', replied TINYINT NOT NULL DEFAULT 0,
  helpful_count INT NOT NULL DEFAULT 0, reported_count INT NOT NULL DEFAULT 0,
  order_marked TINYINT NOT NULL DEFAULT 0, order_mark_attempts INT NOT NULL DEFAULT 0,
  order_mark_last_error VARCHAR(500), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_review_order (order_id)
);
CREATE INDEX idx_review_store_status ON review_record(store_id,status,is_deleted);
CREATE INDEX idx_review_merchant_status ON review_record(merchant_id,status,is_deleted);

CREATE TABLE IF NOT EXISTS review_reply (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, review_id BIGINT NOT NULL, merchant_id BIGINT NOT NULL,
  reply_content VARCHAR(500) NOT NULL, replied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS review_helpful (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, review_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_review_helpful(review_id,user_id)
);
CREATE TABLE IF NOT EXISTS review_report (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, review_id BIGINT NOT NULL, reporter_user_id BIGINT NOT NULL,
  reason VARCHAR(80) NOT NULL, detail VARCHAR(500), evidence_urls VARCHAR(1000), status VARCHAR(30) NOT NULL DEFAULT 'submitted',
  handled_by BIGINT, handled_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_review_report_status ON review_report(review_id,status,is_deleted);
CREATE TABLE IF NOT EXISTS review_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, review_id BIGINT NOT NULL, action VARCHAR(30) NOT NULL,
  from_status VARCHAR(30) NOT NULL, to_status VARCHAR(30) NOT NULL, operator_id BIGINT NOT NULL,
  remark VARCHAR(255), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS support_session (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, session_no VARCHAR(40) NOT NULL, user_id BIGINT NOT NULL,
  user_nickname VARCHAR(120), store_id BIGINT NOT NULL DEFAULT 0, merchant_id BIGINT NOT NULL DEFAULT 0,
  store_name VARCHAR(120), topic VARCHAR(120) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'open',
  related_order_id BIGINT, related_order_no VARCHAR(40), last_message_id BIGINT, last_message_at DATETIME,
  user_unread_count INT NOT NULL DEFAULT 0, merchant_unread_count INT NOT NULL DEFAULT 0,
  closed_at DATETIME, closed_by_type VARCHAR(20), closed_by_id BIGINT, close_reason VARCHAR(120),
  service_scope VARCHAR(20) NOT NULL DEFAULT 'merchant', assistant_mode VARCHAR(20) NOT NULL DEFAULT 'human',
  platform_intervention_status VARCHAR(20) NOT NULL DEFAULT 'none', human_requested_at DATETIME,
  platform_intervened_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_support_session_no(session_no)
);
CREATE INDEX idx_support_user ON support_session(user_id,status,is_deleted);
CREATE INDEX idx_support_merchant ON support_session(merchant_id,status,is_deleted);
CREATE INDEX idx_support_store ON support_session(store_id,status,is_deleted);
CREATE TABLE IF NOT EXISTS support_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, session_id BIGINT NOT NULL, sender_type VARCHAR(20) NOT NULL,
  sender_id BIGINT NOT NULL, content VARCHAR(1000) NOT NULL, message_kind VARCHAR(40) NOT NULL DEFAULT 'text',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_support_message_session ON support_message(session_id,is_deleted,id);
CREATE TABLE IF NOT EXISTS merchant_support_auto_reply_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, merchant_id BIGINT NOT NULL, keywords VARCHAR(255) NOT NULL,
  reply_content VARCHAR(500) NOT NULL, enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_support_auto_reply_merchant ON merchant_support_auto_reply_rule(merchant_id,enabled,is_deleted);

CREATE TABLE IF NOT EXISTS complaint_ticket (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, ticket_no VARCHAR(40) NOT NULL, user_id BIGINT NOT NULL,
  user_nickname VARCHAR(120), order_id BIGINT, order_no VARCHAR(40), store_id BIGINT, store_name VARCHAR(120),
  merchant_id BIGINT, category VARCHAR(30) NOT NULL, title VARCHAR(120) NOT NULL, detail VARCHAR(1000) NOT NULL,
  evidence_urls VARCHAR(1000), status VARCHAR(30) NOT NULL DEFAULT 'pending', accepted_by BIGINT,
  accepted_at DATETIME, resolved_by BIGINT, resolved_at DATETIME, closed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0, UNIQUE KEY uk_complaint_ticket_no(ticket_no)
);
CREATE INDEX idx_complaint_user ON complaint_ticket(user_id,status,is_deleted);
CREATE INDEX idx_complaint_merchant ON complaint_ticket(merchant_id,status,is_deleted);
CREATE TABLE IF NOT EXISTS complaint_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, ticket_id BIGINT NOT NULL, action VARCHAR(30) NOT NULL,
  operator_type VARCHAR(20) NOT NULL, operator_id BIGINT, remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_complaint_log_ticket ON complaint_log(ticket_id,id);

CREATE TABLE IF NOT EXISTS ai_assistant_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, conversation_no VARCHAR(64) NOT NULL, user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'active', last_message_id BIGINT,
  last_message_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_conversation_no(conversation_no)
);
CREATE INDEX idx_ai_conversation_user ON ai_assistant_conversation(user_id,status,is_deleted,last_message_at);
CREATE TABLE IF NOT EXISTS ai_assistant_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, conversation_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL, content TEXT NOT NULL, cards_json TEXT, actions_json TEXT, steps_json TEXT,
  used_skills_json TEXT, model_used TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_ai_message_conversation ON ai_assistant_message(conversation_id,is_deleted,id);

CREATE TABLE IF NOT EXISTS platform_announcement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, title VARCHAR(120) NOT NULL, content VARCHAR(1000) NOT NULL,
  target_client VARCHAR(40) NOT NULL DEFAULT 'all', cover_url VARCHAR(500), status VARCHAR(20) NOT NULL DEFAULT 'draft',
  start_at DATETIME, end_at DATETIME, sort_order INT NOT NULL DEFAULT 0, created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, config_key VARCHAR(80) NOT NULL, config_value VARCHAR(500) NOT NULL,
  remark VARCHAR(255), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_config_key(config_key)
);
CREATE TABLE IF NOT EXISTS sys_dict (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, dict_type VARCHAR(80) NOT NULL, dict_key VARCHAR(80) NOT NULL,
  dict_value VARCHAR(120) NOT NULL, sort_order INT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0, UNIQUE KEY uk_sys_dict(dict_type,dict_key)
);
CREATE TABLE IF NOT EXISTS sys_request_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, request_id VARCHAR(80) NOT NULL, path VARCHAR(255) NOT NULL,
  method VARCHAR(20) NOT NULL, status_code INT, cost_ms INT, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS sys_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, actor_type VARCHAR(40) NOT NULL, actor_id BIGINT,
  action_type VARCHAR(80) NOT NULL, target_type VARCHAR(80), target_id BIGINT, detail VARCHAR(500),
  caller_service VARCHAR(80), idempotency_key VARCHAR(120), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_audit_idempotency(caller_service,idempotency_key)
);
CREATE TABLE IF NOT EXISTS file_asset (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, owner_type VARCHAR(40) NOT NULL, owner_id BIGINT,
  biz_type VARCHAR(40) NOT NULL, original_name VARCHAR(255) NOT NULL, storage_type VARCHAR(30) NOT NULL DEFAULT 'local',
  object_key VARCHAR(255) NOT NULL, public_url VARCHAR(500) NOT NULL, mime_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0, UNIQUE KEY uk_file_asset_object(object_key)
);
