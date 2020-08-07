# Use a common core for all native clients

## Status

Proposed

## Context

The most frequent use of time-tracker is to create, edit, and pause timers.
Users are expected to perform this activity multiple times through the day. To
provide a seamless expirience to users for this, it's important to support good
desktop clients across multiple platforms (macOS, linux). More over, some
platforms have custom ways to integrate with their desktop interface, like gnome
shell extensions, KDE widgets, or macOS menu bar widgets. We want to provide a
similar user expirience across platforms, while avoiding having to re-write
code in different langauges.

Note: Another option is to use a cross-platform framework like
[Electron](https://www.electronjs.org/), but that would mean a non-native
expirience for the users, and limited scope of integrating with platform
features.

## Decision

1. We will build clients for MacOS, GTK, and a Gnome shell extension (GSE).
2. We will have a single core library that can be reused across multiple clients. This
   library would abstract over common functionality like interacting with HTTP
   and websocket APIs, authenticating requests, maintain any local state
   necessary, etc.
3. We will create bindings for different clients in their native languages to provide
   good expirience. This would mean
   - Building bindings to inter-operate with Swift for MacOS
   - Building bindings to inter-operate with GTK libraries.
   - Building a communication pipeline over DBus for the GSE.
4. We will use [Rust](https://www.rust-lang.org/) as the language to build the
   core library.

## Consequences

- Different clients would have a similar release cycles.
- The team will need to ramp up on Rust and it's ecosystem.
- We will still need to maintain some code for different clients (as against
  using something like Electron).
