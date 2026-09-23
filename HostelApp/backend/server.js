// server.js — entry point. Wires together auth, hostel, room, and task routes.

const express = require("express");
const cors = require("cors");

const authRoutes = require("./routes/authRoutes");
const hostelRoutes = require("./routes/hostelRoutes");
const roomRoutes = require("./routes/roomRoutes");
const taskRoutes = require("./routes/taskRoutes");

const app = express();
const PORT = process.env.PORT || 4000;

app.use(cors());
app.use(express.json());

app.get("/health", (req, res) => res.json({ ok: true, service: "HostelApp backend" }));

app.use("/auth", authRoutes);
app.use("/hostels", hostelRoutes);
app.use("/rooms", roomRoutes);
app.use("/rooms", taskRoutes);

// Central error handler — never leak stack traces to the client.
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: "Something went wrong on the server." });
});

app.listen(PORT, () => {
  console.log(`HostelApp backend listening on http://localhost:${PORT}`);
});
