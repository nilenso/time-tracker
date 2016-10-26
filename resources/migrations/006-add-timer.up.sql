CREATE TABLE timer (
       id SERIAL PRIMARY KEY,
       project_id INTEGER REFERENCES project ON DELETE CASCADE NOT NULL,
       app_user_id INTEGER REFERENCES app_user ON DELETE CASCADE NOT NULL,
       started_time TIMESTAMP,
       duration INTERVAL NOT NULL,
       time_created TIMESTAMP NOT NULL
);
