# time-tracker

Be nilenso's time tracking tool.

## Installation

- setup config file: Please see `profiles.clj.sample` for a sample `profiles.clj`.
Change the settings as needed. Rename it to `profiles.clj`.xs

- setup postgres databases:
	``` shell
	initdb db
	pg_ctl -D db -l pg.log start
	psql -d postgres
	create database time_tracker;
	create database time_tracker_test;
	```
- migrate
  ``` lein migrate```

- ensure tests pass
  ```lein test```

## Usage

FIXME: explanation

## License

Copyright © 2016

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
