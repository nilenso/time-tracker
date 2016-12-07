ALTER TABLE project_permission
DROP CONSTRAINT user_project_unique;

ALTER TABLE project_permission
ALTER COLUMN permissions
DROP DEFAULT;

