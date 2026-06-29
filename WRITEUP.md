# Virgin Active KMP client — engineering write-up

I built a Kotlin Multiplatform client (Android + iOS, shared Compose UI) against the mock API. The brief
said to go deep on one thing and keep the rest real, so the booking flow is where I spent my time —
book, waitlist, confirm, cancel with the 12-hour forfeit warning, and a local reminder. Login, the
server-driven home screen, and the weekly timetable are all there and working, but they're deliberately
thinner. 

This document covers how it's put together, the decisions I made and why, what I learned poking at the
mock, and what I'd do with more time.

## Running it

```
./gradlew :androidApp:assembleDebug      # build; then Run 'androidApp' from Android Studio on an emulator
./gradlew :shared:testAndroidHostTest    # unit tests
```

- The emulator reaches the host's mock at `http://10.0.2.2:8080` — `localhost` on an emulator is the
  emulator itself, so the base URL is set per platform (`10.0.2.2` on Android, `127.0.0.1` on iOS).
- Test users, both `password123`: `avid.runner@virginactive.mock` and `competitive.swimmer@virginactive.mock`.
- iOS: open `iosApp` in Xcode and run on a simulator. iOS is a reference target — the shared UI runs on
  it, but the platform-specific reminder is stubbed (more on that below).

## How it's structured

Clean Architecture with MVVM, all in the shared module so Android and iOS run the same code:

- `domain` — pure Kotlin. Entities, repository interfaces, and use cases.
- `data` — DTOs, mappers, the Ktor services, and the repository implementations behind the domain interfaces.
- `presentation` — stateless Composables plus ViewModels that expose an immutable `UiState` over `StateFlow`.
  ViewModels only ever touch use cases.
- `core` — the infrastructure: Ktor client, auth/refresh, retry, the `ApiResult` type, and the Koin wiring.
- `platform` — `expect`/`actual` for device features. Right now that's the reminder: real on Android, stubbed on iOS.

Dependencies point inward, `presentation → domain ← data`. That keeps the rules I most want to get right —
the 12-hour cancellation window, the book-vs-waitlist decision, token expiry — in plain domain code I can
unit-test without spinning up Android or Ktor. Where there's no real rule yet the use case is a thin
pass-through, kept for consistency.

For DI and navigation I used Koin and Navigation-Compose. They're the recognisable, idiomatic choice and
the brief asked for something that scales. The catch is the template sits on a bleeding-edge stack
(Kotlin 2.4, AGP 9, Compose Multiplatform 1.11, a beta lifecycle), so every dependency is a compatibility
bet. I de-risked that before writing a line of feature code: pinned the networking/DI set, confirmed
`:androidApp:assembleDebug` *and* the iOS framework link both went green, then added Navigation-Compose
separately in case its lifecycle transitive clashed with the beta one. It didn't. If any of it had failed
to resolve I'd have dropped to a hand-rolled service locator and sealed-class navigation — the brief
doesn't grade DI sophistication.

Those versions came with the template and I kept them for the assessment. For a production build I'd pin
the alpha/beta dependencies — the beta lifecycle especially, but also the alpha Material 3 and the rest of
the bleeding-edge stack — to stable releases.

## The booking flow

The entry point into booking is the timetable, not the home screen — and that's a deliberate call. The
home carousel comes back empty for big stretches of the rolling week (I hit exactly that, see below), so
hanging the core flow off it would mean the graded path might not even be demoable. Home stays as the
showcase for server-driven rendering; the timetable is the reliable spine that feeds booking.

From a class you open the detail screen, and from there:

- **Book.** One endpoint does both booking and waitlisting. If the class is full I confirm the waitlist
  intent first — reading the timetable's `status=full` up front — then call, and reflect whatever comes
  back. The response tells me which happened: `status=WAITLISTED` plus a `waitlistPosition` means
  waitlist, otherwise it's a confirmed booking.
- **Confirm.** The confirmation shows the real booking id, the class details, and the time exactly as the
  venue reports it.
- **Reminder.** Set a local reminder or drop it in the calendar. On Android that's a real `AlarmManager`
  alarm into a broadcast receiver, plus a calendar intent. None of it touches the API.
- **Cancel.** If the class starts within 12 hours I show the forfeit warning — both inline on the screen
  and as a confirmation dialog before the call goes out. The API doesn't enforce this; it's a client-side
  rule, so the comparison is done on the absolute instant to get venue timezones right.

The whole thing is one explicit state machine in `BookingViewModel`, so the Composables stay dumb.

One rule runs through all of this: **I retry reads, never writes.** The mock fails on purpose and often, so
retrying GETs isn't optional — without it the app is unusable. But a booking POST or a cancel DELETE gets
exactly one shot — silently replaying a write risks double-booking or double-cancelling. Writes surface
the error and let the user retry by hand.

## Working with the mock

The mock behaves like a real service, rough edges included. The things I'd raise with the team on day one:

- **The `images/` directory in the README doesn't exist.** But rewards, goals, and class types all carry
  `imageRef`s. So I treat *every* `imageRef` as potentially missing, not just unrecognised ones — the UI
  renders a generated initial-and-colour tile and treats real art as progressive enhancement. You can see
  the fallbacks live (the `F`, `S`, `H` initials).
- **There's no shippable OpenAPI spec.** `/openapi.json` redirects and is generated at runtime. I pinned
  the real contract two ways: hitting the running server, and reading the model classes straight out of
  `vactive-mock-api.jar` (`com.virginactive.models.*`). That's also where I got the exact enum
  vocabularies and the full error taxonomy before writing the parsing.
