-- 1. ROLES
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL
);

-- IMPORTANT: These names MUST match your ERole enum (ROLE_ADMIN, etc.)
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_LAB_HEAD'), ('ROLE_MEMBER');

-- 2. USERS (Authentification)
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       email VARCHAR(150) UNIQUE NOT NULL,
                       password TEXT NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. MEMBERS (Scientific Profiles)
CREATE TABLE members (
                         id SERIAL PRIMARY KEY,
                         user_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         position VARCHAR(100), -- Professeur, Doctorant, etc.
                         lab VARCHAR(150) DEFAULT 'MEDIATRON',
                         orcid_id VARCHAR(50),
                         bio TEXT,
                         photo_url TEXT
);

-- 4. USER_ROLES (Join Table for Many-to-Many)
-- This matches your @JoinTable(name = "user_roles") in User.java
CREATE TABLE user_roles (
                            user_id INT REFERENCES users(id) ON DELETE CASCADE,
                            role_id INT REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);