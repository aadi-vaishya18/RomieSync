// utils.js — small shared helpers.

const crypto = require("crypto");

function generateId() {
  return crypto.randomUUID();
}

function generateInviteCode() {
  // 6-character, human-friendly (no ambiguous 0/O/1/I characters).
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars[crypto.randomInt(0, chars.length)];
  }
  return code;
}

function nowIso() {
  return new Date().toISOString();
}

/**
 * Single source of truth for task progress. Every place in the backend and
 * frontend that needs a completed count, a total count, or a percentage
 * MUST go through this function — this is exactly what the spec's Bug 2
 * requires: one calculation, never duplicated.
 */
function computeProgress(tasks) {
  const total = tasks.length;
  const completed = tasks.filter((t) => !!t.completed).length;
  const percent = total === 0 ? 0 : Math.round((completed / total) * 100);
  return { completed, total, percent };
}

module.exports = { generateId, generateInviteCode, nowIso, computeProgress };
