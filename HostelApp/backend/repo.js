// repo.js — query/mutation helpers over db.state (plain JS arrays).
// Every function here mirrors exactly one thing the SQL version used to do,
// so the route files' logic and behavior are unchanged — only how the data
// is stored and queried has changed.

const db = require("./db");

function byId(collection, id) {
  return db.state[collection].find((row) => row.id === id) || null;
}

// ---------- Users ----------
function findUserByEmail(email) {
  return db.state.users.find((u) => u.email === email) || null;
}
function findUserById(id) {
  return byId("users", id);
}
function insertUser(user) {
  db.state.users.push(user);
  db.save();
}

// ---------- Hostels ----------
function insertHostel(hostel) {
  db.state.hostels.push(hostel);
  db.save();
}
function findHostelById(id) {
  return byId("hostels", id);
}
function findHostelByInviteCode(code) {
  return db.state.hostels.find((h) => h.inviteCode === code) || null;
}
function listHostelsForUser(userId) {
  const memberships = db.state.hostelMembers
    .filter((m) => m.userId === userId)
    .sort((a, b) => (a.joinedAt < b.joinedAt ? 1 : -1)); // DESC by joinedAt
  return memberships.map((m) => {
    const hostel = findHostelById(m.hostelId);
    return { ...hostel, myRole: m.role };
  }).filter((h) => h.id); // guard against orphaned membership rows
}

// ---------- Hostel Members ----------
function insertHostelMember(row) {
  db.state.hostelMembers.push(row);
  db.save();
}
function findHostelMembership(hostelId, userId) {
  return db.state.hostelMembers.find((m) => m.hostelId === hostelId && m.userId === userId) || null;
}
function listHostelMembers(hostelId) {
  const memberships = db.state.hostelMembers
    .filter((m) => m.hostelId === hostelId)
    .sort((a, b) => (a.joinedAt > b.joinedAt ? 1 : -1)); // ASC by joinedAt
  return memberships.map((m) => {
    const user = findUserById(m.userId);
    return { id: user.id, name: user.name, email: user.email, role: m.role, joinedAt: m.joinedAt };
  });
}

// ---------- Rooms ----------
function insertRoom(room) {
  db.state.rooms.push(room);
  db.save();
}
function findRoomById(id) {
  return byId("rooms", id);
}
function listRoomsByHostel(hostelId) {
  return db.state.rooms
    .filter((r) => r.hostelId === hostelId)
    .sort((a, b) => a.name.localeCompare(b.name));
}

// ---------- Room Members ----------
function insertRoomMember(row) {
  db.state.roomMembers.push(row);
  db.save();
}
function findRoomMembership(roomId, userId) {
  return db.state.roomMembers.find((m) => m.roomId === roomId && m.userId === userId) || null;
}
function listRoomMembers(roomId) {
  const memberships = db.state.roomMembers
    .filter((m) => m.roomId === roomId)
    .sort((a, b) => (a.joinedAt > b.joinedAt ? 1 : -1));
  return memberships.map((m) => {
    const user = findUserById(m.userId);
    return { id: user.id, name: user.name };
  });
}
function countRoomMembers(roomId) {
  return db.state.roomMembers.filter((m) => m.roomId === roomId).length;
}

// ---------- Tasks ----------
function insertTask(task) {
  db.state.tasks.push(task);
  db.save();
}
function listTasksByRoom(roomId) {
  return db.state.tasks
    .filter((t) => t.roomId === roomId)
    .sort((a, b) => (a.createdAt > b.createdAt ? 1 : -1));
}
function findTask(taskId, roomId) {
  return db.state.tasks.find((t) => t.id === taskId && t.roomId === roomId) || null;
}
function updateTaskCompleted(taskId, completed, updatedAt) {
  const task = byId("tasks", taskId);
  if (task) {
    task.completed = completed ? 1 : 0;
    task.updatedAt = updatedAt;
    db.save();
  }
  return task;
}
function deleteTask(taskId) {
  const idx = db.state.tasks.findIndex((t) => t.id === taskId);
  if (idx !== -1) {
    db.state.tasks.splice(idx, 1);
    db.save();
  }
}

// ---------- Group Messages ----------
function insertMessage(row) {
  db.state.groupMessages.push(row);
  db.save();
}
function listMessagesByHostel(hostelId) {
  const messages = db.state.groupMessages
    .filter((m) => m.hostelId === hostelId)
    .sort((a, b) => (a.createdAt > b.createdAt ? 1 : -1))
    .slice(-200); // most recent 200, oldest first
  return messages.map((m) => {
    const user = findUserById(m.userId);
    return { id: m.id, message: m.message, createdAt: m.createdAt, userId: m.userId, userName: user ? user.name : "Unknown" };
  });
}

module.exports = {
  findUserByEmail, findUserById, insertUser,
  insertHostel, findHostelById, findHostelByInviteCode, listHostelsForUser,
  insertHostelMember, findHostelMembership, listHostelMembers,
  insertRoom, findRoomById, listRoomsByHostel,
  insertRoomMember, findRoomMembership, listRoomMembers, countRoomMembers,
  insertTask, listTasksByRoom, findTask, updateTaskCompleted, deleteTask,
  insertMessage, listMessagesByHostel,
};
