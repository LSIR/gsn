# --- !Ups
INSERT INTO security_role(id, role_name) VALUES (1, 'user');
INSERT INTO security_role(id, role_name) VALUES (2, 'admin');
INSERT INTO users(id, email, name, first_name, last_name, active, email_validated) VALUES (0, 'root@localhost', 'Admin', 'Admin', '', True, True);
INSERT INTO users_security_role(users_id, security_role_id) VALUES (0, 1);
INSERT INTO users_security_role(users_id, security_role_id) VALUES (0, 2);
INSERT INTO linked_account(id, user_id, provider_user_id, provider_key) VALUES (0,0,'$2a$10$lRUloW/0IZTsQ1SUi7yFj.bZjSNw9MwrHR3h9VZZvafuPmzNNi0aq','password');
INSERT INTO client(id, name, client_id, secret, redirect, linked, user_id) VALUES (0, 'Default Web UI', 'web-gui-public', 'web-gui-secret', 'http://localhost:8000/profile/', False, 0);

# --- !Downs

DELETE FROM linked_account WHERE id = 0;
DELETE FROM users_security_role WHERE security_role_id = 1 or security_role_id = 2;
DELETE FROM users WHERE id = 0;
DELETE FROM security_role WHERE id = 1 OR id = 2;
