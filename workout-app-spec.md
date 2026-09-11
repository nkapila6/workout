# Workout App - Build Spec

A handoff document for a coding agent. Goal: build an Android app from the attached HTML artifact (`home-strength-routine.html`). The artifact is the visual and interaction target. This doc turns it into a real app with tracking, reminders, exercise variations, and AI-generated routines via an OpenRouter key.

---

## 1. What this is

A minimalist home strength-training app for one user (extendable later). It ships with one fixed full-body dumbbell routine, animated exercise demos, a short warm-up, rest timers, progression rules, and lightweight progress tracking. On top of that, the user can generate new routines or swap exercises using their own OpenRouter API key.

Core principle from the source design: **remove decisions**. Same routine every session. The app should never make the user think about what to do next. Show up, follow, done.

---

## 2. Reference artifact

`home-strength-routine.html` is attached. Treat it as the source of truth for:

- Visual design (dark theme, amber accent, card layout, typography)
- The five core exercises, their sets/reps, cues, and animated demos
- The warm-up block
- The "how to not quit" progression and consistency rules

The HTML uses inline SVG with SMIL animation for each exercise figure. In the app these become native animations (see section 6). Do not just embed the HTML in a WebView unless building an MVP quickly. Rebuild it native.

---

## 3. Tech stack

**Primary recommendation:** Kotlin + Jetpack Compose (native Android).

- UI: Jetpack Compose, Material 3 with a custom dark theme
- Local storage: Room (SQLite) for exercises, routines, session logs, bodyweight entries
- Preferences: DataStore for settings and the OpenRouter key (store the key encrypted, see section 8)
- Networking: Retrofit + OkHttp + kotlinx.serialization for the OpenRouter call
- Notifications: AlarmManager + NotificationManager for reminders
- Animation: Compose animation APIs (see section 6)

**Alternative if the agent prefers cross-platform:** Flutter (Dart). Same feature set. Use `flutter_local_notifications`, `sqflite`, `flutter_secure_storage`, and `http`. Animations via `AnimatedBuilder` / custom painters.

Pick one and state which. Do not mix.

---

## 4. Screens

1. **Home / Today**
   - Shows today's routine name, session count this week, a big "Start" button
   - Weekly target indicator (2 to 3 sessions)
   - Quick link to progress and settings

2. **Session flow** (the core loop)
   - Warm-up card first (checklist, no timer needed, tap to continue)
   - Then each exercise one at a time: animated figure, name, target muscle, sets x reps, form cue
   - Log each set (reps done + weight used). Auto-suggest last session's numbers
   - Rest timer between sets (60 to 90 sec default, configurable), with a skip button
   - End screen: session summary, mark complete, updates weekly count

3. **Exercise detail**
   - Full animated demo, expanded cues, a "swap this exercise" button (opens variations, section 7)

4. **Progress**
   - Bodyweight log: weekly weigh-in entry, line chart of the trend (weekly average, not daily noise)
   - Optional progress photos (every 2 weeks), stored locally, side-by-side compare
   - Per-exercise history: weight and reps over time so the user sees the climb

5. **Generate** (AI, section 8)
   - Prompt the user for goals/constraints, call OpenRouter, preview the generated routine, save it

6. **Settings**
   - Reminder time + days (schedule local notifications)
   - Rest timer default
   - OpenRouter key entry + model selector
   - Units (kg default), theme

---

## 5. Data model

```
Exercise
  id: string
  name: string
  muscleGroup: enum (legs, push, pull, hinge, shoulders, core, fullbody)
  cue: string                // short form tip
  animationKey: string       // maps to a built-in animation, or "generic" for AI-made
  difficulty: enum (easier, standard, harder)
  isCustom: bool             // true if AI-generated or user-added

Routine
  id: string
  name: string
  exercises: [ RoutineItem ]
  isCustom: bool

RoutineItem
  exerciseId: string
  sets: int
  repMin: int
  repMax: int
  perSide: bool              // e.g. one-arm row

SessionLog
  id: string
  date: timestamp
  routineId: string
  entries: [ SetEntry ]

SetEntry
  exerciseId: string
  setNumber: int
  reps: int
  weightKg: float

BodyWeightEntry
  date: timestamp
  weightKg: float

Settings
  reminderTime, reminderDays, restSeconds, units, openRouterKey (encrypted), model
```

