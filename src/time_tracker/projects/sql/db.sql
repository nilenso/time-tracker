-- name: retrieve-if-authorized-query
-- Retrieves a project if the user is authorized.
SELECT project.* FROM project 
INNER JOIN project_permission ON project.id = project_permission.project_id 
INNER JOIN app_user ON app_user.id = project_permission.app_user_id 
WHERE app_user.google_id = :google_id 
AND :permission::permission = ANY (project_permission.permissions) 
AND project.id = :project_id;

-- name: update-if-authorized-query!
-- Updates a project if the user is authorized.
UPDATE project 
SET name = :name 
FROM project_permission, app_user 
WHERE project_permission.project_id = project.id 
AND project_permission.app_user_id = app_user.id 
AND :permission::permission = ANY (project_permission.permissions) 
AND app_user.google_id = :google_id 
AND project.id = :project_id;

-- name: delete-if-authorized-query!
-- Deletes a project if the user is authorized.
DELETE FROM project 
USING project_permission, app_user 
WHERE project_permission.project_id = project.id 
AND project_permission.app_user_id = app_user.id 
AND :permission::permission = ANY (project_permission.permissions) 
AND app_user.google_id = :google_id 
AND project.id = :project_id;

-- name: retrieve-authorized-projects-query
-- Retrieves all the projects that a user is authorized to view.
SELECT project.* FROM project 
INNER JOIN project_permission ON project.id = project_permission.project_id 
INNER JOIN app_user ON app_user.id = project_permission.app_user_id 
WHERE app_user.google_id = :google_id 
AND :permission::permission = ANY (project_permission.permissions);

-- name: is-admin-query
-- Checks if a user is an admin.
SELECT COUNT(*) FROM app_user 
WHERE google_id = :google_id 
AND role = :role::user_role

-- name: create-admin-permission-query!
-- Creates an admin permission for the given user on the given project.
INSERT INTO project_permission 
(project_id, app_user_id, permissions) 
VALUES (:project_id, :user_id, ARRAY['admin']::permission[]);
