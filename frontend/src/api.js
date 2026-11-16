// All backend calls live here. Uses the browser's built-in fetch.
export const API_ROOT = "http://localhost:8080/api";
const ROOT = API_ROOT;

// ---- login session (kept in the browser so a refresh does not log you out) ----
export const session = {
  token: () => localStorage.getItem("ql_token"),
  user: () => {
    const raw = localStorage.getItem("ql_user");
    return raw ? JSON.parse(raw) : null;
  },
  save: (auth) => {
    localStorage.setItem("ql_token", auth.token);
    localStorage.setItem("ql_user", JSON.stringify({ username: auth.username, fullName: auth.fullName, role: auth.role }));
  },
  clear: () => {
    localStorage.removeItem("ql_token");
    localStorage.removeItem("ql_user");
  },
};

// Logged-in user whose token expired or was rejected: go back to the login page
function handleExpired(res, token) {
  if (res.status === 401 && token) {
    session.clear();
    window.location.hash = "";
    window.location.reload();
  }
}

async function request(path = "", options = {}) {
  // A file upload (FormData) must NOT set Content-Type: the browser adds it with the right boundary
  const headers = options.body instanceof FormData ? {} : { "Content-Type": "application/json" };
  const token = session.token();
  if (token) headers.Authorization = "Bearer " + token;

  let res;
  try {
    res = await fetch(ROOT + path, { ...options, headers });
  } catch {
    throw new Error("Cannot reach the backend on port 8080. Is Spring Boot running?");
  }
  handleExpired(res, token);

  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(body?.error || "Request failed (" + res.status + ")");
  return body;
}

// Downloads a file as a Blob. An <img src> cannot send the login token, so we fetch it ourselves.
async function blob(path) {
  const token = session.token();
  let res;
  try {
    res = await fetch(ROOT + path, { headers: token ? { Authorization: "Bearer " + token } : {} });
  } catch {
    throw new Error("Cannot reach the backend on port 8080. Is Spring Boot running?");
  }
  handleExpired(res, token);
  if (!res.ok) throw new Error("The file could not be loaded (" + res.status + ")");
  return res.blob();
}

const post = (data) => ({ method: "POST", body: JSON.stringify(data) });
const patch = (data) => ({ method: "PATCH", body: data === undefined ? undefined : JSON.stringify(data) });
const form = (obj) => {
  const f = new FormData();
  Object.entries(obj).forEach(([k, v]) => { if (v !== undefined && v !== null) f.append(k, v); });
  return f;
};

export const api = {
  // account
  login: (data) => request("/auth/login", post(data)),
  register: (data) => request("/auth/register", post(data)),
  changePassword: (data) => request("/auth/change-password", post(data)),

  // defects
  list: (status, q) => {
    const p = new URLSearchParams();
    if (status) p.set("status", status);
    if (q) p.set("q", q);
    const qs = p.toString();
    return request("/defects" + (qs ? "?" + qs : ""));
  },
  get: (id) => request("/defects/" + id),
  create: (data) => request("/defects", post(data)),
  update: (id, data) => request("/defects/" + id, { method: "PUT", body: JSON.stringify(data) }),
  remove: (id) => request("/defects/" + id, { method: "DELETE" }),
  setStatus: (id, status) => request("/defects/" + id + "/status", patch({ status })),
  addAction: (id, data) => request("/defects/" + id + "/actions", post(data)),
  completeAction: (id, actionId) => request("/defects/" + id + "/actions/" + actionId + "/complete", patch()),
  removeAction: (id, actionId) => request("/defects/" + id + "/actions/" + actionId, { method: "DELETE" }),
  audit: (id) => request("/defects/" + id + "/audit"),

  // photos and files on a defect
  attachments: (id) => request("/defects/" + id + "/attachments"),
  uploadAttachment: (id, file, shared) =>
    request("/defects/" + id + "/attachments", { method: "POST", body: form({ file, shared }) }),
  removeAttachment: (id, aid) => request("/defects/" + id + "/attachments/" + aid, { method: "DELETE" }),
  attachmentBlob: (id, aid) => blob("/defects/" + id + "/attachments/" + aid + "/file"),

  // drawing register
  drawings: (q) => request("/drawings" + (q ? "?q=" + encodeURIComponent(q) : "")),
  drawingForPart: (part) => request("/drawings/by-part?partNumber=" + encodeURIComponent(part)),
  createDrawing: (data) => request("/drawings", { method: "POST", body: form(data) }),
  addRevision: (id, data) => request("/drawings/" + id + "/revisions", { method: "POST", body: form(data) }),
  drawingBlob: (id, rid) => blob("/drawings/" + id + "/revisions/" + rid + "/file"),

  // reports and sharing
  summary: () => request("/reports/summary"),
  share: (id) => request("/defects/" + id + "/share", post({})),
  emailReport: (id, data) => request("/defects/" + id + "/email", post(data)),
  publicReport: (token) => request("/public/reports/" + token),

  // admin
  users: () => request("/users"),
  updateUser: (id, data) => request("/users/" + id, patch(data)),
  unlockUser: (id) => request("/users/" + id + "/unlock", patch()),
  loginEvents: () => request("/users/login-events"),
};