Seed the DB on first launch with the default routine and the full exercise library (section 7).

---

## 6. Animations

Each core exercise has a looping figure animation showing the movement. In the HTML these are SVG + SMIL. Rebuild as native Compose animations.

Approach: draw each figure as a set of line segments and circles on a `Canvas`, drive the joints with an `infiniteTransition` and `animateFloat`. Interpolate the moving points between two poses (standing/contracted) with an ease-in-out curve, ~2.2 to 2.8 sec per loop. Respect the system "reduce motion" setting by showing a static contracted pose instead.

The five animations to replicate (from the artifact):

1. Goblet squat: hips and knees drop, feet planted, weight held at chest
2. Floor press: figure lying down, arms press dumbbells up and lower
3. One-arm row: hinged figure, one arm pulls the dumbbell up to the hip
4. Romanian deadlift: upper body hinges at the hip, weights slide down and back up
5. Shoulder press: dumbbells press from shoulder height to overhead

For AI-generated or swapped exercises with no bespoke animation, fall back to a generic labeled placeholder (e.g. a still figure with the muscle group highlighted) rather than faking a wrong motion.

---

## 7. Exercise library and variations

Ship the five core moves plus swap options for each. The "swap this exercise" button and the AI generator both draw from this. Group by muscle so any move can be replaced by another in the same group.

**Legs (squat pattern)**
- Standard: Goblet squat
- Easier: Bodyweight squat, Box squat (sit to a chair)
- Harder: Tempo goblet squat (3 sec down), Bulgarian split squat, Front-rack squat

**Push (chest)**
- Standard: Dumbbell floor press
- Easier: Incline pushup (hands on couch), Knee pushup
- Harder: Single-arm floor press, Tempo floor press, Close-grip floor press

**Pull (back)**
- Standard: One-arm dumbbell row
- Easier: Supported two-arm row (light)
- Harder: Chest-supported row, Gorilla row, Tempo row

**Hinge (hamstrings/glutes)**
- Standard: Romanian deadlift
- Easier: Hip hinge to a box, Light kettlebell RDL
- Harder: Single-leg RDL, Deficit RDL, Tempo RDL

**Shoulders**
- Standard: Dumbbell shoulder press
- Easier: Seated shoulder press, Half-kneeling press
- Harder: Arnold press, Push press, Single-arm press

**Optional core add-on (if user wants a sixth move)**
- Plank, Dead bug, Suitcase carry, Hollow hold

Progression rule baked into the app logic: when the user hits the top of the rep range (e.g. all 3 sets at 12 reps) for two sessions running, prompt them to add weight next time, or offer the "harder" variation if they have no heavier dumbbell.

---

## 8. AI generation via OpenRouter

Let the user generate new routines or swap exercises using their own OpenRouter key. Never ship a key in the app.

**Setup**
- Settings screen: text field for the OpenRouter API key, stored via encrypted DataStore / `flutter_secure_storage`
- Model selector: default to a capable, low-cost model. Let the user pick. Populate a short list (e.g. a Claude model, a GPT model, a Llama model) or let them type a model slug.

**Endpoint**
```
POST https://openrouter.ai/api/v1/chat/completions
Headers:
  Authorization: Bearer <user_key>
  Content-Type: application/json
Body:
  {
    "model": "<selected_model>",
    "messages": [
      { "role": "system", "content": "<system prompt below>" },
      { "role": "user", "content": "<user's request + their constraints>" }
    ]
  }
```

**System prompt for generation** (the app sends this, not the user):
> You are a strength coach generating a home dumbbell workout. Return ONLY valid JSON matching the schema, no prose, no markdown fences. Movements must be doable at home with adjustable or fixed dumbbells and bodyweight only. Keep total exercises between 4 and 6. Use standard rep ranges (6 to 15). Every exercise needs a muscleGroup from: legs, push, pull, hinge, shoulders, core. Include a one-line form cue for each.

