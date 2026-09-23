// routes/roomRoutes.js — join a room, view a room's detail + members + tasks.

const express = require("express");
const repo = require("../repo");
const { generateId, nowIso, computeProgress } = require("../utils");
const { requireAuth } = require("../auth");

const router = express.Router();

// POST /rooms/:roomId/join — self-assign to a room. Requires hostel membership
// (verified by looking up the room's hostel), matching "room.hostelId = user's hostelId".
router.post("/:roomId/join", requireAuth, (req, res) => {
  const room = repo.findRoomById(req.params.roomId);
  if (!room) return res.status(404).json({ error: "Room not found." });

  const hostelMembership = repo.findHostelMembership(room.hostelId, req.userId);
  if (!hostelMembership) {
    return res.status(403).json({ error: "You must join this hostel group before joining one of its rooms." });
  }

  const existing = repo.findRoomMembership(room.id, req.userId);
  if (!existing) {
    repo.insertRoomMember({ id: generateId(), roomId: room.id, userId: req.userId, joinedAt: nowIso() });
  }

  res.json({ room });
});

// GET /rooms/:roomId — full detail: members + tasks + progress.
// Requires the caller to actually be a member of the room's hostel; separately
// reports whether they've joined the room itself (isMember), matching the
// spec's room-level access rule.
router.get("/:roomId", requireAuth, (req, res) => {
  const room = repo.findRoomById(req.params.roomId);
  if (!room) return res.status(404).json({ error: "Room not found." });

  const hostelMembership = repo.findHostelMembership(room.hostelId, req.userId);
  if (!hostelMembership) {
    return res.status(403).json({ error: "You are not a member of this hostel group." });
  }

  const roomMembership = repo.findRoomMembership(room.id, req.userId);
  const members = repo.listRoomMembers(room.id);
  const tasks = repo.listTasksByRoom(room.id);

  res.json({
    room,
    members,
    tasks,
    progress: computeProgress(tasks),
    isMember: !!roomMembership,
  });
});

module.exports = router;
