DROP INDEX google_id_idx;

ALTER TABLE project_permission ALTER COLUMN permissions DROP NOT NULL;
ALTER TABLE project_permission ALTER COLUMN app_user_id DROP NOT NULL;
ALTER TABLE project_permission ALTER COLUMN project_id DROP NOT NULL;

ALTER TABLE project ALTER COLUMN name DROP NOT NULL;
ALTER TABLE project ALTER COLUMN name TYPE varchar(300);

ALTER TABLE app_user ALTER COLUMN role DROP NOT NULL;
ALTER TABLE app_user ALTER COLUMN name DROP NOT NULL;
ALTER TABLE app_user ALTER COLUMN name TYPE varchar(300);
ALTER TABLE app_user ALTER COLUMN google_id DROP NOT NULL;
ALTER TABLE app_user ALTER COLUMN google_id TYPE varchar(300);

