ALTER TABLE project
      ADD COLUMN client_id INTEGER NOT NULL
      REFERENCES client(id);
