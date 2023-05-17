CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS bookings (
	id uuid DEFAULT uuid_generate_v4(),
	share_id uuid,
	user_id uuid,
	from_timestamp TIMESTAMP,
	to_timestamp TIMESTAMP
);