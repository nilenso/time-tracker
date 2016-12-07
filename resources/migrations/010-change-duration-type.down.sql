ALTER TABLE timer
ALTER COLUMN duration DROP DEFAULT;

ALTER TABLE timer
ALTER COLUMN duration TYPE interval hour to second USING duration * INTERVAL '1 second';

ALTER TABLE timer
ALTER COLUMN duration SET DEFAULT INTERVAL '0 second';
