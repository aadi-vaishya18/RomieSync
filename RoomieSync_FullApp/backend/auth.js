// auth.js — JWT auth + server-enforced authorization middleware.
// The spec is explicit: "Do not trust the frontend for authorization."
// Every one of these checks runs on the backend against the datastore,
// regardless of what the frontend chooses to show or hide.

const jwt = require("jsonwebtoken");
const repo = require("./repo");

const JWT_SECRET = process.env.JWT_SECRET || "dev-secret-change-in-production-CHANGE-ME";
const TOKEN_EXPIRY = "7d";

function signToken(userId) {
  return jwt.sign({ userId }, JWT_SECRET, { expiresIn: TOKEN_EXPIRY });
}

/** Requires a valid JWT. Attaches req.userId and req.user. */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) {
    return res.status(401).json({ error: "Not authenticated." });
  }
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    const user = repo.findUserById(payload.userId);
    if (!user) return res.status(401).json({ error: "Not authenticated." });
    req.userId = user.id;
    req.user = { id: user.id, name: user.name, email: user.email };
    next();
  } catch (e) {
    return res.status(401).json({ error: "Invalid or expired session." });
  }
}

/** Requires req.userId to be a member of :hostelId. Attaches req.hostelMembership. */
function requireHostelMember(req, res, next) {
  const hostelId = req.params.hostelId;
  const membership = repo.findHostelMembership(hostelId, req.userId);
  if (!membership) {
    return res.status(403).json({ error: "You are not a member of this hostel group." });
  }
  req.hostelMembership = membership;
  next();
}

/** Requires req.userId to be an admin of :hostelId. */
function requireHostelAdmin(req, res, next) {
  const hostelId = req.params.hostelId;
  const membership = repo.findHostelMembership(hostelId, req.userId);
  if (!membership) {
    return res.status(403).json({ error: "You are not a member of this hostel group." });
  }
  if (membership.role !== "admin") {
    return res.status(403).json({ error: "Only a hostel admin can do this." });
  }
  req.hostelMembership = membership;
  next();
}

/**
 * Requires req.userId to be a member of :roomId, AND verifies the room
 * actually belongs to a hostel the user is a member of (defense in depth —
 * matches the spec's "room.hostelId = user's hostelId" rule).
 */
function requireRoomMember(req, res, next) {
  const roomId = req.params.roomId;
  const room = repo.findRoomById(roomId);
  if (!room) return res.status(404).json({ error: "Room not found." });

  const hostelMembership = repo.findHostelMembership(room.hostelId, req.userId);
  if (!hostelMembership) {
    return res.status(403).json({ error: "You are not a member of this hostel group." });
  }

  const roomMembership = repo.findRoomMembership(roomId, req.userId);
  if (!roomMembership) {
    return res.status(403).json({ error: "You are not a member of this room." });
  }

  req.room = room;
  next();
}

module.exports = {
  signToken,
  requireAuth,
  requireHostelMember,
  requireHostelAdmin,
  requireRoomMember,
};
