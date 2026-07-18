# Neet

A NEET (India medical entrance exam) practice app: an Android app (Kotlin, Jetpack Compose)
backed by a small Kotlin server (Ktor) that generates unlimited practice questions via OpenAI,
with beautifully formatted, Markdown explanations.

Pick a subject/topic/difficulty (the full official NEET syllabus is covered across Physics,
Chemistry, Botany, and Zoology), get an AI-generated MCQ, answer it, and see a formatted
explanation — with real rendered LaTeX for formulas, not just plain text. Answers are saved
locally (Room) so you can see your accuracy per subject and review exactly which questions you
got wrong and what the right answer was, and the app avoids repeating questions you've already
seen on the same topic. Full mock tests, accounts, and backend-side persistence are intentionally
not built yet (see "What's not here yet" below).

## Why a backend at all?

An OpenAI API key embedded directly in an Android APK can be extracted and stolen. The backend
holds the key server-side and the app only ever talks to your own backend.

## Prerequisites

- JDK 17+ (project was built and tested against JDK 26 with Kotlin 2.4.10 / AGP 9.3.0)
- Android Studio (or just the Android SDK + command-line tools) with SDK Platform 34+ and an
  emulator or device
- An OpenAI API key (https://platform.openai.com) — this is never bundled into the app

## Backend setup

```
cd backend
cp .env.example .env
# edit .env and set OPENAI_API_KEY to your own key
./gradlew run
```

The server starts on `http://localhost:8080` by default (change `PORT` in `.env` if that port is
taken). Verify it's working:

```
curl -X POST http://localhost:8080/questions/generate \
  -H "Content-Type: application/json" \
  -d '{"subject":"BOTANY","topic":"Photosynthesis"}'
```

You should get back a JSON question with a stem, 4 options, a correct option key, and a
three-part Markdown explanation.

## Android app setup

1. Open the `android/` folder in Android Studio (or run the Gradle wrapper directly from
   `android/`).
2. The debug build points at `http://10.0.2.2:8080/` by default — that's the Android emulator's
   special alias for your host machine, matching the backend's default port. If your backend is
   running on a different port (e.g. it was already in use), update the debug `buildConfigField`
   for `BASE_URL` in `android/app/build.gradle.kts` to match.
3. If you're testing on a physical device instead of an emulator, `10.0.2.2` won't work — point
   `BASE_URL` at your machine's LAN IP (or a tunnel like ngrok/cloudflared) instead.
4. Run the app on an emulator or device. Pick a subject and topic, start practicing, answer the
   question, and you should see a formatted explanation with correct/incorrect highlighting.

## Repo layout

```
Neet/
  backend/   # Ktor server: holds the OpenAI key, exposes POST /questions/generate
  android/   # Jetpack Compose app: topic picker -> question -> answer + explanation
```

See each module for more implementation detail; the backend's `PromptBuilder.kt` and
`OpenAiSchema.kt` are the two files that control question quality and format if you want to tune
those.

## Persistence, LaTeX, answer verification, and the topic list

- **Persistence** is entirely on-device (Room, `android/.../data/local/`). The backend stays
  fully stateless — no accounts, no server-side database. Every answered question is recorded in
  full (stem, all options, your answer, the correct answer, explanation) — not just a summary —
  which powers the Progress tab (per-subject accuracy, tap any past answer to reopen it in a
  read-only review screen) and the "avoid repeating a question you've already seen on this topic"
  behavior (the app sends its recent stems for the subject+topic back to the backend, which asks
  the model to avoid duplicates — see `excludeStems` in both `Models.kt` files and
  `PromptBuilder.kt`).
- **LaTeX** renders via Markwon's `ext-latex` plugin (`android/.../ui/components/MarkdownText.kt`),
  applied to question stems, options, and explanations alike. The renderer's convention is double
  `$$` for both inline and block math, which is non-standard — models drift back to standard
  single-`$`, `\(...\)`, or `\[...\]` LaTeX despite explicit prompt instructions, so
  `MarkdownText.kt` normalizes all of these to double-`$$` before rendering as a defensive
  fallback (prompt compliance alone wasn't reliable enough).
- **Answer verification**: after generating a question, the backend makes a second OpenAI call
  (`OpenAiClient.verifyAnswer`) that is shown the stem, options, and the first call's proposed
  answer/explanation, and is told to independently re-derive the answer itself and override the
  proposal if they disagree or the first explanation was self-contradictory (e.g. it second-guesses
  itself mid-explanation without updating the stated answer key). This roughly doubles latency and
  API cost per question but meaningfully improves answer reliability; see
  `PromptBuilder.verificationSystemPrompt` to tune it.
- **Topic list** (`android/.../domain/TopicCatalog.kt`) covers the full official NEET syllabus,
  not just a sample — see that file if the syllabus changes and topics need updating.
- **Navigation**: a bottom nav bar (Practice / Progress) is the primary way to move around the
  app now, backed by a single `NavHost` in `NeetNavHost.kt`.
- **Backup/restore**: the Progress tab has Backup/Restore buttons that export/import your full
  local answer history (including complete question data, not just a summary) as a JSON file via
  Android's system file picker (Storage Access Framework) — useful before reinstalling the app or
  clearing its data. Restore merges non-destructively (matching ids are overwritten, everything
  else is added). Backed by bulk Room APIs (`AnswerDao.insertAll`/`getAllOnce`) and
  `HistoryRepository.exportBackupJson`/`importBackupJson`.
- **Focus areas**: each topic in `TopicCatalog.kt` is tagged with an approximate NEET exam
  weightage (HIGH/MEDIUM/LOW — a qualitative tier based on well-established previous-year
  question-distribution patterns, not exact counts). The Progress tab cross-references this
  against your actual practice history (`AnswerDao.topicStats`) to surface a prioritized list of
  topics worth studying next — favoring important topics you've never attempted or are weak in
  (below 60% accuracy) over ones you're already solid on. Tapping a card jumps straight into
  practicing that topic, skipping the picker. See `StatsViewModel.computeFocusAreas` for the
  ranking logic and `TopicCatalog.weightageOf` for the per-topic tiers.

## What's not here yet (by design)

- Full multi-subject timed mock tests / scoring
- User accounts, auth, or any backend persistence (the backend is fully stateless)
- Payments/subscriptions
- Play Store release signing/hardening
