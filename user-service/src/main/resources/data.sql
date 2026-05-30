-- Seed users (matching previous static data)
INSERT INTO users (id, name, email, role) VALUES (1, 'John Doex12', 'john@example.com', 'admin') ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, name, email, role) VALUES (2, 'Jane Smith green', 'jane@example.com', 'user') ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, name, email, role) VALUES (3, 'Bob Wilson', 'bob@example.com', 'user') ON CONFLICT (id) DO NOTHING;

-- Seed contacts (matching previous static data)
INSERT INTO contacts (id, user_id, phone, address, emergency) VALUES (1, 1, '+1-555-0101', '123 Main St, New York', '+1-555-0901') ON CONFLICT (id) DO NOTHING;
INSERT INTO contacts (id, user_id, phone, address, emergency) VALUES (2, 2, '+1-555-0102', '456 Oak Ave, Chicago', '+1-555-0902') ON CONFLICT (id) DO NOTHING;
INSERT INTO contacts (id, user_id, phone, address, emergency) VALUES (3, 3, '+1-555-0103', '789 Pine Rd, Seattle', '+1-555-0903') ON CONFLICT (id) DO NOTHING;

-- Reset sequences
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('contacts_id_seq', (SELECT MAX(id) FROM contacts));
