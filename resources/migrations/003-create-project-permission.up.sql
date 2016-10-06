CREATE TYPE permission AS ENUM ('admin', 'none');

CREATE TABLE project_permission (
    id SERIAL PRIMARY KEY,
    project_id INTEGER REFERENCES project ON DELETE CASCADE,
    app_user_id INTEGER REFERENCES app_user ON DELETE CASCADE,
    permissions permission ARRAY
);
