// routes/authRoutes.js — signup, login, logout, current-user.

const express = require("express");
const bcrypt = require("bcryptjs");
const repo = require("../repo");
const { generateId, nowIso } = require("../utils");
const { signToken, requireAuth } = require("../auth");

const router = express.Router();

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// POST /auth/signup
router.post("/signup", (req, res) => {
  const { name, email, password } = req.body || {};

  if (!name || typeof name !== "string" || !name.trim()) {
    return res.status(400).json({ error: "Name is required." });
  }
  if (!email || !isValidEmail(email)) {
    return res.status(400).json({ error: "A valid email is required." });
  }
  if (!password || typeof password !== "string" || password.length < 6) {
    return res.status(400).json({ error: "Password must be at least 6 characters." });
  }

  const normalizedEmail = email.toLowerCase();
  const existing = repo.findUserByEmail(normalizedEmail);
  if (existing) {
    return res.status(409).json({ error: "An account with this email already exists." });
  }

  const passwordHash = bcrypt.hashSync(password, 10); // never store plain text
  const user = {
    id: generateId(),
    name: name.trim(),
    email: normalizedEmail,
    passwordHash,
    createdAt: nowIso(),
  };
  repo.insertUser(user);

  const token = signToken(user.id);
  res.status(201).json({
    token,
    user: { id: user.id, name: user.name, email: user.email },
  });
});

// POST /auth/login
router.post("/login", (req, res) => {
  const { email, password } = req.body || {};
  if (!email || !password) {
    return res.status(400).json({ error: "Email and password are required." });
  }

  const user = repo.findUserByEmail(email.toLowerCase());
  if (!user) {
    return res.status(401).json({ error: "Invalid email or password." });
  }

  const valid = bcrypt.compareSync(password, user.passwordHash);
  if (!valid) {
    return res.status(401).json({ error: "Invalid email or password." });
  }

  const token = signToken(user.id);
  res.json({
    token,
    user: { id: user.id, name: user.name, email: user.email },
  });
});

// POST /auth/logout
// Stateless JWTs can't be server-invalidated without a blocklist; the
// frontend simply discards the token. This endpoint exists for symmetry
// and so the frontend has a single clear action to call.
router.post("/logout", requireAuth, (req, res) => {
  res.json({ ok: true });
});

// GET /auth/me
router.get("/me", requireAuth, (req, res) => {
  res.json({ user: req.user });
});

module.exports = router;
