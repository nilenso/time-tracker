CREATE TABLE task(
       id SERIAL PRIMARY KEY,
       name VARCHAR(300),
       project_id INTEGER REFERENCES project(id)
);
