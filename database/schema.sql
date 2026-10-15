USE expense_tracker;

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    type        VARCHAR(10)  NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_category_name_type UNIQUE (name, type),
    CONSTRAINT chk_category_type CHECK (type IN ('INCOME', 'EXPENSE'))
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    amount            DECIMAL(12,2)  NOT NULL,
    type              VARCHAR(10)    NOT NULL,
    category_id       BIGINT         NOT NULL,
    transaction_date  DATE           NOT NULL,
    description       VARCHAR(255)   NULL,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_transaction_amount CHECK (amount > 0),
    CONSTRAINT chk_transaction_type CHECK (type IN ('INCOME', 'EXPENSE')),
    INDEX idx_transactions_date (transaction_date),
    INDEX idx_transactions_type (type)
    ) ENGINE=InnoDB;

-- Default categories (INSERT IGNORE skips rows that already exist, so this file is safe to re-run)
INSERT IGNORE INTO categories (name, type, is_default) VALUES
    ('Food', 'EXPENSE', TRUE),
    ('Travel', 'EXPENSE', TRUE),
    ('Shopping', 'EXPENSE', TRUE),
    ('Bills', 'EXPENSE', TRUE),
    ('Education', 'EXPENSE', TRUE),
    ('Healthcare', 'EXPENSE', TRUE),
    ('Other', 'EXPENSE', TRUE),
    ('Salary', 'INCOME', TRUE),
    ('Freelance', 'INCOME', TRUE),
    ('Gift', 'INCOME', TRUE),
    ('Other', 'INCOME', TRUE);