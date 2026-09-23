// routes/taskRoutes.js — room-scoped task CRUD.
// Every response includes progress computed via the shared computeProgress()
// function — this is what guarantees the bar and the counter can never
// disagree, per the spec's Bug 2 requirement.

const express = require("express");
const repo = require("../repo");
const { generateId, nowIso, computeProgress } = require("../utils");
const { requireAuth, requireRoomMember } = require("../auth");

const router = express.Router();

function tasksAndProgress(roomId) {
  const tasks = repo.listTasksByRoom(roomId);
  return { tasks, progress: computeProgress(tasks) };
}

// GET /rooms/:roomId/tasks
router.get("/:roomId/tasks", requireAuth, requireRoomMember, (req, res) => {
  res.json(tasksAndProgress(req.params.roomId));
});

// POST /rooms/:roomId/tasks
router.post("/:roomId/tasks", requireAuth, requireRoomMember, (req, res) => {
  const { title } = req.body || {};
  if (!title || typeof title !== "string" || !title.trim()) {
    return res.status(400).json({ error: "Task title is required." });
  }
  const ts = nowIso();
  const task = {
    id: generateId(),
    roomId: req.params.roomId,
    title: title.trim(),
    completed: 0,
    createdBy: req.userId,
    createdAt: ts,
    updatedAt: ts,
  };
  repo.insertTask(task);

  res.status(201).json(tasksAndProgress(req.params.roomId));
});

// PATCH /rooms/:roomId/tasks/:taskId — toggle (or explicitly set) completed.
router.patch("/:roomId/tasks/:taskId", requireAuth, requireRoomMember, (req, res) => {
  const task = repo.findTask(req.params.taskId, req.params.roomId);
  if (!task) return res.status(404).json({ error: "Task not found in this room." });

  const nextCompleted =
    typeof req.body?.completed === "boolean" ? req.body.completed : !task.completed;

  repo.updateTaskCompleted(task.id, nextCompleted, nowIso());

  res.json(tasksAndProgress(req.params.roomId));
});

// DELETE /rooms/:roomId/tasks/:taskId
router.delete("/:roomId/tasks/:taskId", requireAuth, requireRoomMember, (req, res) => {
  const task = repo.findTask(req.params.taskId, req.params.roomId);
  if (!task) return res.status(404).json({ error: "Task not found in this room." });

  repo.deleteTask(task.id);
  res.json(tasksAndProgress(req.params.roomId));
});

module.exports = router;
