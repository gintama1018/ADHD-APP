# CircuitSense — Design Document

Pages, user flow, and visual design system for the remaining 5-day build.
Everything here is scoped to what's realistic to build solo in the time
left — no page or interaction listed here should require more than a
day's work, and STRETCH items are clearly marked so they get cut first
if time runs short.

---

## 1. Design Philosophy

The product exists to hold the attention of ADHD / low-concentration
learners through a static concept (Ohm's Law) that's normally taught
with a flat, frozen diagram. Every design decision should answer one
question: **does this make the circuit feel alive, or does it make it
look like a textbook page?**

Three rules that govern every screen:
1. **Round, not sharp.** Rounded corners, rounded wires, rounded UI
   chrome. Sharp right angles read as "technical schematic," which is
   exactly the static feeling we're designing away from.
2. **One character carries the story.** Sparky (the current character)
   should appear in some form on almost every screen — as a static
   icon on the capture screen, animated during playback, idle-bouncing
   during narration pauses. Consistency here is what makes it feel like
   a "tutor" rather than a "tool."
3. **Motion has a reason.** Every animation ties to a concept: Sparky
   speeds up when current increases, squishes when hitting resistance,
   glows brighter near the battery. Motion for decoration (without a
   physics reason) is not worth the build time this week.

---

## 2. Full Page / Screen List

Tagged MUST (needed for a complete demo) or STRETCH (cut first if
behind schedule).

| # | Screen | Status | Priority |
|---|--------|--------|----------|
| 1 | Capture / Home Screen | Built (needs cartoon re-skin) | MUST |
| 2 | Detection / Overview Screen | Built (needs cartoon re-skin + real tab logic) | MUST |
| 3 | Tutor Playback Screen (the animated cartoon core) | Not built | MUST |
| 4 | JSON Inspector (bottom sheet, not a full page) | Not built | MUST |
| 5 | Ask a Doubt / AI Chat Screen | Not built | STRETCH |
| 6 | Splash / Launch Screen | Not built | CUT — skip entirely, go straight to Capture Screen on launch |

Only 4 real screens plus one bottom sheet. Do not add more pages than
this list — every extra screen is time stolen from polish.

---

## 3. User Flow

```
[App Launch]
      |
      v
[1. Capture / Home Screen]
   - User either:
       a) Points camera at a printed/hand-drawn circuit and taps capture
       b) Taps one of the curated presets (9V/100ohm, 24V/60ohm, etc.)
      |
      v
[Recognition runs — brief loading state, ~1-2 sec]
      |
      v
[2. Detection / Overview Screen]
   - Shows the recognized circuit (now in cartoon style)
   - Shows the Ohm's Law badge (V, I, R)
   - Shows detection banner ("Circuit detected: 3V battery, 15ohm resistor...")
   - If recognition failed / fell back to defaults, banner explicitly
     says so — never silently substitutes values
   - User taps "Play" / a beat tab, OR the app auto-advances after 2 sec
      |
      v
[3. Tutor Playback Screen]  <-- this is the core deliverable
   - 4-beat animated sequence plays automatically:
       Beat 1: zoom to battery, Sparky appears, "current is born"
       Beat 2: Sparky travels along the wire
       Beat 3: zoom to resistor, Sparky squishes, callout explains resistance
       Beat 4: zoom out, full loop animates continuously
   - Synced narration (TTS) plays under each beat
   - Playback controls: play/pause, speed (1x/1.5x), replay
   - Floating action button: "View JSON" opens the JSON Inspector sheet
      |
      +---> [4. JSON Inspector Sheet] (modal, dismissible, does not
      |      navigate away from playback)
      |
      +---> [5. Ask a Doubt] (STRETCH — only if Day 4 goes well)
      |      Simple chat screen, opened via a button after playback
      |      finishes. Fully separate flow; failure here should never
      |      block returning to Capture Screen.
      |
      v
[Loop / Replay / "Scan another circuit" -> back to Screen 1]
```

Key principle: **the user should reach the actual animation (Screen 3)
within 2 taps of opening the app.** Every extra step between launch and
"seeing Sparky move" is attention lost — exactly what we're designing
against.

---

## 4. Visual Design System

### Color Palette
| Role | Color | Hex |
|------|-------|-----|
| Background (primary) | Deep navy/charcoal | `#0D1321` |
| Background (elevated/cards) | Slightly lighter navy | `#1B2333` |
| Primary accent (current/electricity) | Electric blue | `#2EC5FF` |
| Secondary accent (energy/resistance) | Warm amber/orange | `#FF9F45` |
| Success / detection confirmed | Soft green | `#4ADE80` |
| Warning / fallback-used | Muted yellow | `#FACC15` |
| Text (primary) | Off-white | `#F1F5F9` |
| Text (secondary/muted) | Cool gray | `#94A3B8` |

Keep this palette identical to what's already partially visible in the
built screens (the blue/dark theme is already correct — extend it, don't
replace it).

### Typography
- Headers: bold, slightly rounded sans-serif (e.g. system default with
  `FontWeight.Bold`) — large, confident, never more than 4-5 words.
- Body / narration subtitles: medium weight, high-contrast, generous
  line spacing (subtitles need to be readable at a glance, not read
  carefully — ADHD-friendly means skimmable).
- Numbers (V, I, R values): monospace or tabular figures so values
  don't visually "jump" when they update.

### Shape Language
- All cards, buttons, and component boxes: `RoundedCornerShape(16-24dp)`
  minimum. No sharp rectangles anywhere in the cartoon-facing screens.
- Wires: draw as gently curved paths (slight bezier bow), not perfectly
  straight lines — straight lines read as "schematic," curves read as
  "friendly."
- Battery and resistor icons: simplify the classic symbols into rounded,
  slightly chunky shapes — think "friendly toy circuit" rather than
  "IEC standard symbol."

### Iconography
Match the tab icons already visible in the built screens — keep this
convention, it's working:
- Battery -> ⚡ (lightning bolt)
- Wire -> 🔵 (blue dot / current)
- Resistor -> 🔥 (friction/heat)
- Overview -> a simple loop/circuit icon

---

## 5. Character Design — "Sparky"

Sparky is the animated current character that travels through the
circuit. Keep the design deliberately simple — this is a 1-day build,
not a character-art project.

**Base shape:** a filled circle, electric blue (`#2EC5FF`), roughly
24-32dp diameter, with:
- Two small white circles (eyes) positioned in the upper half
- A single curved line (mouth) that changes shape per emotional state
- A soft outer glow (low-alpha blur/shadow) to read as "energy," not
  "ball"

**Expression states (tied to beats, not decoration):**
| Beat | Expression | How to fake it cheaply |
|------|-----------|------------------------|
| Battery (born) | Excited, wide-eyed | Eyes slightly larger, mouth in a big open curve |
| Wire transit | Calm, content | Default eyes, gentle smile curve |
| Resistor (collision) | Squished, strained | Scale Sparky's shape non-uniformly (squash horizontally) using a Compose scale animation, mouth flattens into a line |
| Full loop | Steady, flowing | Default expression, but multiple faint trailing copies behind it to imply continuous motion (simple alpha-faded duplicates along the recent path) |

This whole character can be built with Canvas `drawCircle` + `drawArc`
calls — no image assets, no external art tools needed, which matters
given the time left.

---

## 6. Screen-by-Screen UI Notes

### Screen 1 — Capture / Home
- Keep existing camera viewfinder + preset picker layout (it already
  works per the current build).
- Re-skin only: round the viewfinder frame corners further, add a small
  idle Sparky icon near the capture button (static, just a friendly
  presence, not animated here).
- Preset buttons: round pill shape, already close to this — just confirm
  consistent corner radius.

### Screen 2 — Detection / Overview
- Re-skin the diagram itself using the shape language above (curved
  wires, rounded component icons) instead of the current straight-line
  technical schematic look.
- Keep the Ohm's Law badge and detection banner — both are working and
  add credibility. Just restyle to match the palette/typography above.
- Make sure the fallback-value warning (Section 0 of plan.txt) has a
  clear visual treatment here — a distinct banner color (use the
  "Warning" yellow from the palette) so it's never confused with a
  successful detection (green).

### Screen 3 — Tutor Playback (build from scratch)
- Full-bleed Canvas taking most of the screen for the animation.
- Narration subtitle bar pinned to the bottom third, semi-transparent
  dark background so it's readable over any animation content behind it.
- Playback controls (play/pause, speed, replay) as a slim control bar
  just above the subtitle area — reuse the control icons already
  visible in the current build's Screen 2, they just need to be wired
  to real playback state here.
- "View JSON" as a small floating action button, top-right corner,
  unobtrusive.

### Screen 4 — JSON Inspector (bottom sheet)
- Simple modal bottom sheet, dark card background, monospace JSON text
  with basic syntax coloring if time allows (keys in blue, values in
  amber) — skip syntax coloring entirely if it costs more than 20
  minutes, plain monospace text is still a strong credibility signal.

### Screen 5 — Ask a Doubt (STRETCH only)
- Minimal chat UI: a scrollable message list + a text input at the
  bottom. Do not attempt anything more elaborate than this — no typing
  indicators, no rich formatting, given the time budget.

---

## 7. Animation & Interaction Principles

- **Ease everything.** Use `EaseInOutCubic` or similar for all camera
  zooms/pans and Sparky's movement — linear motion reads as robotic and
  undercuts the "friendly tutor" feeling.
- **Squash and stretch at the resistor beat** is the single highest-value
  animation detail for selling the "cartoon" feel in a demo video —
  prioritize this over any other polish if time is short.
- **Don't animate UI chrome.** Buttons, controls, and text should appear
  instantly/fade quickly — save animation budget entirely for Sparky
  and the camera choreography, since that's what the judges are
  actually evaluating.

---

## 8. What NOT to Design (scope discipline)

- No onboarding/tutorial screens — the app should be self-explanatory
  from the Capture screen alone.
- No settings/preferences screen.
- No user accounts, login, or persistence beyond the current session.
- No additional circuit types or component icons beyond battery,
  resistor, and wire — even in the design system, don't design icons
  for components that aren't in scope.
