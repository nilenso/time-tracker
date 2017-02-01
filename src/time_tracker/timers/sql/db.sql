-- name: has-timing-access-query
-- Checks if this user has the given permission on the given project.
SELECT COUNT(*) FROM app_user
INNER JOIN project_permission ON app_user.id = project_permission.app_user_id
INNER JOIN project ON project_permission.project_id = project.id
WHERE :permission::permission = ANY (project_permission.permissions)
AND app_user.google_id = :google_id
AND project.id = :project_id;

-- name: owns-query
-- Checks if this user owns a timer.
SELECT COUNT(*) FROM app_user
INNER JOIN timer ON app_user.id = timer.app_user_id
WHERE timer.id = :timer_id
AND app_user.google_id = :google_id;

-- name: create-timer-query<!
-- Creates a timer given a google id and a project id.
INSERT INTO timer (project_id, app_user_id, time_created, notes)
VALUES (:project_id, (SELECT id FROM app_user WHERE google_id = :google_id), to_timestamp(:created_time), :notes);

-- name: delete-timer-query!
-- Deletes a timer..
DELETE FROM timer
WHERE timer.id = :timer_id;

-- name: retrieve-authorized-timers-query
-- Retrieves all the timers the user is authorized to modify.
SELECT timer.* FROM timer
INNER JOIN app_user ON app_user.id = timer.app_user_id
WHERE app_user.google_id = :google_id;

-- name: start-timer-query!
-- Starts a timer if it isn't started already and if the user owns it.
UPDATE timer
SET started_time = to_timestamp(:current_time)
WHERE timer.id = :timer_id
AND timer.started_time IS NULL;

-- name: retrieve-all-query
-- Retrieves all timers.
SELECT timer.* FROM timer;

-- name: retrieve-between-query
-- Retrieves all timers created in [start_epoch, end_epoch)
SELECT timer.* FROM timer
WHERE time_created >= to_timestamp(:start_epoch)
AND time_created < to_timestamp(:end_epoch);

-- name: retrieve-between-authorized-query
-- Retrieves all timers created in [start_epoch, end_epoch) owned by google_id
SELECT timer.* FROM timer
INNER JOIN app_user ON app_user.id=timer.app_user_id
WHERE app_user.google_id = :google_id
AND timer.time_created >= to_timestamp(:start_epoch)
AND timer.time_created < to_timestamp(:end_epoch);

-- name: retrieve-timer-query
-- Retrieves a single timer.
SELECT timer.* FROM timer
WHERE timer.id = :timer_id;

-- name: stop-timer-query!
-- Stops a timer if it isn't stopped already and if the user owns it.
UPDATE timer
SET duration = :duration, started_time = NULL
WHERE timer.started_time IS NOT NULL
AND timer.id = :timer_id;

-- name: update-timer-query!
-- Updates the timer. Sets the timer's duration to the given value and restarts it if already started.
UPDATE timer
SET notes = :notes,
    duration = :duration,
    started_time = CASE WHEN started_time IS NULL THEN NULL
                        ELSE to_timestamp(:current_time)
                   END
WHERE timer.id = :timer_id;

-- name: retrieve-started-timers-query
-- Retrieves all the timers the user has started.
SELECT timer.* FROM timer
INNER JOIN app_user ON app_user.id = timer.app_user_id
WHERE app_user.google_id = :google_id
AND timer.started_time IS NOT NULL;
