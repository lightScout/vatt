# Virgin Active KMP Client — Write-up & Decision Log

> Living document. Decisions are recorded **as they are made**, with rationale, so the reasoning is
> captured fresh rather than reconstructed at the end. Final pass tidies voice/structure.

## How to run

**Mock API** (from the distribution folder):
```
./start.command            # macOS; serves http://localhost:8080, Swagger at /swagger
```

**Android client:**
```
./gradlew :androidApp:assembleDebug      # build
# Run from Android Studio on an emulator (Run 'androidApp').
```
- The Android emulator reaches the host's mock at **`http://10.0.2.2:8080`** (configured as the
  Android base URL — `localhost` on the emulator is the emulator itself).
- Test users (both `password123`): `avid.runner@virginactive.mock`, `competitive.swimmer@virginactive.mock`.

**iOS:** open `iosApp` in Xcode and run on a simulator (base URL `http://127.0.0.1:8080`). iOS is included
as a reference target; platform-specific features (reminders) are stubbed on iOS — see decisions.

**Tests:** `./gradlew :shared:testAndroidHostTest`

---

## Architecture

**Clean Architecture + MVVM**, all in the Compose-Multiplatform `shared` module (one UI codebase for
Android + iOS).

- `domain` — pure Kotlin: entities, repository **interfaces**, use cases (business rules). Depends on nothing.
- `data` — DTOs, mappers, Ktor API services, `RepositoryImpl`s implementing the domain interfaces.
- `presentation` — MVVM: stateless Composables + `ViewModel`s exposing immutable `UiState` via `StateFlow`;
  ViewModels depend only on use cases.
- `core` — networking (Ktor client, auth/refresh, retry, `ApiResult`) and DI (Koin).
- `platform` — `expect/actual` for device features (reminders): Android real, iOS stub.

Dependencies point inward (`presentation → domain ← data`). Business rules that carry risk (the 12h
cancellation window, book/waitlist decision, token refresh) live in domain use cases so they're unit-tested
in isolation without Android/Ktor.

---

## Decision log

Format: **Decision · Why · Alternatives · Trade-off accepted.**

