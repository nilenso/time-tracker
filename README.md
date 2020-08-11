# time-tracker

__🚧 Note 🚧__ We're currently in the process of restructuring the project, expect master to be broken until 12th August.

A time-tracker built for education, fun and eventually profit.

## Setup

### Frontend Setup
You'll need:
* [`yarn`](https://classic.yarnpkg.com/en/docs/install)
* `node`
* Preferably a good ClojureScript editor of your choice

#### Running the development build with hot reloading
`yarn start`, then browse to `http://localhost:8090`.  
Although it's not necessary for hot reloading, you should connect to the REPL from your editor. Follow the instructions at https://shadow-cljs.github.io/docs/UsersGuide.html#_editor_integration.

#### Running the tests
`yarn test`. You can also run tests from the REPL using `cljs.test/run-test`.

#### Generating a release build
`yarn release`. Assets will go into the `resources/public` folder.

### Backend Setup
#### Local Development

- Ensure you have docker-compose installed. podman-compose will also work.
- Install **Leiningen**. The instructions for doing so, are available at https://leiningen.org/#install.
- Run docker compose to start dev & test dependencies from the root directory:

  ```bash
  docker-compose up -d
  ```

- To start a server, either run:

```bash
lein run -- -sf config/config.dev.edn
```

or:

```bash
$ lein repl

# in the repl:
time-tracker.core=> (init! "config/config.dev.edn")
(start-server!)
```

#### Production

*Note: The following setup assumes that you're running Ubuntu.*

- Install Dependencies

``` bash
apt-get install nginx npm node git unzip openjdk-8-jdk postgresql
```

- Create Database

``` bash
adduser timetracker
# This will prompt for a password
sudo -u postgres createuser timetracker -P
sudo -u postgres createdb timetracker -O timetracker
```

- Set the following environment variables in your `~/.bash_profile` with the appropriate changes:

``` bash
GOOGLE_TOKENINFO_URL="https://www.googleapis.com/oauth2/v3/tokeninfo"
CP_MAX_IDLE_TIME_EXCESS_CONNECTIONS="1800"
CP_MAX_IDLE_TIME="10800"
DB_CONNECTION_STRING="jdbc:postgresql://localhost/timetracker?user=timetracker&password=your_password_if_any"
GOOGLE_CLIENT_ID="your_google_client_id"
APP_LOG_LEVEL="debug"
PORT="8000"
ALLOWED_HOSTED_DOMAIN="yourgoogleappsforworkhosteddomain.com"
LOG_FILE_PREFIX="/path/to/log/file.log"
```

- Configure iptables:

``` bash
iptables -A INPUT -i lo -p tcp -m tcp --dport 8000 -j ACCEPT
iptables -A INPUT -p tcp -m tcp --dport 8000 -j DROP
ip6tables -A INPUT -i lo -p tcp -m tcp --dport 8000 -j ACCEPT
ip6tables -A INPUT -p tcp -m tcp --dport 8000 -j DROP
```

- Copy Nginx configuration in `config/nginx.conf` to `/etc/nginx/sites-available/timetracker.conf` and create a symbolic like to it from `/etc/nginx/sites-enabled/`:

```bash
ln -s /etc/nginx/sites-available/timetracker.conf /etc/nginx/sites-enabled/timetracker.conf

# Restart nginx for the changes to take effect
sudo nginx -s reload
```

## Testing

To run tests, do:

```bash
TIME_TRACKER_TEST_CONFIG=./config/config.test.edn lein test
```

## Deployment

Once you're done setting up the prerequisites, you can deploy the app to production from your local machine by running the following command:

``` bash
./scripts/deploy.sh
```

## License

Copyright © 2017 Nilenso Software

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
