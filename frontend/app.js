const API = "http://localhost:8080/api";
const state = { token: localStorage.getItem("skillswap_token"), user: null, authMode: "login", skills: [], requests: [] };
const $ = (id) => document.getElementById(id);

function toast(message) {
  const el = $("toast"); el.textContent = message; el.classList.add("show");
  setTimeout(() => el.classList.remove("show"), 2600);
}
async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(`${API}${path}`, { ...options, headers });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || "Request failed. Is the backend running?");
  return data;
}
function setAuthMode(mode) {
  state.authMode = mode;
  document.querySelectorAll(".auth-tab").forEach(b => b.classList.toggle("active", b.dataset.auth === mode));
  $("nameLabel").classList.toggle("hidden", mode !== "register");
  $("authTitle").textContent = mode === "login" ? "Welcome back" : "Create your account";
  $("authSubtitle").textContent = mode === "login" ? "Log in to continue your learning journey." : "Join the community and start exchanging skills.";
  $("authSubmit").textContent = mode === "login" ? "Log in" : "Create account";
  $("authMessage").textContent = "";
}
document.querySelectorAll(".auth-tab").forEach(b => b.addEventListener("click", () => setAuthMode(b.dataset.auth)));

$("authForm").addEventListener("submit", async e => {
  e.preventDefault(); $("authMessage").textContent = "";
  try {
    const payload = { email: $("authEmail").value.trim(), password: $("authPassword").value };
    if (state.authMode === "register") payload.name = $("authName").value.trim();
    const data = await api(`/auth/${state.authMode}`, { method: "POST", body: JSON.stringify(payload) });
    state.token = data.token; state.user = data.student;
    localStorage.setItem("skillswap_token", state.token);
    await showApp();
    toast(state.authMode === "login" ? "Welcome back!" : "Your SkillSwap account is ready!");
  } catch (err) { $("authMessage").textContent = err.message; }
});

function logout() {
  state.token = null; state.user = null;
  localStorage.removeItem("skillswap_token");
  $("appView").classList.add("hidden"); $("mainNav").classList.add("hidden");
  $("logoutBtn").classList.add("hidden"); $("authView").classList.remove("hidden");
  $("navUser").textContent = ""; $("authForm").reset();
}
$("logoutBtn").addEventListener("click", logout);

function navigate(view) {
  document.querySelectorAll(".view").forEach(v => v.classList.add("hidden"));
  $(`${view}View`).classList.remove("hidden");
  document.querySelectorAll(".nav-link").forEach(b => b.classList.toggle("active", b.dataset.view === view));
  if (view === "dashboard") loadDashboard();
  if (view === "discover") loadDiscover();
  if (view === "skills") loadSkills();
  if (view === "requests") loadRequests();
}
document.querySelectorAll(".nav-link").forEach(b => b.addEventListener("click", () => navigate(b.dataset.view)));
document.querySelectorAll("[data-go]").forEach(b => b.addEventListener("click", () => navigate(b.dataset.go)));

