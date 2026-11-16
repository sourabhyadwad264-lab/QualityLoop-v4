import { useEffect, useState } from "react";
import { api } from "../api.js";
import { checkFile, formatSize, openBlob } from "../files.js";

const ALLOWED = ["png", "jpg", "jpeg", "pdf"];

// Photos and PDFs kept as proof for one defect
export default function Attachments({ defectId, closed, canManage }) {
  const [items, setItems] = useState([]);
  const [urls, setUrls] = useState({});
  const [file, setFile] = useState(null);
  const [shared, setShared] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = () => api.attachments(defectId).then(setItems).catch((e) => setError(e.message));
  useEffect(() => { load(); }, [defectId]);

  // Thumbnails: fetch each image with the login token and show it from a temporary browser address
  useEffect(() => {
    let alive = true;
    const made = [];
    (async () => {
      const next = {};
      for (const a of items) {
        if (!a.contentType.startsWith("image/")) continue;
        try {
          const url = URL.createObjectURL(await api.attachmentBlob(defectId, a.id));
          made.push(url);
          next[a.id] = url;
        } catch { /* a missing thumbnail is not worth an error box */ }
      }
      if (alive) setUrls(next);
    })();
    return () => { alive = false; made.forEach((u) => URL.revokeObjectURL(u)); };
  }, [items, defectId]);

  const upload = async () => {
    const problem = checkFile(file, ALLOWED);
    if (problem) { setError(problem); return; }
    try {
      setBusy(true);
      setError("");
      await api.uploadAttachment(defectId, file, shared);
      setFile(null);
      setShared(false);
      document.getElementById("attach-input").value = "";
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const open = async (a) => {
    try { openBlob(await api.attachmentBlob(defectId, a.id), a.originalName, a.contentType); }
    catch (e) { setError(e.message); }
  };

  const remove = async (a) => {
    if (!window.confirm("Remove " + a.originalName + "?")) return;
    try { await api.removeAttachment(defectId, a.id); await load(); }
    catch (e) { setError(e.message); }
  };

  return (
    <div className="mb-4">
      <h3 className="h6">Photos and proof files</h3>
      {error && <div className="alert alert-danger py-1">{error}</div>}
      {items.length === 0 && <p className="text-secondary">No files yet.</p>}
      <div className="thumbs mb-2">
        {items.map((a) => (
          <div className="thumb" key={a.id}>
            {urls[a.id] ? (
              <img src={urls[a.id]} alt={a.originalName} onClick={() => open(a)} />
            ) : (
              <button className="file-tile" onClick={() => open(a)}>{a.contentType === "application/pdf" ? "PDF" : "..."}</button>
            )}
            <div className="small text-truncate" title={a.originalName}>{a.originalName}</div>
            <div className="small text-secondary">{formatSize(a.sizeBytes)}{a.sharedWithCustomer ? ", customer can see" : ""}</div>
            {canManage && !closed && (
              <button className="btn btn-link btn-sm p-0 text-danger" onClick={() => remove(a)}>Remove</button>
            )}
          </div>
        ))}
      </div>
      {!closed && (
        <div className="d-flex flex-wrap align-items-center gap-2">
          <input id="attach-input" type="file" accept=".png,.jpg,.jpeg,.pdf" className="form-control form-control-sm w-auto"
            onChange={(e) => setFile(e.target.files[0] || null)} />
          <label className="form-check-label small d-flex align-items-center gap-1">
            <input type="checkbox" className="form-check-input m-0" checked={shared} onChange={(e) => setShared(e.target.checked)} />
            Show to customer
          </label>
          <button className="btn btn-sm btn-success" onClick={upload} disabled={busy}>{busy ? "Uploading..." : "Upload"}</button>
        </div>
      )}
      <div className="form-text">PNG, JPG or PDF, up to 10 MB, at most 10 files per defect.</div>
    </div>
  );
}
