# time-tracker-web

The web interface for time tracker.

## Setup
You'll need:
* `npm`
* `node`
* Preferably a good ClojureScript editor of your choice

### Running the development build with hot reloading
`npm start`, then browse to `http://localhost:8090`.  
Although it's not necessary for hot reloading, you should connect to the REPL from your editor. Follow the instructions at https://shadow-cljs.github.io/docs/UsersGuide.html#_editor_integration.

### Running the tests
`npm test`. You can also run tests from the REPL using `cljs.test/run-test`.

### Generating a release build
`npm run release`
