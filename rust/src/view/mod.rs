//! The view: shows model data and reports what the user did.
//!
//! Empty until phase 2. It will hold a `View` trait that the controller talks to,
//! the console renderers for the day, week and month views, and later the egui GUI
//! (phase 5), which sends user actions to the controller as messages, the Rust
//! counterpart of the Java app's `PropertyChange` events.
//!
//! A view reads model types to display them but never changes the model: every
//! change goes through the controller.
