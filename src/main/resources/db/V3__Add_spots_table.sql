CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS spots (
	id uuid DEFAULT uuid_generate_v4(),
	identifier VARCHAR,
	user_id uuid
);