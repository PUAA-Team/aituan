UPDATE merchant_profile
SET account_id = NULL, updated_at = CURRENT_TIMESTAMP
WHERE account_id IS NOT NULL
  AND id NOT IN (
    SELECT keep_id FROM (
      SELECT MIN(id) AS keep_id
      FROM merchant_profile
      WHERE account_id IS NOT NULL AND is_deleted = 0
      GROUP BY account_id
    ) kept
  );

CREATE UNIQUE INDEX uk_merchant_profile_account ON merchant_profile(account_id);

CREATE TABLE IF NOT EXISTS merchant_application (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  application_no VARCHAR(32) NOT NULL,
  account_id BIGINT,
  merchant_name VARCHAR(120) NOT NULL,
  contact_name VARCHAR(60) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  business_type VARCHAR(40) NOT NULL,
  store_name VARCHAR(120) NOT NULL,
  address VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'pending',
  audit_remark VARCHAR(500),
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  audited_by BIGINT,
  audited_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_merchant_application_no (application_no)
);

CREATE TABLE IF NOT EXISTS merchant_certification_material (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT,
  application_id BIGINT,
  material_type VARCHAR(40) NOT NULL,
  material_name VARCHAR(120) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'pending',
  reject_reason VARCHAR(500),
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  audited_by BIGINT,
  audited_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS merchant_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  target_type VARCHAR(40) NOT NULL,
  target_id BIGINT NOT NULL,
  action VARCHAR(40) NOT NULL,
  result VARCHAR(30) NOT NULL,
  remark VARCHAR(500),
  operated_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
