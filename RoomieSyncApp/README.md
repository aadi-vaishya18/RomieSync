# RoomieSync — Native Android App

A real Android app (Kotlin + XML), not a web page. Same product as the earlier
HTML prototype — shared chores and expense tracking for flatmates — rebuilt
using actual Android development tools.

## Tech stack
- **Language:** Kotlin
- **UI:** XML layouts + View Binding (no Compose, no web view)
- **Architecture:** Single Activity + Fragments (Home, Chores, Expenses,
  Settle Up, Profile), Material Components (BottomNavigationView, Chips,
  BottomSheetDialogFragment)
- **Data:** Local only — stored as JSON in Android `SharedPreferences` via
  `org.json` (no external database library, no network calls, no third-party
  dependencies beyond standard AndroidX/Material libraries)
- **Min SDK:** 24 (Android 7.0+) · **Target/Compile SDK:** 34

## How to open and run it
1. Install [Android Studio](https://developer.android.com/studio) (free) if
   you don't have it.
2. **File → Open** → select the `RoomieSyncApp` folder (the one containing
   `settings.gradle.kts`).
3. Let Gradle sync. **Important:** this project does not include
   `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` (they're binary/generated
   files). Android Studio will detect this and either regenerate them
   automatically or prompt you to — just accept it. This is normal and only
   happens once.
4. Run on an emulator or a physical device via the green ▶ Run button.

## What's real here
This is not a mockup screenshot generator — every screen is backed by actual
logic:
- `data/Models.kt` — data classes for Flatmate, Chore, Expense, Payment
- `data/AppRepository.kt` — persistence (SharedPreferences) + the balance
  engine: `computeNet()` and `simplifyDebts()` (a real greedy debt-simplification
  algorithm, same as the web version)
- Five screens, each a real `Fragment` with its own layout and `RecyclerView`
  adapters: `HomeFragment`, `ChoresFragment`, `ExpensesFragment`,
  `SettleUpFragment`, `ProfileFragment`
- Three `BottomSheetDialogFragment`s for adding an expense, a chore, or a
  flatmate

## Honest limitations — please read this before opening a support ticket
1. **I could not compile or run this app myself.** This sandbox has no
   Android SDK and can't download one (network is restricted to package
   registries, not Google's Android repositories). I wrote every file
   carefully and verified:
   - All 47 XML files are well-formed (parsed successfully)
   - Every `binding.xxx` reference in every Kotlin file matches a real
     `android:id` in its corresponding layout (checked programmatically)
   - Every `R.drawable` / `R.color` / `R.style` / `R.string` reference
     resolves to a resource that actually exists (checked programmatically)
   - All Kotlin files have balanced braces/parens/brackets

   What I *couldn't* check: actual Kotlin type errors, Gradle dependency
   resolution, or anything that only a real compiler catches. **If Android
   Studio shows a build error, copy the exact error message back to me and
   I'll fix it** — that's a normal, expected part of shipping real code, not
   a sign something went fundamentally wrong.

2. **No app icon PNGs, only vector drawables.** The launcher icon uses
   `ic_launcher_foreground.xml` / `ic_launcher_background.xml` (Android
   adaptive icon format) rather than raster PNGs at every density. This is a
   fully valid, modern approach — no action needed.

3. **Local storage only.** Data lives in `SharedPreferences` on the device —
   this matches the web prototype's behavior (per-device, not shared between
   flatmates' phones). Making it sync across real people's phones would need
   a real backend, which is out of scope for this file (see our earlier
   conversation about what a production version would require).

## Design system
Same visual identity as the report and the web prototype:
- Harbor Blue `#3A5A78` (primary), Ember Amber `#E08E45` (accent)
- Ink `#24303A` (text), Linen `#F4EFE6` (background)
- Logo mark: roofline + coin badge, as a vector drawable
  (`drawable/ic_logo_mark.xml`) used in the launcher icon and the in-app top bar
