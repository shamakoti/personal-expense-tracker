USE expense_tracker;

-- Development sample data. Safe to delete later with: DELETE FROM transactions;
INSERT INTO transactions (amount, type, category_id, transaction_date, description)
SELECT v.amount, v.type, c.id, v.tx_date, v.description
FROM (
         SELECT 50000.00 AS amount, 'INCOME'  AS type, 'Salary'     AS cat, '2026-08-01' AS tx_date, 'August salary'            AS description
         UNION ALL SELECT  4200.50, 'EXPENSE', 'Food',       '2026-08-05', 'Groceries'
         UNION ALL SELECT  3500.00, 'EXPENSE', 'Bills',      '2026-08-10', 'Electricity and internet'
         UNION ALL SELECT  2800.00, 'EXPENSE', 'Travel',     '2026-08-18', 'Weekend trip'
         UNION ALL SELECT  6100.00, 'EXPENSE', 'Shopping',   '2026-08-22', 'New headphones'
         UNION ALL SELECT 50000.00, 'INCOME',  'Salary',     '2026-09-01', 'September salary'
         UNION ALL SELECT 12000.00, 'INCOME',  'Freelance',  '2026-09-12', 'Website project'
         UNION ALL SELECT  5100.75, 'EXPENSE', 'Food',       '2026-09-07', 'Groceries and dining'
         UNION ALL SELECT  3500.00, 'EXPENSE', 'Bills',      '2026-09-10', 'Electricity and internet'
         UNION ALL SELECT  8000.00, 'EXPENSE', 'Education',  '2026-09-15', 'Online course'
         UNION ALL SELECT  1500.00, 'EXPENSE', 'Healthcare', '2026-09-20', 'Doctor visit'
         UNION ALL SELECT  2300.25, 'EXPENSE', 'Shopping',   '2026-09-25', 'Clothes'
         UNION ALL SELECT 50000.00, 'INCOME',  'Salary',     '2026-10-01', 'October salary'
         UNION ALL SELECT  4800.00, 'EXPENSE', 'Food',       '2026-10-04', 'Groceries'
         UNION ALL SELECT  3200.00, 'EXPENSE', 'Travel',     '2026-10-09', 'Train tickets'
         UNION ALL SELECT  3600.00, 'EXPENSE', 'Bills',      '2026-10-10', 'Electricity and internet'
         UNION ALL SELECT   900.00, 'EXPENSE', 'Other',      '2026-10-12', 'Miscellaneous'
         UNION ALL SELECT  2000.00, 'INCOME',  'Gift',       '2026-10-14', 'Birthday gift'
     ) AS v
         JOIN categories c ON c.name = v.cat AND c.type = v.type;