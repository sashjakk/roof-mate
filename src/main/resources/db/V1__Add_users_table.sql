CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
	id uuid DEFAULT uuid_generate_v4(),
	name VARCHAR,
	surname VARCHAR,
	phone VARCHAR
);