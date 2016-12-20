-- name: is-registered-query
-- Check if the user is in the database.
SELECT * FROM app_user
WHERE app_user.google_id = :google_id;

-- name: register-user-query!
-- Registers a user in the database.
INSERT INTO app_user
(google_id, name)
VALUES (:google_id, :name)
ON CONFLICT DO NOTHING;
