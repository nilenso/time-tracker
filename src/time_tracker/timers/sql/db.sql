-- name: has-timing-access-query
-- Checks if this user has the given permission on the given project.
SELECT COUNT(*) from app_user
INNER JOIN project_permission ON app_user.id = project_permission.app_user_id
INNER JOIN project ON project_permission.project_id = project.id
WHERE :permission::permission = ANY (project_permission.permissions)
AND app_user.google_id = :google_id
AND project.id = :project_id;

-- name: create-timer-query<!
-- Creates a timer given a google id and a project id.
INSERT INTO timer (project_id, app_user_id)
VALUES (:project_id, (SELECT id FROM app_user WHERE google_id = :google_id));
