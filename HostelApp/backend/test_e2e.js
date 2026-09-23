// test_e2e.js — exercises the exact scenarios listed in the spec's
// "Testing Requirements" section against the live running server.

const BASE = "http://localhost:4000";
let pass = 0, fail = 0;

function check(label, cond, extra) {
  if (cond) { pass++; console.log(`  OK   ${label}`); }
  else { fail++; console.log(`  FAIL ${label}` + (extra ? `  -> ${JSON.stringify(extra)}` : "")); }
}

async function api(method, path, body, token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  let json = null;
  try { json = await res.json(); } catch (e) {}
  return { status: res.status, json };
}

async function main() {
  console.log("=== AUTH ===");
  const signupA = await api("POST", "/auth/signup", { name: "Aman", email: "aman@test.com", password: "password123" });
  check("signup creates user + token", signupA.status === 201 && !!signupA.json.token, signupA.json);
  const tokenAman = signupA.json.token;

  const dupSignup = await api("POST", "/auth/signup", { name: "Aman2", email: "aman@test.com", password: "password123" });
  check("duplicate email signup rejected", dupSignup.status === 409, dupSignup.json);

  const badLogin = await api("POST", "/auth/login", { email: "aman@test.com", password: "wrongpass" });
  check("invalid login rejected", badLogin.status === 401, badLogin.json);

  const goodLogin = await api("POST", "/auth/login", { email: "aman@test.com", password: "password123" });
  check("valid login succeeds", goodLogin.status === 200 && !!goodLogin.json.token, goodLogin.json);

  const meNoToken = await api("GET", "/auth/me", null, null);
  check("unauthenticated /auth/me rejected", meNoToken.status === 401, meNoToken.json);

  const me = await api("GET", "/auth/me", null, tokenAman);
  check("authenticated /auth/me works", me.status === 200 && me.json.user.email === "aman@test.com", me.json);

  // Second user for group/isolation testing
  const signupB = await api("POST", "/auth/signup", { name: "Rahul", email: "rahul@test.com", password: "password123" });
  const tokenRahul = signupB.json.token;

  // Outsider (never joins any hostel) for authorization-rejection testing
  const signupC = await api("POST", "/auth/signup", { name: "Outsider", email: "outsider@test.com", password: "password123" });
  const tokenOutsider = signupC.json.token;

  console.log("\n=== HOSTEL GROUP ===");
  const createHostel = await api("POST", "/hostels", { name: "ABC Hostel" }, tokenAman);
  check("create hostel succeeds", createHostel.status === 201 && !!createHostel.json.hostel.inviteCode, createHostel.json);
  const hostelId = createHostel.json.hostel.id;
  const inviteCode = createHostel.json.hostel.inviteCode;

  const badJoin = await api("POST", "/hostels/join", { inviteCode: "WRONGCODE" }, tokenRahul);
  check("invalid invite code rejected", badJoin.status === 404, badJoin.json);

  const goodJoin = await api("POST", "/hostels/join", { inviteCode }, tokenRahul);
  check("valid invite code join succeeds", goodJoin.status === 200 && goodJoin.json.hostel.id === hostelId, goodJoin.json);

  const members = await api("GET", `/hostels/${hostelId}/members`, null, tokenAman);
  check("member list has 2 members", members.status === 200 && members.json.members.length === 2, members.json);

  const outsiderView = await api("GET", `/hostels/${hostelId}`, null, tokenOutsider);
  check("non-member cannot view hostel", outsiderView.status === 403, outsiderView.json);

  const outsiderMessages = await api("GET", `/hostels/${hostelId}/messages`, null, tokenOutsider);
  check("non-member cannot view hostel messages", outsiderMessages.status === 403, outsiderMessages.json);

  const nonAdminInvite = await api("GET", `/hostels/${hostelId}/invite-code`, null, tokenRahul);
  check("non-admin member cannot view invite code", nonAdminInvite.status === 403, nonAdminInvite.json);

  const adminInvite = await api("GET", `/hostels/${hostelId}/invite-code`, null, tokenAman);
  check("admin can view invite code", adminInvite.status === 200 && adminInvite.json.inviteCode === inviteCode, adminInvite.json);

  console.log("\n=== ROOMS ===");
  const nonAdminCreateRoom = await api("POST", `/hostels/${hostelId}/rooms`, { name: "Room 101" }, tokenRahul);
  check("non-admin cannot create room", nonAdminCreateRoom.status === 403, nonAdminCreateRoom.json);

  const createRoom101 = await api("POST", `/hostels/${hostelId}/rooms`, { name: "Room 101" }, tokenAman);
  check("admin can create room", createRoom101.status === 201, createRoom101.json);
  const room101 = createRoom101.json.room.id;

  const createRoom102 = await api("POST", `/hostels/${hostelId}/rooms`, { name: "Room 102" }, tokenAman);
  const room102 = createRoom102.json.room.id;

  const joinRoom101Aman = await api("POST", `/rooms/${room101}/join`, {}, tokenAman);
  check("Aman joins Room 101", joinRoom101Aman.status === 200, joinRoom101Aman.json);

  const joinRoom102Rahul = await api("POST", `/rooms/${room102}/join`, {}, tokenRahul);
  check("Rahul joins Room 102", joinRoom102Rahul.status === 200, joinRoom102Rahul.json);

  const rahulViewsRoom101 = await api("GET", `/rooms/${room101}`, null, tokenRahul);
  check("Rahul (not a Room 101 member) sees room but isMember=false", rahulViewsRoom101.status === 200 && rahulViewsRoom101.json.isMember === false, rahulViewsRoom101.json);

  const rahulAddTaskRoom101 = await api("POST", `/rooms/${room101}/tasks`, { title: "Sneaky task" }, tokenRahul);
  check("non-room-member cannot add tasks to that room", rahulAddTaskRoom101.status === 403, rahulAddTaskRoom101.json);

  console.log("\n=== PROGRESS BAR / TASK COUNTER SYNC (spec's Bug 2 scenarios) ===");

  // 0 tasks
  const emptyTasks = await api("GET", `/rooms/${room101}/tasks`, null, tokenAman);
  check("0 tasks -> 0/0, 0%", emptyTasks.json.progress.completed === 0 && emptyTasks.json.progress.total === 0 && emptyTasks.json.progress.percent === 0, emptyTasks.json.progress);

  // Add 1 task -> 0/1
  const t1 = await api("POST", `/rooms/${room101}/tasks`, { title: "Clean Room" }, tokenAman);
  check("after adding 1 task -> 0/1, 0%", t1.json.progress.completed === 0 && t1.json.progress.total === 1 && t1.json.progress.percent === 0, t1.json.progress);
  const task1Id = t1.json.tasks[0].id;

  // Complete it -> 1/1, 100%
  const t1done = await api("PATCH", `/rooms/${room101}/tasks/${task1Id}`, { completed: true }, tokenAman);
  check("completing the only task -> 1/1, 100%", t1done.json.progress.completed === 1 && t1done.json.progress.total === 1 && t1done.json.progress.percent === 100, t1done.json.progress);

  // Add 3 more tasks (total 4), complete 1 more (total completed 2) -> 2/4, 50%
  await api("POST", `/rooms/${room101}/tasks`, { title: "Buy Supplies" }, tokenAman);
  const t3 = await api("POST", `/rooms/${room101}/tasks`, { title: "Fix Table" }, tokenAman);
  await api("POST", `/rooms/${room101}/tasks`, { title: "Mop Floor" }, tokenAman);
  const t3done = await api("PATCH", `/rooms/${room101}/tasks/${t3.json.tasks.find(t => t.title === "Fix Table").id}`, { completed: true }, tokenAman);
  check("2 of 4 completed -> 2/4, 50%", t3done.json.progress.completed === 2 && t3done.json.progress.total === 4 && t3done.json.progress.percent === 50, t3done.json.progress);

  // Uncomplete the first task -> 1/4, 25%
  const t1undone = await api("PATCH", `/rooms/${room101}/tasks/${task1Id}`, { completed: false }, tokenAman);
  check("uncompleting a task -> 1/4, 25%", t1undone.json.progress.completed === 1 && t1undone.json.progress.total === 4 && t1undone.json.progress.percent === 25, t1undone.json.progress);

  // Delete a completed task -> total drops correctly
  const beforeDelete = await api("GET", `/rooms/${room101}/tasks`, null, tokenAman);
  const anyTask = beforeDelete.json.tasks[0];
  const afterDelete = await api("DELETE", `/rooms/${room101}/tasks/${anyTask.id}`, null, tokenAman);
  check("deleting a task reduces total by 1", afterDelete.json.progress.total === beforeDelete.json.progress.total - 1, afterDelete.json.progress);

  // Complete all remaining -> 100%
  const remaining = await api("GET", `/rooms/${room101}/tasks`, null, tokenAman);
  for (const t of remaining.json.tasks) {
    await api("PATCH", `/rooms/${room101}/tasks/${t.id}`, { completed: true }, tokenAman);
  }
  const allDone = await api("GET", `/rooms/${room101}/tasks`, null, tokenAman);
  check("all tasks completed -> 100%, completed === total", allDone.json.progress.percent === 100 && allDone.json.progress.completed === allDone.json.progress.total, allDone.json.progress);

  console.log("\n=== ROOM TASK ISOLATION (Room A tasks must not affect Room B) ===");
  await api("POST", `/rooms/${room102}/tasks`, { title: "Room 102 task" }, tokenRahul);
  const room101State = await api("GET", `/rooms/${room101}/tasks`, null, tokenAman);
  const room102State = await api("GET", `/rooms/${room102}/tasks`, null, tokenRahul);
  check("Room 101 and Room 102 have independent task lists", room101State.json.tasks.every(t => t.title !== "Room 102 task"), { room101: room101State.json.tasks.map(t=>t.title) });
  check("Room 102 progress unaffected by Room 101 completions", room102State.json.progress.completed === 0 && room102State.json.progress.total === 1, room102State.json.progress);

  console.log("\n=== GROUP CHAT ===");
  const send1 = await api("POST", `/hostels/${hostelId}/messages`, { message: "Does anyone know how to fix the WiFi issue?" }, tokenAman);
  check("member can send message", send1.status === 201, send1.json);
  await api("POST", `/hostels/${hostelId}/messages`, { message: "Try restarting the router." }, tokenRahul);

  const outsiderSend = await api("POST", `/hostels/${hostelId}/messages`, { message: "I shouldn't be able to post this" }, tokenOutsider);
  check("non-member cannot send message", outsiderSend.status === 403, outsiderSend.json);

  const history = await api("GET", `/hostels/${hostelId}/messages`, null, tokenRahul);
  check("message history has 2 messages in order", history.json.messages.length === 2 && history.json.messages[0].message.includes("WiFi"), history.json.messages);

  console.log("\n=== MULTI-HOSTEL ISOLATION ===");
  const createHostel2 = await api("POST", "/hostels", { name: "XYZ Hostel" }, tokenOutsider);
  const hostel2Id = createHostel2.json.hostel.id;
  await api("POST", `/hostels/${hostel2Id}/messages`, { message: "Group B secret message" }, tokenOutsider);

  const amanViewsHostel2 = await api("GET", `/hostels/${hostel2Id}/messages`, null, tokenAman);
  check("Group A member cannot see Group B's messages", amanViewsHostel2.status === 403, amanViewsHostel2.json);

  const myHostels = await api("GET", "/hostels/mine", null, tokenAman);
  check("Aman's hostel list only shows ABC Hostel, not XYZ", myHostels.json.hostels.length === 1 && myHostels.json.hostels[0].name === "ABC Hostel", myHostels.json.hostels);

  console.log(`\n=== RESULTS: ${pass} passed, ${fail} failed ===`);
  process.exit(fail > 0 ? 1 : 0);
}

main().catch((e) => { console.error("TEST SCRIPT CRASHED:", e); process.exit(1); });
