// routes/hostelRoutes.js — create hostel, join via invite code, list members,
// list/create rooms, group chat. Every read/write here is scoped to hostels
// the caller actually belongs to — enforced server-side via auth.js middleware.

const express = require("express");
const repo = require("../repo");
const { generateId, generateInviteCode, nowIso, computeProgress } = require("../utils");
const { requireAuth, requireHostelMember, requireHostelAdmin } = require("../auth");

const router = express.Router();

// POST /hostels — create a new hostel group. Creator becomes its admin.
router.post("/", requireAuth, (req, res) => {
  const { name } = req.body || {};
  if (!name || typeof name !== "string" || !name.trim()) {
    return res.status(400).json({ error: "Hostel name is required." });
  }

  const hostel = {
    id: generateId(),
    name: name.trim(),
    inviteCode: generateInviteCode(),
    createdAt: nowIso(),
    createdBy: req.userId,
  };
  repo.insertHostel(hostel);
  repo.insertHostelMember({
    id: generateId(), userId: req.userId, hostelId: hostel.id, role: "admin", joinedAt: nowIso(),
  });

  res.status(201).json({ hostel });
});

// POST /hostels/join — join an existing hostel using its invite code.
// Simple, immediate-join model (no approval queue) per "keep permissions
// simple initially" — an approval queue is a natural next step, not required now.
router.post("/join", requireAuth, (req, res) => {
  const { inviteCode } = req.body || {};
  if (!inviteCode || typeof inviteCode !== "string") {
    return res.status(400).json({ error: "An invite code is required." });
  }

  const hostel = repo.findHostelByInviteCode(inviteCode.trim().toUpperCase());
  if (!hostel) {
    return res.status(404).json({ error: "Invalid invite code." });
  }

  const existing = repo.findHostelMembership(hostel.id, req.userId);
  if (existing) {
    return res.status(200).json({ hostel, alreadyMember: true });
  }

  repo.insertHostelMember({
    id: generateId(), userId: req.userId, hostelId: hostel.id, role: "member", joinedAt: nowIso(),
  });

  res.status(200).json({ hostel, alreadyMember: false });
});

// GET /hostels/mine — list hostels the current user belongs to.
router.get("/mine", requireAuth, (req, res) => {
  const hostels = repo.listHostelsForUser(req.userId);
  res.json({ hostels });
});

// GET /hostels/:hostelId — details (members-only).
router.get("/:hostelId", requireAuth, requireHostelMember, (req, res) => {
  const hostel = repo.findHostelById(req.params.hostelId);
  if (!hostel) return res.status(404).json({ error: "Hostel not found." });
  res.json({ hostel, myRole: req.hostelMembership.role });
});

// GET /hostels/:hostelId/members — members-only.
router.get("/:hostelId/members", requireAuth, requireHostelMember, (req, res) => {
  const members = repo.listHostelMembers(req.params.hostelId);
  res.json({ members });
});

// GET /hostels/:hostelId/invite-code — admin-only (don't leak the join code to every member).
router.get("/:hostelId/invite-code", requireAuth, requireHostelAdmin, (req, res) => {
  const hostel = repo.findHostelById(req.params.hostelId);
  res.json({ inviteCode: hostel.inviteCode });
});

// GET /hostels/:hostelId/rooms — members-only. Includes each room's progress
// via the single shared computeProgress() function.
router.get("/:hostelId/rooms", requireAuth, requireHostelMember, (req, res) => {
  const rooms = repo.listRoomsByHostel(req.params.hostelId);
  const withProgress = rooms.map((room) => {
    const tasks = repo.listTasksByRoom(room.id);
    const memberCount = repo.countRoomMembers(room.id);
    return { ...room, progress: computeProgress(tasks), memberCount };
  });
  res.json({ rooms: withProgress });
});

// POST /hostels/:hostelId/rooms — admin-only: create a new room.
router.post("/:hostelId/rooms", requireAuth, requireHostelAdmin, (req, res) => {
  const { name } = req.body || {};
  if (!name || typeof name !== "string" || !name.trim()) {
    return res.status(400).json({ error: "Room name is required." });
  }
  const room = {
    id: generateId(),
    hostelId: req.params.hostelId,
    name: name.trim(),
    createdAt: nowIso(),
  };
  repo.insertRoom(room);
  res.status(201).json({ room });
});

// GET /hostels/:hostelId/messages — members-only group chat history.
router.get("/:hostelId/messages", requireAuth, requireHostelMember, (req, res) => {
  const messages = repo.listMessagesByHostel(req.params.hostelId);
  res.json({ messages });
});

// POST /hostels/:hostelId/messages — members-only: send a message.
router.post("/:hostelId/messages", requireAuth, requireHostelMember, (req, res) => {
  const { message } = req.body || {};
  if (!message || typeof message !== "string" || !message.trim()) {
    return res.status(400).json({ error: "Message cannot be empty." });
  }
  const row = {
    id: generateId(),
    hostelId: req.params.hostelId,
    userId: req.userId,
    message: message.trim().slice(0, 2000),
    createdAt: nowIso(),
  };
  repo.insertMessage(row);

  res.status(201).json({
    message: { id: row.id, message: row.message, createdAt: row.createdAt, userId: req.userId, userName: req.user.name },
  });
});

module.exports = router;
