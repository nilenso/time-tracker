-- name: is-registered-query
-- Check if the user is in the database.
SELECT COUNT(*) FROM app_user
WHERE app_user.google_id = :google_id;
