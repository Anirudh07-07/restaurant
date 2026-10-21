-- ==============================================================
-- WhatsApp Order Notifications & Status Synchronization Migration
-- Database: MySQL 8.0+
-- ==============================================================

-- 1. Add Optimistic Locking and Maker Assignment to Orders table
ALTER TABLE orders 
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS maker_id BIGINT NULL,
    ADD CONSTRAINT fk_order_maker FOREIGN KEY (maker_id) REFERENCES users(id) ON DELETE SET NULL;

-- 2. Create WhatsApp Notification Logs (Transactional Outbox)
CREATE TABLE IF NOT EXISTS whatsapp_notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    recipient_phone VARCHAR(25) NOT NULL,
    recipient_role VARCHAR(20) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    target_order_status VARCHAR(30) NULL,
    message_preview VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(100) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    INDEX idx_wa_order_id (order_id),
    INDEX idx_wa_status (status),
    INDEX idx_wa_recipient (recipient_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Create WhatsApp Webhook Events (Idempotency and Deduplication)
CREATE TABLE IF NOT EXISTS whatsapp_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(150) NOT NULL,
    sender_phone VARCHAR(25) NOT NULL,
    action_payload VARCHAR(100) NULL,
    order_id BIGINT NULL,
    status_applied VARCHAR(30) NULL,
    processed_at DATETIME NOT NULL,
    UNIQUE INDEX idx_wa_event_msg_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================
-- ROLLBACK SCRIPT (Execute only if rolling back this migration)
-- ==============================================================
-- DROP TABLE IF EXISTS whatsapp_webhook_events;
-- DROP TABLE IF EXISTS whatsapp_notification_logs;
-- ALTER TABLE orders DROP FOREIGN KEY fk_order_maker;
-- ALTER TABLE orders DROP COLUMN maker_id;
-- ALTER TABLE orders DROP COLUMN version;