- **Clean Architecture + MVVM.** *Why:* brief asks for code that scales/maintains; clean layer boundaries
  make the graded booking logic testable in isolation. *Alternatives:* flat MVVM (faster, less ceremony).
  *Trade-off:* a little boilerplate (thin pass-through use cases where there's no rule yet) for clear seams
  and trivial domain tests.
- **Koin (DI) + Navigation-Compose (nav).** *Why:* idiomatic, recognisable "enterprise" structure; chosen
  by me (the candidate) to demonstrate it. *Alternatives:* hand-rolled service locator + sealed-class nav
  (zero deps, lower compat risk on this bleeding-edge stack). *Trade-off:* extra dependencies to keep
  compatible with Kotlin 2.4 / CMP 1.11; gated behind a build spike with a hand-roll fallback.
- **In-memory tokens behind a `TokenStore` interface.** *Why:* the mock wipes all state on restart, so
  disk persistence has little practical payoff here; the real signal is the **refresh-token flow** wired
  through Ktor's `Auth` plugin. *Alternatives:* multiplatform-settings/DataStore persistence.
  *Trade-off:* tokens don't survive app restart — acceptable given the mock resets anyway; persistence is a
  one-line swap behind the interface.
- **Reminders: Android real, iOS stub.** *Why:* iOS is a reference target; budget is better spent on the
  booking flow than EventKit/permission wiring. *Trade-off:* iOS reminder is a compiling no-op; documented.
- **Booking entry point = Timetable, not Home.** *Why:* the home `classCarousel.items` came back **empty**
  (late-week rolling window — confirmed live, see below). A booking flow hung off Home wouldn't be demoable.
  Home remains the server-driven-rendering showcase; the timetable is the reliable spine into booking.
- **Retry policy: retry idempotent GETs on transient failure; never blind-retry booking POST / cancel
  DELETE.** *Why:* the mock injects `ChaosFailure` on any endpoint, so GET retry is needed just to be
  usable; but retrying a write risks double-booking / double-cancelling. *Trade-off:* writes surface an
  explicit error with a manual retry affordance instead of auto-retrying.
- **Build spike passed (Phase 0).** Pinned against the template's bleeding-edge stack
  (Kotlin 2.4.0 / AGP 9.0.1 / CMP 1.11.1 / lifecycle 2.11.0-beta01): Ktor `3.5.1`, kotlinx-serialization
  `1.11.0`, coroutines `1.11.0`, kotlinx-datetime `0.8.0-0.6.x-compat`, Koin `4.2.2`, Navigation-Compose
  `2.9.2`. *Why staged:* added the networking/DI set first and verified `:androidApp:assembleDebug` **and**
  `:shared:linkDebugFrameworkIosSimulatorArm64` both green before adding Navigation-Compose separately
  (its lifecycle transitive could have clashed with the beta lifecycle). All green on both targets → kept
  the libs; the hand-roll fallback wasn't needed.
- **Defensive JSON:** `ignoreUnknownKeys`, `coerceInputValues`, lenient, `explicitNulls=false`; unknown
  enum values map to an `Unknown` domain fallback rather than throwing. *Why:* brief explicitly warns the
  docs are stale and shapes may change; the live manifest already contains an `experimental` block we must
  skip. *Trade-off:* none meaningful — strictly more robust.

---

## API observations (verified against the live mock + the server jar)

These are the things I'd flag to the team on day one.

1. **The README's `images/` directory doesn't exist** in the distribution, yet class types/rewards/goals
   carry `imageRef`s (e.g. `reward_smoothie`, `goal_spin`). So **every** `imageRef` needs a graceful
   fallback, not just unrecognised ones. The client renders a type/category-derived placeholder (icon +
   colour) and treats real assets as progressive enhancement.
2. **OpenAPI is runtime-only.** `/openapi.json` 302-redirects and is generated by the server; there's no
   shippable spec. I pinned the real contract by hitting the running server and by reading the model
   classes inside `vactive-mock-api.jar` (`com.virginactive.models.*`).
3. **Chaos is real and frequent.** Endpoints randomly return
   `{"error":"ChaosFailure","message":"…temporarily unavailable…","requestId":"…"}`. `GET /home/manifest`
   failed ~1 in 2 calls during testing. This drove the retry-GET / never-retry-writes policy and
   loading/error states everywhere.
4. **Consistent error envelope:** `{ error, message, code?, requestId }`. Full taxonomy from the jar:
   `ChaosFailure, ClassInPast, AlreadyBooked, AlreadyWaitlisted, BookingNotFound, ClassNotFound,
   ClubNotFound, InvalidCredentials, InvalidRefreshToken, Unauthorized, DateOutOfRange, InvalidDate,
   UserNotFound`. The client maps these to typed domain errors and tailored UX (e.g. `InvalidRefreshToken`
   → forced logout; `AlreadyBooked` → informative, not a hard error).
5. **Home manifest is a polymorphic `blocks[]` list.** Observed block `type`s: `greeting`, `hero`,
   `myClub`, `classCarousel` (with `viewAllAction{type:openTimetable, clubId}`), `myRewards`, `myGoals`,
   `promotion`, and an `experimental` block with an opaque `{payload:{kind,title,data}}`. The
   `experimental` block is exactly the "render what you know, skip the rest" test — handled by an
   `Unknown` block that's dropped from rendering.
6. **`clubId` comes from the login/`/me` response** (`user.homeClub.id`, e.g. `club_sea_point`) — it's not
   a separate lookup. The timetable and booking endpoints need it.
7. **Booking contract** (`POST /clubs/{clubId}/classes/{classId}/bookings` → `BookingResponse`):
   `{ bookingId, status: BOOKED|WAITLISTED, waitlistPosition: Int?, classInstance }`. The **single
   endpoint auto-waitlists** when the class is full — waitlist is signalled by `status=WAITLISTED` + a
   non-null `waitlistPosition`. The client reads the timetable `status=full` to warn/offer waitlist
   *before* calling, then reflects the response.
8. **Times carry venue offsets** (e.g. `2026-06-22T07:00:00+02:00`). Rendered **as-is** (no local-tz
   conversion, per brief); the 12h cancellation window compares the offset-aware start to `now`.
9. **`classId` is composite and date-stamped** (e.g. `sp-sunrise-yoga::2026-06-22`) — must be URL-encoded
   in the path (`::` → `%3A%3A`).
10. **Rolling-week emptiness is real.** Tested late on Sun 2026-06-28: the week was `06-22…06-28` and
    *every* class was already in the past, so `classCarousel.items` was empty and live booking/waitlist
    couldn't be exercised without rolling the system clock. I designed the booking state machine against
    the real `BookingResponse` shape (from the jar) instead of forcing a clock change. Booking a past
    class returns `422 ClassInPast`.

### Enum vocabularies (for the team)
`BookingStatus{BOOKED,WAITLISTED}` · `ClassStatus{AVAILABLE,FULL,CANCELLED}` ·
`ClassType{GROUP_WORKOUT,YOGA,SPIN,PILATES,HIIT,SWIMMING}` · `UserBookingStatus{NONE,BOOKED,WAITLISTED}` ·
`MembershipTier{ESSENTIAL,PREMIUM,CLUB}`. JSON uses lower/lowerCamel forms (`groupWorkout`, `available`).

---

## What I'd do next / cut under time
- (to fill as the build progresses)

## AI usage
- (to fill: where used / accepted vs rejected / what I'd do differently)
