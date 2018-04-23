CREATE TABLE task(
       id SERIAL PRIMARY KEY,
       name VARCHAR(300),
       project_id INTEGER NOT NULL REFERENCES project(id)
);
