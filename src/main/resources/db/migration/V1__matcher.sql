CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE app_users (
    id uuid PRIMARY KEY,
    username varchar(80) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE TABLE documents (
    id uuid PRIMARY KEY,
    owner_name varchar(80) NOT NULL,
    kind varchar(20) NOT NULL CHECK (kind IN ('RESUME', 'JOB')),
    filename varchar(255) NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('PENDING', 'READY', 'FAILED')),
    text_content text,
    error_message varchar(1000),
    created_at timestamptz NOT NULL
);

CREATE INDEX documents_owner_kind_idx ON documents (owner_name, kind);
