//! The controller: turns input into operations on the [`model`](crate::model).
//!
//! Empty until phase 2. It will hold the command parser (the same command language
//! as the Java app, so `commands.txt` keeps working), the session that executes
//! parsed commands against a [`Calendars`](crate::model::Calendars), and the
//! headless and interactive runners.
//!
//! The controller depends on the model and on the `View` trait in
//! [`view`](crate::view); it never draws anything itself.
