ALTER TABLE timer DROP COLUMN project_id;
ALTER TABLE timer
      ADD COLUMN task_id INTEGER NOT NULL
      REFERENCES task(id);
