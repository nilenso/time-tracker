CREATE TYPE user_role AS ENUM ('admin', 'user');

ALTER TABLE app_user
ADD COLUMN role user_role DEFAULT 'user'::user_role;
