# RoomieSync — Multi-Room, Multi-User Hostel Task Management

RoomieSync rebuilt to match the "Hostel Task Management App — Bug Fixes +
MultiRoom + Group System" spec: real authentication, hostel groups with
invite codes, multiple rooms per hostel, room-scoped tasks with a fixed
progress calculation, and a hostel-wide group chat — all with authorization
enforced on the backend, not just hidden in the UI.

## What's actually running here

Unlike everything built earlier in this project, this is a real **client +
server** application, served entirely from one place:

- **`backend/`** — a Node.js + Express API. This has to be running for the
  app to work at all.
- **`frontend/`** — a Progressive Web App (installable, with an offline-capable
  app shell) that the backend serves directly at its own address — there's
  no separate frontend server and no hardcoded IP to edit.

## Why no database server, no Docker, nothing to install beyond Node

The data layer (`backend/db.js` + `backend/repo.js`) is deliberately **pure
JavaScript** — it stores everything in one `data.json` file, with no native
binary dependencies at all. This was a direct decision after hitting real
native-compilation failures with `better-sqlite3` during testing (the same
class of problem as the Android/Gradle native-toolchain issues earlier in
this project) — so this backend installs and runs identically on any
machine with Node.js, no C++ build tools, no Python, no gyp.

## How to run it

1. **Install Node.js** (v18 or later) if you don't have it: https://nodejs.org
2. Open a terminal in the `backend` folder and run:
   ```
   npm install
   npm start
   ```
   You should see: `RoomieSync listening on http://localhost:4000`
3. Open **http://localhost:4000** in your browser (not the HTML file directly —
   the server now serves the frontend itself, from the same address as the API).
4. Sign up, create a hostel (you become its admin automatically), create a
   room from the Profile tab, and start adding tasks.
5. To test multi-user behavior: sign up as a second user in a different
   browser tab (or incognito window), and use "Join with an Invite Code"
   with the code shown on the first user's Profile tab.

## Installing it as an app on your phone (PWA)

1. Make sure your phone is on the **same WiFi network** as the computer
   running the backend.
2. Find your computer's local IP address (Windows: `ipconfig`, look for
   "IPv4 Address"; Mac: System Settings → Wi-Fi → Details).
3. On your phone's browser, go to `http://<that-ip>:4000` — e.g.
   `http://192.168.1.42:4000`.
4. Use your browser's "Add to Home Screen" or "Install app" option (Chrome
   on Android will usually prompt automatically; on iPhone, use Safari's
   Share button → "Add to Home Screen").
5. It now opens from your home screen with its own icon, full-screen, no
   browser bar.

**One real limitation to know about:** service workers (what makes an
"Install app" prompt and offline caching work) require a secure origin —
`https://`, or the literal address `localhost`. A plain `http://` address
on your local network (like `192.168.1.42`) doesn't fully qualify as secure
in Chrome, so on your phone you may get a basic "Add to Home Screen"
shortcut rather than the full install-with-offline-caching experience you'd
get on the same computer at `localhost`. For the complete experience from
anywhere (not just your home WiFi, with full install + offline support),
the backend needs to be deployed somewhere with real HTTPS (Render, Railway,
and Fly.io all have free tiers that provide this automatically) — a real
deployment step I can walk you through if you want to take this further.

## What's been verified (not just written)

I could not compile/run the earlier Android app in this environment, but I
absolutely can run a Node backend — so I did, extensively:

- **34 automated end-to-end tests** (`backend/test_e2e.js`) covering
  every scenario the spec's "Testing Requirements" section lists: signup,
  login, invalid login, invite codes (valid/invalid), non-member rejection
  on hostels/rooms/messages, admin-only actions, the exact progress-bar
  scenarios from the spec (0/1, 1/1, 2/4, uncompleting, deleting), room task
  isolation (Room A never affects Room B), group chat send/receive, and
  multi-hostel privacy (Group A can never see Group B's messages). All 34
  pass.
- **A real headless-browser run** of the actual frontend against the actual
  running backend — signup, hostel creation, room creation, joining a room,
  adding/completing/tasks with live progress-bar screenshots at each state,
  sending a chat message, viewing members. Screenshots confirmed the
  progress bar and the completed/total text never disagree, at every step.
- A fresh `npm install` from a clean `node_modules`-free state, timed at
  about 1 second with zero native compilation.

## Bug fixes from the spec, specifically

**Bug 1 (white-on-white text)**: every input/textarea in the frontend has
an explicit white background, dark text, and a visible focus ring —
declared with `!important` specifically so no other rule can accidentally
make text invisible again. Disabled fields are visibly greyed out rather
than blank.

**Bug 2 (progress bar/counter mismatch)**: there is exactly one function,
`computeProgress()` in `backend/utils.js`, that calculates completed count,
total count, and percentage. Every screen and every API response gets its
numbers from that one function — nothing recalculates it independently, so
the bar and the text can't drift apart.

## Data model

Matches the spec's suggested schema (`backend/db.js`): users, hostels,
hostelMembers (with role: admin/member), rooms, roomMembers, tasks
(room-scoped), groupMessages (hostel-scoped). Passwords are hashed with
bcrypt — never stored in plain text.

## Honest limitations

- **Chat updates via polling** (every 3.5 seconds while the Discuss tab is
  open), not WebSockets — "near-real-time," which the spec explicitly
  allows as an option, but it's not instant push.
- **No join-approval queue.** Anyone with a valid invite code joins
  immediately. The spec allows this ("keep permissions simple initially");
  an approval step is a natural next addition if you want it.
- **JWT stored in memory only**, not localStorage/cookies — refreshing the
  page logs you out. This was a deliberate choice to avoid browser storage
  APIs; ask me to add persistent login if you want it for real use.
- **This runs on your machine only.** Both people need to be able to reach
  the same backend (same machine, or same network with the URL updated in
  `hostelapp.html`) — it isn't deployed anywhere with a public URL. Getting
  a real public URL means deploying the backend somewhere (Render, Railway,
  Fly.io all have free tiers) — a real infrastructure decision I can walk
  you through if you want to take this further.
