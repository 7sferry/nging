-- Seed accounts (matching previous static data)
INSERT INTO accounts (id, user_id, balance) VALUES (1, 1, 15000.50) ON CONFLICT (id) DO NOTHING;
INSERT INTO accounts (id, user_id, balance) VALUES (2, 2, 8250.75) ON CONFLICT (id) DO NOTHING;
INSERT INTO accounts (id, user_id, balance) VALUES (3, 3, 3420.00) ON CONFLICT (id) DO NOTHING;

-- Reset sequence
SELECT setval('accounts_id_seq', (SELECT MAX(id) FROM accounts));
