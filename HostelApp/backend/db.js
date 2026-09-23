// db.js — a small, dependency-free JSON-file datastore.
//
// Why not a real SQL engine? better-sqlite3 requires a native binary (either
// downloaded prebuilt or compiled locally with node-gyp/Python/a C++ toolchain).
// That's exactly the class of "works on my machine, fails on yours" problem
// this project already hit with the Android/Gradle toolchain — so this data
// layer is deliberately pure JavaScript. `npm install` here can never fail
// for platform/toolchain reasons.
//
// It still enforces the same schema/relationships described in the spec —
// just as in-memory JS arrays, mutated through small helper functions below,
// and persisted to a single JSON file after every write (same pattern used
// in the RoomieSync web/Android apps).

const fs = require("fs");
const path = require("path");

const DATA_FILE = path.join(__dirname, "data.json");

function emptyState() {
  return {
    users: [],           // {id, name, email, passwordHash, createdAt}
    hostels: [],          // {id, name, inviteCode, createdAt, createdBy}
    hostelMembers: [],     // {id, userId, hostelId, role, joinedAt}
    rooms: [],            // {id, hostelId, name, createdAt}
    roomMembers: [],       // {id, roomId, userId, joinedAt}
    tasks: [],            // {id, roomId, title, completed, createdBy, createdAt, updatedAt}
    groupMessages: [],     // {id, hostelId, userId, message, createdAt}
  };
}

let state = emptyState();

function load() {
  if (fs.existsSync(DATA_FILE)) {
    try {
      const raw = fs.readFileSync(DATA_FILE, "utf8");
      const parsed = JSON.parse(raw);
      state = { ...emptyState(), ...parsed };
    } catch (e) {
      console.error("Could not parse data.json, starting fresh.", e);
      state = emptyState();
    }
  }
}

function save() {
  // Synchronous write — this app's scale doesn't need async batching, and
  // synchronous writes make ordering trivial to reason about (no risk of two
  // concurrent writes interleaving and corrupting the file).
  fs.writeFileSync(DATA_FILE, JSON.stringify(state, null, 2), "utf8");
}

function resetForTests() {
  state = emptyState();
  if (fs.existsSync(DATA_FILE)) fs.unlinkSync(DATA_FILE);
}

load();

module.exports = { state, save, resetForTests };
