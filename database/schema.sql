CREATE DATABASE IF NOT EXISTS expense_tracker
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE expense_tracker;

-- One row per registered person. Passwords are stored as BCrypt hashes, never as plain text.
CREATE TABLE IF NOT EXISTS users (
                                     id             BIGINT        NOT NULL AUTO_INCREMENT,
                                     email          VARCHAR(255)  NOT NULL,
    display_name   VARCHAR(100)  NOT NULL,
    password_hash  VARCHAR(100)  NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
    ) ENGINE=InnoDB;

-- Default categories have user_id = NULL (shared by everyone).
-- Custom categories belong to the user who created them.
CREATE TABLE IF NOT EXISTS categories (
                                          id          BIGINT       NOT NULL AUTO_INCREMENT,
                                          user_id     BIGINT       NULL,
                                          name        VARCHAR(50)  NOT NULL,
    type        VARCHAR(10)  NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_category_user_name_type UNIQUE (user_id, name, type),
    CONSTRAINT chk_category_type CHECK (type IN ('INCOME', 'EXPENSE'))
    ) ENGINE=InnoDB;

-- Every transaction belongs to exactly one user and one category.
CREATE TABLE IF NOT EXISTS transactions (
                                            id                BIGINT         NOT NULL AUTO_INCREMENT,
                                            user_id           BIGINT         NOT NULL,
                                            amount            DECIMAL(12,2)  NOT NULL,
    type              VARCHAR(10)    NOT NULL,
    category_id       BIGINT         NOT NULL,
    transaction_date  DATE           NOT NULL,
    description       VARCHAR(255)   NULL,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_transaction_amount CHECK (amount > 0),
    CONSTRAINT chk_transaction_type CHECK (type IN ('INCOME', 'EXPENSE')),
    INDEX idx_transactions_user_date (user_id, transaction_date),
    INDEX idx_transactions_type (type)
    ) ENGINE=InnoDB;

-- Default categories. The NOT EXISTS check makes this safe to run more than once.
INSERT INTO categories (name, type, is_default)
SELECT v.name, v.type, TRUE
FROM (
         SELECT 'Food' AS name, 'EXPENSE' AS type
         UNION ALL SELECT 'Travel', 'EXPENSE'
         UNION ALL SELECT 'Shopping', 'EXPENSE'
         UNION ALL SELECT 'Bills', 'EXPENSE'
         UNION ALL SELECT 'Education', 'EXPENSE'
         UNION ALL SELECT 'Healthcare', 'EXPENSE'
         UNION ALL SELECT 'Other', 'EXPENSE'
         UNION ALL SELECT 'Salary', 'INCOME'
         UNION ALL SELECT 'Freelance', 'INCOME'
         UNION ALL SELECT 'Gift', 'INCOME'
         UNION ALL SELECT 'Other', 'INCOME'
     ) AS v
WHERE NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE c.user_id IS NULL AND c.name = v.name AND c.type = v.type
);