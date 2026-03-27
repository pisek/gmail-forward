CREATE TABLE IF NOT EXISTS seen_messages (
    account_name VARCHAR(255) NOT NULL,
    message_id   VARCHAR(1024) NOT NULL,
    seen_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_name, message_id)
);

CREATE INDEX IF NOT EXISTS idx_seen_account ON seen_messages(account_name);