**Required JSON output schema** (app validates against this before saving; reject and retry once on malformed output):
```json
{
  "name": "string",
  "exercises": [
    {
      "name": "string",
      "muscleGroup": "legs|push|pull|hinge|shoulders|core",
      "sets": 3,
      "repMin": 8,
      "repMax": 12,
      "perSide": false,
      "cue": "string"
    }
  ]
}
```

**Flow**
1. User taps Generate, optionally types constraints ("no jumping, bad knees", "20 min max", "focus on back")
2. App builds the messages array, calls OpenRouter
3. Parse response, strip any accidental code fences, validate JSON against schema
4. Show a preview (same card layout as the core routine). Generated exercises use the generic animation fallback unless their name matches a library entry, in which case reuse that animation
5. User saves it as a new Routine (`isCustom: true`) or discards

**Error handling**
- No key set: route the user to Settings
- 401: tell the user the key is invalid
- Rate limit / network error: show a retry
- Malformed JSON after one retry: show "generation failed, try again"

---

## 9. Reminders

Local notifications, no server. User sets a time and days in Settings. Schedule with AlarmManager (Android) / `flutter_local_notifications`. Notification copy example: "Strength session. Go light, learn the moves." Tapping it opens the session flow. Handle reschedule on device reboot.

---

## 10. Design system (from the artifact)

```
Background   #13151b
Panel        #1b1e26
Panel alt    #20242e
Ink (text)   #eef1f6
Muted        #9aa3b2
Line/border  #2b303c
Accent amber #ffb400
Amber dim    #c98d10
Success green#57d38c
```

- Rounded cards (radius ~18dp), 1px subtle borders
- Bold, tight headings; muted secondary text
- Amber used sparingly for numbers, accents, the primary button, and the animated dumbbell plates
- System sans / default platform font, heavy weights for headings
- Warm-up block gets a faint amber-tinted background to set it apart

---

## 11. Build order (suggested milestones)

1. Static app: seed DB, Home, Session flow with the 5 core exercises and native animations, rest timer, warm-up. No tracking yet.
2. Logging + progression: log sets, per-exercise history, auto-suggest last numbers, progression prompts.
3. Progress screen: bodyweight log + trend chart, optional photos.
4. Reminders.
5. Exercise variations / swap.
6. OpenRouter generation.

Ship milestone 1 as a working MVP before adding the rest.

---

## 12. Prompt variations for handing to an agent

Pick whichever fits the agent you are using. Attach `home-strength-routine.html` and this spec in all cases.

### Variation A - direct and full
> Build an Android app from the attached spec (`workout-app-spec.md`) and reference artifact (`home-strength-routine.html`). Use Kotlin and Jetpack Compose. Follow the spec's screens, data model, design system, and build order. Start by delivering milestone 1 (the working session flow with native animations) and confirm it runs before moving on. Ask me before making any change that deviates from the spec.

### Variation B - MVP-first, iterate
> I want an Android workout app. Attached is a full spec and an HTML artifact that shows the exact look and the five exercises. First, build only the MVP: seed the five exercises, the warm-up, the session flow with rest timers, and native Compose animations matching the artifact. Skip tracking, reminders, and AI for now. Get that compiling and running, show me, then we will add the rest from the spec one milestone at a time.

### Variation C - design-led
> Attached is an HTML artifact for a home strength app and a build spec. Match the artifact's visual design exactly first: the dark theme, amber accent, card layout, and the five animated exercise figures rebuilt as native animations. Nail the look and the session flow, then wire up the data model, tracking, reminders, and the OpenRouter generation feature per the spec. Flag anything in the spec that is unclear or technically risky before you build it.

---

## 13. Notes and constraints

- Single user, offline-first. No accounts, no backend. Everything local except the optional OpenRouter call.
- The OpenRouter key is the user's own. Store it encrypted, never log it, never hardcode one.
- Keep the app small and fast. The whole point of the design is low friction. Do not add social features, streaks that guilt-trip, or ads.
- Units default to kg. Support lb as a setting.
- Respect the system reduce-motion setting for the animations.
