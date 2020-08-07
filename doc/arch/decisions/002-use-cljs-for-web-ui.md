# Use ClojureScript, re-frame and shadow-cljs for the web frontend

## Status

Proposed

## Context
The web frontend of time-tracker is crucial, since it is the only way to access certain important features such as user management and invoicing. It will be built as a single-page application. Web frontend development is complex nowadays, and choosing the right stack can help mitigate this complexity and speed up the development process. We are evaluating Typescript and ClojureScript.  

Typescript is widely used and has a lot of mindshare. As such, setting up a Typescript project and making use of the Typescript/Javascript ecosystem is straightforward. 

ClojureScript offers a number of advantages over Typescript, such as programming in a functional-first style with immutable data structures, LISP syntax, seamless live code reloading, a fast REPL-driven workflow and generally less code and boilerplate. In addition to this, the [`re-frame`](http://day8.github.io/re-frame/) framework is a well thought-out approach to state management. It manages state and side effects very well and with much less boilerplate compared to popular JS/TS equivalents such as Redux. All programmers at nilenso are familiar with Clojure (if not ClojureScript already), and learning ClojureScript and getting productive shouldn't be an issue for them.

One potential concern with ClojureScript is easy access to the JavaScript/NPM ecosystem of libraries and tooling features. This concern is addressed by `shadow-cljs`, a popular build tool for ClojureScript which (among other things) makes access to NPM packages as frictionless as possible. 

Some features readily available in JS/TS such as CSS modules are tricky to set up or unusable with ClojureScript.

## Decision
1. We will use [ClojureScript](https://clojurescript.org/) as the programming language for the web frontend.
2. We will use [re-frame](http://day8.github.io/re-frame/) as the main application framework.
3. We will use [shadow-cljs](http://shadow-cljs.org/) as the build tool for ClojureScript. For other assets such as CSS, we will use other build tooling as necessary.

## Consequences
- The team shouldn't have trouble getting productive since they are already familiar with Clojure.
- The team will have to learn `re-frame`.
- The team will have easy access to NPM libraries should the need arise.
- Since ClojureScript doesn't have the mindshare of something like JS or TS, it presents a hurdle for potential outside contributors who aren't familiar with the language. 
