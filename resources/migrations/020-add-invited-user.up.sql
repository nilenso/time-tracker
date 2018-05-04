CREATE TABLE invited_user (
       email TEXT PRIMARY KEY,
       invited_by INTEGER REFERENCES app_user(id),
       registered BOOLEAN NOT NULL DEFAULT false
);