async function showApp() {
  $("authView").classList.add("hidden"); $("appView").classList.remove("hidden");
  $("mainNav").classList.remove("hidden"); $("logoutBtn").classList.remove("hidden");
  $("navUser").textContent = state.user.name;
  $("welcomeTitle").textContent = `Welcome, ${state.user.name.split(" ")[0]}!`;
  await loadDashboard();
}
async function loadDashboard() {
  try {
    const [me, skills, matches, requests] = await Promise.all([api("/me"), api("/skills"), api("/matches"), api("/requests")]);
    state.user = me; state.skills = skills; state.requests = requests;
    $("teachCount").textContent = skills.filter(s => s.skillType === "TEACH").length;
    $("learnCount").textContent = skills.filter(s => s.skillType === "LEARN").length;
    $("matchCount").textContent = matches.length;
    $("requestCount").textContent = requests.filter(r => r.status === "PENDING").length;
  } catch (err) { toast(err.message); if (err.message.includes("log in")) logout(); }
}
async function loadSkills() {
  try {
    const [me, skills] = await Promise.all([api("/me"), api("/skills")]);
    state.user = me; state.skills = skills;
    $("college").value = me.college || ""; $("department").value = me.department || ""; $("bio").value = me.bio || "";
    renderSkillGroup("teachSkills", skills.filter(s => s.skillType === "TEACH"));
    renderSkillGroup("learnSkills", skills.filter(s => s.skillType === "LEARN"));
  } catch (err) { toast(err.message); }
}
function renderSkillGroup(id, list) {
  const container = $(id);
  if (!list.length) { container.innerHTML = '<span class="muted">No skills added yet.</span>'; return; }
  container.innerHTML = list.map(s => `<span class="skill-tag">${escapeHtml(s.skillName)} <button title="Remove skill" data-delete-skill="${s.id}">×</button></span>`).join("");
  container.querySelectorAll("[data-delete-skill]").forEach(b => b.addEventListener("click", async () => {
    try { await api(`/skills/${b.dataset.deleteSkill}`, { method: "DELETE" }); await loadSkills(); await loadDashboard(); toast("Skill removed."); }
    catch (err) { toast(err.message); }
  }));
}
$("skillForm").addEventListener("submit", async e => {
  e.preventDefault(); $("skillMessage").textContent = "";
  try {
    await api("/skills", { method: "POST", body: JSON.stringify({ skillName: $("skillName").value, skillType: $("skillType").value }) });
    $("skillForm").reset(); await loadSkills(); await loadDashboard(); toast("Skill added to your profile.");
  } catch (err) { $("skillMessage").textContent = err.message; }
});
$("profileForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    await api("/me", { method: "PUT", body: JSON.stringify({ college: $("college").value, department: $("department").value, bio: $("bio").value }) });
    $("profileMessage").textContent = "Profile saved successfully."; $("profileMessage").style.color = "#25835b";
  } catch (err) { $("profileMessage").textContent = err.message; }
});
async function loadDiscover() {
  try {
    const [matches, students] = await Promise.all([api("/matches"), api("/students")]);
    $("matchList").innerHTML = matches.length ? matches.map(s => personCard(s, true)).join("") : '<div class="empty">No skill matches yet. Add skills you teach and want to learn to discover compatible peers.</div>';
    $("studentList").innerHTML = students.length ? students.map(s => personCard(s, false)).join("") : '<div class="empty">No other students have joined yet. Invite a friend to try SkillSwap!</div>';
    bindRequestButtons();
  } catch (err) { toast(err.message); }
}
function personCard(s, isMatch) {
  const teach = (s.skills || []).filter(x => x.skillType === "TEACH");
  const learn = (s.skills || []).filter(x => x.skillType === "LEARN");
  const matching = (s.matchSkills || []);
  const skillToRequest = matching[0] || (teach[0] && teach[0].skillName) || "";
  return `<article class="person-card"><div class="avatar">${escapeHtml((s.name || "?").charAt(0).toUpperCase())}</div><h3>${escapeHtml(s.name)}</h3><p class="muted">${escapeHtml([s.department,s.college].filter(Boolean).join(" · ") || "Student community member")}</p>${s.bio ? `<p class="muted">${escapeHtml(s.bio)}</p>` : ""}${isMatch ? `<div class="chips">${matching.map(x => `<span class="chip">${escapeHtml(x)} · can teach</span>`).join("")}${(s.reciprocalSkills||[]).map(x => `<span class="chip green">${escapeHtml(x)} · wants to learn</span>`).join("")}</div>` : `<div class="chips">${teach.slice(0,4).map(x=>`<span class="chip">${escapeHtml(x.skillName)}</span>`).join("")}${learn.slice(0,3).map(x=>`<span class="chip green">Wants: ${escapeHtml(x.skillName)}</span>`).join("")}</div>`}<button class="btn btn-primary full" data-request-to="${s.id}" data-request-skill="${escapeHtml(skillToRequest)}">Send learning request</button></article>`;
}
function bindRequestButtons() {
  document.querySelectorAll("[data-request-to]").forEach(b => b.addEventListener("click", async () => {
    const skill = b.dataset.requestSkill;
    if (!skill) { toast("This student has not listed a teachable skill yet."); return; }
    const message = prompt(`Add a short message about why you want to learn ${skill} (optional):`) || "";
    try {
      await api("/requests", { method: "POST", body: JSON.stringify({ receiverId: b.dataset.requestTo, skill, message }) });
      toast("Learning request sent!"); await loadDashboard();
    } catch (err) { toast(err.message); }
  }));
}
async function loadRequests() {
  try {
    const list = await api("/requests"); state.requests = list;
    $("requestList").innerHTML = list.length ? list.map(r => {
      const person = r.direction === "SENT" ? `To ${r.receiverName}` : `From ${r.senderName}`;
      let actions = "";
      if (r.direction === "RECEIVED" && r.status === "PENDING") actions = `<button class="btn btn-primary" data-status-id="${r.id}" data-status="ACCEPTED">Accept</button><button class="btn btn-outline" data-status-id="${r.id}" data-status="REJECTED">Decline</button>`;
      if (r.direction === "SENT" && r.status === "ACCEPTED") actions = `<button class="btn btn-primary" data-status-id="${r.id}" data-status="COMPLETED">Mark completed</button>`;
      return `<article class="request-card"><div><strong>${escapeHtml(r.skill)}</strong><p>${escapeHtml(person)} · ${escapeHtml(r.message || "No message added")}</p><span class="status ${r.status}">${r.status}</span></div><div class="request-actions">${actions}</div></article>`;
    }).join("") : '<div class="empty">No learning requests yet. Visit Discover to connect with students.</div>';
    $("requestList").querySelectorAll("[data-status-id]").forEach(b => b.addEventListener("click", async () => {
      try {
        await api(`/requests/${b.dataset.statusId}/status`, { method: "PUT", body: JSON.stringify({ status: b.dataset.status }) });
        await loadRequests(); await loadDashboard(); toast("Request updated.");
      } catch (err) { toast(err.message); }
    }));
  } catch (err) { toast(err.message); }
}
function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, ch => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;" }[ch]));
}
$("refreshDiscover").addEventListener("click", loadDiscover);
$("refreshRequests").addEventListener("click", loadRequests);

(async function init() {
  if (!state.token) return;
  try {
    state.user = await api("/me");
    await showApp();
  } catch { logout(); }
})();
