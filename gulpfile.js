"use strict";

const gulp = require("gulp");
const sass = require("gulp-sass");
const sourcemaps = require("gulp-sourcemaps");
const concat = require("gulp-concat");
const cssnano = require("cssnano");
const postcss = require("gulp-postcss");

const { spawn } = require("child_process");

sass.compiler = require("sass");

const sassDev = function () {
  return gulp
    .src("./src/styles/**/*.scss")
    .pipe(sourcemaps.init())
    .pipe(sass().on("error", sass.logError))
    .pipe(concat("index.css"))
    .pipe(sourcemaps.write())
    .pipe(gulp.dest("./resources/public/css"));
};

exports.watchSass = function () {
  gulp.watch("./src/styles/**/*.scss", { ignoreInitial: false }, sassDev);
};

exports.buildSassProd = function () {
  var plugins = [cssnano()];

  return gulp
    .src("./src/styles/**/*.scss")
    .pipe(sass().on("error", sass.logError))
    .pipe(concat("index.css"))
    .pipe(postcss(plugins))
    .pipe(gulp.dest("./resources/public/css"));
};

exports.runShadow = function (cb) {
  const shadow = spawn("yarn", ["run", "shadow-cljs", "watch", "app"]);
  shadow.stdout.on("data", (data) => {
    process.stdout.write(`shadow-cljs: ${data}`);
  });

  shadow.stderr.on("data", (data) => {
    process.stderr.write(`shadow-cljs: ${data}`);
  });

  shadow.on("close", (code) => {
    if (code === 0) {
      cb();
    } else {
      cb(new Error(`shadow-cljs exited with return code ${code}`));
    }
  });
};

exports.default = gulp.parallel(exports.watchSass, exports.runShadow);
