-- Upgrade script for databases created before user accounts existed. New installs only need schema.sql.
USE expense_tracker;

CREATE TABLE IF NOT EXISTS users (
                                     id             BIGINT        NOT NULL AUTO_INCREMENT,
                                     email          VARCHAR(255)  NOT NULL,
    display_name   VARCHAR(100)  NOT NULL,
    password_hash  VARCHAR(100)  NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
    ) ENGINE=InnoDB;

-- Existing transactions and custom categories have no owner, so remove them.
-- (You can reload the sample data after registering.)
DELETE FROM transactions;
DELETE FROM categories WHERE is_default = FALSE;

ALTER TABLE transactions
    ADD COLUMN user_id BIGINT NOT NULL AFTER id,
    ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD INDEX idx_transactions_user_date (user_id, transaction_date);

-- user_id stays NULL for the default categories that everyone shares
ALTER TABLE categories
    ADD COLUMN user_id BIGINT NULL AFTER id,
    ADD CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
DROP INDEX uq_category_name_type,
    ADD CONSTRAINT uq_category_user_name_type UNIQUE (user_id, name, type);