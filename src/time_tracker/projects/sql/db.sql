-- name: has-project-permission-query
-- Check if the user has the given permission on the given project.
SELECT COUNT(*) FROM project
INNER JOIN project_permission ON project.id = project_permission.project_id
INNER JOIN app_user ON app_user.id = project_permission.app_user_id
WHERE app_user.google_id = :google_id
AND :permission::permission = ANY (project_permission.permissions)
AND project.id = :project_id;

-- name: has-role-query
-- Checks if a user has a particular role.
SELECT COUNT(*) FROM app_user 
WHERE google_id = :google_id 
AND role = :role::user_role

-- name: retrieve-query
-- Retrieves a project if the user is authorized.
SELECT project.* FROM project 
WHERE project.id = :project_id;

-- name: update-query!
-- Updates a project if the user is authorized.
UPDATE project 
SET name = :name 
WHERE project.id = :project_id;

-- name: delete-query!
-- Deletes a project if the user is authorized.
DELETE FROM project 
WHERE project.id = :project_id;

-- name: retrieve-authorized-projects-query
-- Retrieves all the projects that a user is authorized to view.
SELECT project.* FROM project 
INNER JOIN project_permission ON project.id = project_permission.project_id 
INNER JOIN app_user ON app_user.id = project_permission.app_user_id 
WHERE app_user.google_id = :google_id 
AND :permission::permission = ANY (project_permission.permissions);

-- name: retrieve-all-projects-query
-- Retrieves all the projects.
SELECT project.* from project;

-- name: create-admin-permission-query!
-- Creates an admin permission for the given user on the given project.
INSERT INTO project_permission 
(project_id, app_user_id, permissions) 
VALUES (:project_id, :user_id, ARRAY['admin']::permission[]);

-- name: grant-permission-query!
-- Grants a permission on the given project to a user.
INSERT INTO project_permission
(project_id, app_user_id, permissions)
VALUES (:project_id, :user_id, array_append('{}'::permission[], :permission::permission))
ON CONFLICT (project_id, app_user_id) DO UPDATE
SET permissions = array_append(project_permission.permissions, :permission::permission)
WHERE project_permission.project_id = :project_id
AND project_permission.app_user_id = :user_id;
