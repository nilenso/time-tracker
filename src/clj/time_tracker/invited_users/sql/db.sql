-- name: create-invited-user-query<!
-- Inserts an invited user in the database.
INSERT INTO invited_user
(email, invited_by)
VALUES (:email, :invited_by)
ON CONFLICT DO NOTHING;

-- name: retrieve-invited-user-query
-- Retrieves an invited user
SELECT * FROM invited_user
WHERE invited_user.email = :email AND invited_user.registered = false
LIMIT 1;

-- name: mark-registered-user-query!
-- Marks invited user as registered
UPDATE invited_user SET registered = true
WHERE invited_user.email = :email;

-- name: retrieve-all-invited-users-query
-- Retrieve all invited users
SELECT * FROM invited_user
WHERE invited_user.registered = false;
