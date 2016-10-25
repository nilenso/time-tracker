ALTER TABLE app_user ALTER COLUMN google_id TYPE text;
ALTER TABLE app_user ALTER COLUMN google_id SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN name TYPE text;
ALTER TABLE app_user ALTER COLUMN name SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN role SET NOT NULL;

ALTER TABLE project ALTER COLUMN name TYPE text;
ALTER TABLE project ALTER COLUMN name SET NOT NULL;

ALTER TABLE project_permission ALTER COLUMN project_id SET NOT NULL;
ALTER TABLE project_permission ALTER COLUMN app_user_id SET NOT NULL;
ALTER TABLE project_permission ALTER COLUMN permissions SET NOT NULL;

CREATE UNIQUE INDEX google_id_idx ON app_user (google_id);
