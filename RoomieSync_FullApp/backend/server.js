// server.js — entry point. Wires together auth, hostel, room, and task routes,
// and serves the frontend from the same origin (so the PWA can install
// correctly and the frontend never needs a hardcoded backend address).

const express = require("express");
const cors = require("cors");
const path = require("path");

const authRoutes = require("./routes/authRoutes");
const hostelRoutes = require("./routes/hostelRoutes");
const roomRoutes = require("./routes/roomRoutes");
const taskRoutes = require("./routes/taskRoutes");

const app = express();
const PORT = process.env.PORT || 4000;

app.use(cors());
app.use(express.json());

app.get("/health", (req, res) => res.json({ ok: true, service: "RoomieSync backend" }));

app.use("/auth", authRoutes);
app.use("/hostels", hostelRoutes);
app.use("/rooms", roomRoutes);
app.use("/rooms", taskRoutes);

// Serve the PWA frontend from the same origin as the API. This matters for
// two real reasons: (1) the frontend can use relative fetch() paths instead
// of a hardcoded IP, so it works unmodified whether you open it on this
// computer or a phone on the same network; (2) service workers require a
// proper origin (not a double-clicked local file), which is required for
// the "Add to Home Screen" / install experience to work at all.
const frontendDir = path.join(__dirname, "..", "frontend");
app.use(express.static(frontendDir));
app.get("/", (req, res) => res.sendFile(path.join(frontendDir, "index.html")));

// Central error handler — never leak stack traces to the client.
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: "Something went wrong on the server." });
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`RoomieSync listening on http://localhost:${PORT}`);
  console.log(`On your phone (same WiFi), use this computer's local IP instead of localhost.`);
});

