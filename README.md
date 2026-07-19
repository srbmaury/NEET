# Neet

A NEET (India medical entrance exam) prep app: an Android app (Kotlin, Jetpack Compose) backed by
a Kotlin server (Ktor) that generates unlimited practice questions, mock tests, and topic
notes/flashcards via OpenAI — all with real rendered LaTeX, not just plain text.

The full official NEET syllabus is covered across Physics, Chemistry, Botany, and Zoology.
Practice is adaptive (difficulty is chosen for you, weighted toward Medium/Hard, and never repeats
a question you've already seen on that topic), progress is tracked per-subject with full mistake
review, and an optional account syncs your history across devices. A deployed backend is live by
default, so the Android app runs out of the box with no setup — running your own backend is only
needed if you want to develop against it.

## Features

- **Practice**: pick a subject/topic, answer AI-generated MCQs with formatted explanations.
  Difficulty is adaptive — chosen automatically per question from a distribution weighted toward
  Medium/Hard, shifted further by your accuracy on that topic — never a manual picker.
- **Smart Practice**: an auto-built 10-question queue spanning your weakest topics across all four
  subjects, ranked by exam weightage and accuracy.
- **Timed Sprint**: 10 questions on one topic, a 10-minute countdown, no negative marking — a
  lighter-weight drill than a full mock test.
- **Mock Test**: full timed, multi-subject tests matching the real exam's section structure and
  scoring, with a read-only review mode afterward.
- **Notes**: on-demand concept/formula sheets per topic (cached server-side after first
  generation, so every student after the first pays no extra cost), plus a flashcard mode built
  from the same generation.
- **Quick Reference**: a hand-authored (not AI-generated) sheet of physical constants,
  trigonometry, vectors, and calculus — accuracy matters too much here to risk on a model.
- **Progress**: per-subject accuracy, a full history of answered questions (tap any to reopen its
  full explanation), a "Mistakes to review" shortlist, and a syllabus-wide coverage heatmap.
- **Revision Tracker**: log revision counts per topic — both syllabus topics and your own custom
  ones.
- **Accounts** (optional): practice works fully without signing in; signing in syncs your full
  local history to the backend so it survives a reinstall or follows you to another device.

## Why a backend at all?

An OpenAI API key embedded directly in an Android APK can be extracted and stolen. The backend
holds the key server-side and the app only ever talks to your own backend.

## Prerequisites

- JDK 17+ (project was built and tested against JDK 26 with Kotlin 2.4.10 / AGP 9.3.0)
- Android Studio (or just the Android SDK + command-line tools) with SDK Platform 34+ and an
  emulator or device
- An OpenAI API key (https://platform.openai.com) — this is never bundled into the app
- A Postgres database — only needed if you're running your own backend (see below). A free
  [Neon](https://neon.tech) instance works well; any Postgres host does.

## Running the Android app (no backend setup needed)

The debug build points at a deployed Render backend by default
(`android/app/build.gradle.kts` → `BASE_URL`), so you can build and run the app immediately — no
backend, no API key, no database. This is the fastest way to just try the app.

Either open `android/` in Android Studio and hit Run, or from the command line (with an emulator
already running, or a device connected):

```
cd android
export JAVA_HOME=$(/usr/libexec/java_home)   # if gradlew can't find a JDK
./gradlew assembleDebug
./gradlew installDebug
```

Run your own backend instead if you want to develop against backend changes, tune the prompts, or
avoid depending on the shared deployed instance.

## Running your own backend

```
cd backend
cp .env.example .env
# edit .env: set OPENAI_API_KEY, DATABASE_URL (a Postgres connection string), and JWT_SECRET
./gradlew run
```

The server starts on `http://localhost:8080` by default (change `PORT` in `.env` if that port is
taken). It creates its own tables on first run (`SchemaUtils.createMissingTablesAndColumns`) — no
separate migration step. Verify it's working:

```
curl -X POST http://localhost:8080/questions/generate \
  -H "Content-Type: application/json" \
  -d '{"subject":"BOTANY","topic":"Photosynthesis"}'
```

You should get back a JSON question with a stem, 4 options, a correct option key, and a
three-part Markdown explanation.

To point the Android app at this local backend instead of the deployed one:

- **Emulator**: set `BASE_URL` in `android/app/build.gradle.kts`'s `debug` block to
  `http://10.0.2.2:PORT/` — that's the emulator's special alias for your host machine.
- **Physical device**: `10.0.2.2` won't work; point `BASE_URL` at your machine's LAN IP (or a
  tunnel like ngrok/cloudflared) instead.

## Repo layout

```
Neet/
  backend/   # Ktor server: OpenAI calls, Postgres persistence, auth, exposes the HTTP API
  android/   # Jetpack Compose app: bottom-nav tabs for Practice / Mock Test / Notes / Progress
```

See each module for more implementation detail; the backend's `PromptBuilder.kt` and
`OpenAiSchema.kt` are the two files that control question/notes quality and format if you want to
tune those.

## Backend API

All routes are unauthenticated except `/sync/*`, which requires a JWT from `/auth/login` or
`/auth/signup` (sent as `Authorization: Bearer <token>`).

| Route | Purpose |
| --- | --- |
| `POST /questions/generate` | Generate one MCQ for a subject/topic/difficulty, avoiding stems the caller has already seen |
| `POST /mock-test/generate` | Generate a full batch of questions for a mock test (up to 250 slots) |
| `GET /notes/{subject}/{topic}` | Get (or generate and cache) a topic's concept/formula sheet + flashcard data |
| `POST /auth/signup`, `POST /auth/login` | Account creation / login, returns a JWT |
| `POST /sync/push`, `GET /sync/pull` | Push/pull a full local-history snapshot for the signed-in account |

## Implementation notes

- **Persistence**: local history lives in Room on-device (`android/.../data/local/`) so the app is
  fully usable without an account. The backend's Postgres database stores accounts, synced
  history snapshots, and the notes cache — see `backend/.../db/Tables.kt`. Every answered question
  is recorded in full (stem, all options, your answer, the correct answer, explanation), which
  powers both the Progress tab and the "avoid repeating a question you've already seen on this
  topic" behavior (the app sends its recent stems for the subject+topic back to the backend, which
  asks the model to avoid duplicates — see `excludeStems` in both `Models.kt` files and
  `PromptBuilder.kt`).
- **LaTeX** renders via Markwon's `ext-latex` plugin (`android/.../ui/components/MarkdownText.kt`),
  applied to question stems, options, and explanations alike. The renderer's convention is double
  `$$` for both inline and block math, which is non-standard — models drift back to standard
  single-`$`, `\(...\)`/`\[...\]`, or even bare LaTeX commands with no delimiter at all, despite
  explicit prompt instructions. `MarkdownText.kt` normalizes all of these to double-`$$` as a
  defensive fallback, applied only to text outside already-valid `$$` blocks so a correctly
  delimited formula never gets re-matched and corrupted.
- **Answer verification**: after generating a question, the backend makes a second OpenAI call
  (`OpenAiClient.verifyAnswer`) that is shown the stem, options, and the first call's proposed
  answer/explanation, and is told to independently re-derive the answer itself and override the
  proposal if they disagree or the first explanation was self-contradictory. This roughly doubles
  latency and API cost per question but meaningfully improves answer reliability; see
  `PromptBuilder.verificationSystemPrompt` to tune it.
- **Prefetching**: the next question in a session (plain Practice's "Next question", Smart
  Practice, Timed Sprint) is generated in the background while the current one is still on screen,
  so it's usually already sitting ready by the time you advance — see `prefetchNext` in the
  respective ViewModels. Timed Sprint's countdown only starts once the first question has actually
  finished loading, not at session start.
- **Adaptive difficulty**: chosen by weighted-random selection (`domain/DifficultyAdvisor.kt`),
  shifted by the topic's recent accuracy, with Medium+Hard always outweighing Easy — not a strict
  threshold, so repeated practice on the same topic doesn't always land the same difficulty.
- **Notes caching**: `GET /notes/{subject}/{topic}` is a global cache keyed by (subject, topic),
  not per-user — the content is identical for every student, so it's generated once ever and
  served from Postgres for every subsequent request.
- **Topic list** (`android/.../domain/TopicCatalog.kt`) covers the full official NEET syllabus,
  not just a sample, each tagged with an approximate exam weightage (HIGH/MEDIUM/LOW) used to
  prioritize Smart Practice and the coverage heatmap.
- **Navigation**: a 4-tab bottom nav (Practice / Mock Test / Notes / Progress) is the primary way
  to move around the app, backed by a single `NavHost` in `NeetNavHost.kt`. Secondary features
  (Smart Practice, Quick Reference, Coverage Heatmap, Revision Tracker) live behind a single
  "More" screen off the Practice tab rather than adding more bottom-nav tabs.

## What's not here yet (by design)

- Payments/subscriptions
- Play Store release signing/hardening
- Push notifications / reminders
- Offline question generation (practice requires network connectivity)
