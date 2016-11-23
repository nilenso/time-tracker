ALTER TABLE project_permission
ALTER COLUMN permissions
SET DEFAULT '{}';

ALTER TABLE project_permission
ADD CONSTRAINT user_project_unique
UNIQUE (project_id, app_user_id);
