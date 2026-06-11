CREATE TABLE IF NOT EXISTS ai_assistant_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  conversation_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  last_message_id BIGINT,
  last_message_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_assistant_conversation_no (conversation_no)
);

CREATE INDEX idx_ai_assistant_conversation_user
  ON ai_assistant_conversation (user_id, status, is_deleted, last_message_at);

CREATE TABLE IF NOT EXISTS ai_assistant_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  cards_json TEXT,
  actions_json TEXT,
  steps_json TEXT,
  used_skills_json TEXT,
  model_used TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_assistant_message_conversation
  ON ai_assistant_message (conversation_id, is_deleted, id);
