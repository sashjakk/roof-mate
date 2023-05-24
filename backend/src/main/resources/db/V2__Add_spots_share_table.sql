CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS shares (
	id uuid DEFAULT uuid_generate_v4(),
	spot_id uuid,
	from_timestamp TIMESTAMP,
	to_timestamp TIMESTAMP
);