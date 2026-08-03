-- Test data shared by all integration tests (runs after V43 migrations).

INSERT INTO users (id, first_name, last_name, birthdate, email, phone, password_hash, role, status)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'Manager', 'One', '1980-01-01', 'manager@cinema.test', '0102030405', 'hash', 'MANAGER', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002', 'Employee', 'One', '1985-01-01', 'employee@cinema.test', '0607080910', 'hash', 'EMPLOYEE', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000003', 'Client', 'One', '1990-01-01', 'client@cinema.test', '1112131415', 'hash', 'CLIENT', 'ACTIVE');

INSERT INTO rooms (id, number, capacity)
VALUES ('00000000-0000-0000-0000-000000000021', 'SALLE1', 15);