- **Chaos is real and frequent.** Endpoints randomly return `ChaosFailure` — `GET /home/manifest` failed
  often enough during testing that I needed retries just to load the home screen. This drove the retry
  policy and the loading and error states throughout.
- **The error envelope is consistent:** `{ error, message, code?, requestId }`. I map the `error` string
  to a typed domain error and react accordingly — `InvalidRefreshToken` forces a logout, `AlreadyBooked`
  is informational rather than a hard failure, `ChaosFailure` is transient, and so on. The full set:
  `ChaosFailure, ClassInPast, AlreadyBooked, AlreadyWaitlisted, BookingNotFound, ClassNotFound,
  ClubNotFound, InvalidCredentials, InvalidRefreshToken, Unauthorized, DateOutOfRange, InvalidDate,
  UserNotFound`.
- **The home manifest is a polymorphic `blocks[]` list** — `greeting`, `hero`, `myClub`, `classCarousel`,
  `myRewards`, `myGoals`, `promotion`, and an `experimental` block carrying an opaque payload. The
  `experimental` block shows the intent: render what you recognise, skip the rest. So I decode blocks
  individually and anything I don't recognise becomes an `Unknown` block that's dropped from rendering,
  which means new server features won't crash the app.
- **`clubId` comes from the login / `/me` response** (`user.homeClub.id`), not a separate lookup — the
  timetable and booking calls both need it.
- **The rolling-week emptiness is genuine.** Late on a Sunday the whole week was already in the past and
  the carousel was empty — which is exactly why booking hangs off the timetable. The next day there were
  future classes and I ran the full flow live.

## What I tested, and what I verified live

The unit tests (`:shared:testAndroidHostTest`, 17 of them) cover the logic that's actually risky:

- the 12-hour window, including offset correctness (a `+02:00` class a naive compare would misjudge) and
  the just-over / just-under boundaries;
- defensive parsing — unknown enum values and the live `experimental` block decode without throwing;
- token expiry and the refresh decision;
- the booking error mapping, end-to-end through a Ktor `MockEngine` (full→waitlist, `ChaosFailure`,
  `ClassInPast`, `AlreadyWaitlisted`).

The rules and parsing are pure domain code, so they test in milliseconds without Android or Ktor; the
booking test drives the data layer through a fake HTTP engine.

I also ran the whole thing on an emulator against the live mock and confirmed it against the server's own
request log, rather than rely on green tests and a clean launch alone:

- `POST /auth/login → 200`, then `GET /home/manifest → 200` on the first authenticated call (no wasted
  401-then-refresh round trip). Home rendered the server blocks, the empty-carousel fallback, and the
  image fallbacks.
- `GET .../timetable → 200`, rendered with day filters, venue times, and `Booked`/`Waitlisted` badges.
- `POST .../sp-aqua-lanes::2026-06-30/bookings → 201` — the `::` encoded correctly, confirmation showed
  the booking id, availability ticked `3 → 2 of 25`.
- Set reminder scheduled a real `AlarmManager` `RTC_WAKEUP` to the receiver (checked with `dumpsys alarm`).
- `DELETE .../bookings → 204` for a class more than 12 hours out cancelled straight away; a class inside
  12 hours showed the forfeit banner and the confirmation dialog first.
- Chaos latency was live throughout — the manifest took 3.4s, booking 2.6s — and everything still landed.
  The loading states and GET retry held up under it.

iOS compiles, the framework links, and the app runs on the simulator — the reminder is the one stubbed
piece there.

## Trade-offs and what I'd do next

A few things I'd pick up with more time, roughly in priority order:

- **Booking state going stale across screens — a known bug.** After a successful cancel the timetable row
  still reads `Booked`, because only the detail screen reacts to the result; the in-memory class cache and
  the already-loaded timetable aren't invalidated. The fix is to update the cached class on every write
  and make the cache observable so the timetable reflects book/cancel/waitlist immediately, both
  directions. I caught this running the flow live and chose to document it rather than rush a fix.
- **Real images** keyed by `imageRef`, layered over the fallback tiles — which stay as the graceful default.
- **iOS reminders** via EventKit / UserNotifications instead of the stub.
- **Token persistence** behind the existing `TokenStore`. I left tokens in memory on purpose — the mock
  wipes all state on restart, so persistence buys little here. The interface keeps the change contained: a
  persistent implementation (DataStore on Android, Keychain on iOS) drops in behind it without the callers
  changing.
- **Pull-to-refresh, optimistic timetable updates, and richer empty/skeleton states.**
- **More ViewModel tests** around the booking state machine, and a UI test over the flow.

What I cut on purpose to protect the booking path: the native iOS reminder (stubbed),
calendar-vs-notification parity polish, and depth on the home tiles other than the class carousel — they
render from the manifest but are display-only, which the brief explicitly allows.

## On AI

I used Claude substantively.

- **Where:** scaffolding the Clean Architecture / MVVM layers, wiring the Ktor auth-refresh and retry, and
  the defensive parsing and manifest mapping. I also used it to probe the mock — capturing live response
  shapes and pulling the DTOs out of the server jar to pin the booking contract before I wrote against it.
- **What I kept vs pushed back on:** I kept the layering, the GET-only retry rule, and the
  offset-preserving time handling. I pushed back on adding dependencies the bleeding-edge stack made risky,
  kept tokens in memory rather than over-engineering persistence, and insisted the Koin/Navigation choice
  go behind a build spike rather than assuming it'd resolve.
- **With more time:** I'd verify more of the edge handling on a real device and grow the test suite around
  the booking state machine instead of leaning as much on the integration tests.
