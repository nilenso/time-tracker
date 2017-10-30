-- name: retrieve-all-clients-query
-- Retrieves all the clients.
SELECT client.* from client;

-- name: has-role-query
-- Checks if a user has a particular role.
SELECT COUNT(*) FROM app_user
WHERE google_id = :google_id
AND role = :role::user_role
