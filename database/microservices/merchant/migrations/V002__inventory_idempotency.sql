CREATE TABLE IF NOT EXISTS inventory_idempotency_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  caller_service VARCHAR(64) NOT NULL,
  api_action VARCHAR(40) NOT NULL,
  idempotency_key VARCHAR(255) NOT NULL,
  request_summary VARCHAR(2000),
  result_summary VARCHAR(4000),
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_inventory_idem (caller_service, api_action, idempotency_key)
);
