ALTER TABLE timer DROP COLUMN task_id;
ALTER TABLE timer
      ADD COLUMN project_id INTEGER
      REFERENCES project(id);
