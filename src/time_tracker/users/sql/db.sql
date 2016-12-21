-- name: retrieve-user-data-query
-- Retrieves user data.
SELECT * FROM app_user
WHERE app_user.google_id = :google_id
LIMIT 1;

-- name: register-user-query!
-- Registers a user in the database.
INSERT INTO app_user
(google_id, name)
VALUES (:google_id, :name)
ON CONFLICT DO NOTHING;

