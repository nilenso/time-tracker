-- name: has-timing-access-query
-- Checks if this user has the given permission on the given project.
SELECT COUNT(*) from app_user
INNER JOIN project_permission ON app_user.id = project_permission.app_user_id
INNER JOIN project ON project_permission.project_id = project.id
WHERE :permission::permission = ANY (project_permission.permissions)
AND app_user.google_id = :google_id
AND project.id = :project_id;

-- name: create-timer-query<!
-- Creates a timer given a google id and a project id.
INSERT INTO timer (project_id, app_user_id)
VALUES (:project_id, (SELECT id FROM app_user WHERE google_id = :google_id));

-- name: delete-if-authorized-query!
-- Deletes a timer if the user owns it.
DELETE FROM timer
USING app_user
WHERE app_user.id = timer.app_user_id
AND app_user.google_id = :google_id
AND timer.id = :timer_id;

-- name: retrieve-authorized-timers-query
-- Retrieves all the timers the user is authorized to modify.
SELECT timer.* FROM timer
INNER JOIN app_user ON app_user.id = timer.app_user_id
WHERE app_user.google_id = :google_id;

-- name: start-timer-query!
-- Starts a timer if it isn't started already and if the user owns it.
UPDATE timer
SET started_time = to_timestamp(:current_time)
FROM app_user
WHERE timer.id = :timer_id
AND timer.started_time IS NULL
AND app_user.id = timer.app_user_id
AND app_user.google_id = :google_id;

-- name: retrieve-timer-query
-- Retrieves a single timer if the user owns it.
SELECT timer.* FROM timer
INNER JOIN app_user ON app_user.id = timer.app_user_id
WHERE app_user.google_id = :google_id
AND timer.id = :timer_id;

-- name: stop-timer-query!
-- Stops a timer if it isn't stopped already and if the user owns it.
UPDATE timer
SET duration = duration + (to_timestamp(:current_time) - started_time), started_time = NULL
FROM app_user
WHERE timer.started_time IS NOT NULL
AND timer.id = :timer_id
AND app_user.id = timer.app_user_id
AND app_user.google_id = :google_id;

-- name: update-timer-duration-query!
-- Sets the timer's duration to the given value and restarts it if the user owns it.
UPDATE timer
SET duration = :duration,
    started_time = CASE WHEN started_time IS NULL THEN NULL
                        ELSE to_timestamp(:current_time)
                   END
FROM app_user
WHERE app_user.id = timer.app_user_id
AND app_user.google_id = :google_id
AND timer.id = :timer_id;
