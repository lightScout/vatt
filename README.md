# Vatt — Virgin Active KMP client

A Kotlin Multiplatform (Android + iOS) client for the Virgin Active assessment mock API, with a
shared Compose Multiplatform UI. Depth is on the **booking flow**; other features are thin but real.

> **The full write-up — architecture, decisions, API observations, trade-offs, AI usage — is in
> [WRITEUP.md](WRITEUP.md).** Start there.

## Quick start

1. **Run the mock API** (from the assessment distribution): `./start.command` → serves
   `http://localhost:8080`. Use test user `avid.runner@virginactive.mock` / `password123`.
2. **Run the Android app:** open in Android Studio and Run `androidApp` on an emulator
   (or `./gradlew :androidApp:installDebug`). The emulator reaches the host mock at `10.0.2.2:8080`
   (configured automatically).
3. **Run tests:** `./gradlew :shared:testAndroidHostTest`
4. **iOS** (reference target): open `iosApp` in Xcode and run on a simulator (`127.0.0.1:8080`).

## Architecture

Clean Architecture + MVVM, all in `shared`:
`presentation` (Compose + ViewModels) → `domain` (entities, use cases, repository interfaces) ← `data`
(Ktor, DTOs, mappers, repository impls). See [WRITEUP.md](WRITEUP.md) for the full breakdown.

## What's implemented

- Login with bearer-token auth + automatic `/auth/refresh` and forced logout on refresh failure.
- Server-driven home screen (renders whatever `blocks` the API returns; unknown blocks skipped).
- Weekly timetable with day filters — the entry point into booking.
- **Booking flow (the focus):** book / join waitlist (with confirm) / confirmation with booking id /
  cancel with a within-12h forfeit warning / set a local reminder or add to calendar.
- Defensive networking: typed errors, GET-only retry (never retries booking/cancel writes), lenient JSON.
