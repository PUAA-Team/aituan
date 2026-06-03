-- Stage6 成员E：客服 AI/人工与平台介入状态

ALTER TABLE support_session ADD COLUMN service_scope VARCHAR(20) NOT NULL DEFAULT 'merchant';
ALTER TABLE support_session ADD COLUMN assistant_mode VARCHAR(20) NOT NULL DEFAULT 'human';
ALTER TABLE support_session ADD COLUMN platform_intervention_status VARCHAR(20) NOT NULL DEFAULT 'none';
ALTER TABLE support_session ADD COLUMN human_requested_at DATETIME;
ALTER TABLE support_session ADD COLUMN platform_intervened_at DATETIME;
ALTER TABLE support_message MODIFY COLUMN message_kind VARCHAR(40);

UPDATE support_session
SET service_scope = CASE WHEN store_id = 0 THEN 'platform' ELSE 'merchant' END,
    assistant_mode = CASE WHEN store_id = 0 THEN 'ai' ELSE 'human' END,
    platform_intervention_status = 'none'
WHERE is_deleted = 0;

CREATE INDEX idx_support_session_scope ON support_session (service_scope, assistant_mode, status);
CREATE INDEX idx_support_session_intervention ON support_session (platform_intervention_status, status);
