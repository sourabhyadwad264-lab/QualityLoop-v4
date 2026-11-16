import { useEffect, useState } from "react";
import { api } from "../api.js";
import { checkFile, formatSize, openBlob } from "../files.js";

const ALLOWED = ["pdf", "png", "jpg", "jpeg", "dwg", "dxf"];
const ACCEPT = ".pdf,.png,.jpg,.jpeg,.dwg,.dxf";

export default function Drawings({ canManage }) {
  const [list, setList] = useState([]);
  const [q, setQ] = useState("");
  const [error, setError] = useState("");
  const [form, setForm] = useState({ partNumber: "", title: "", changeNote: "", file: null });
  const [formKey, setFormKey] = useState(0);

  const load = () => api.drawings(q).then(setList).catch((e) => setError(e.message));
  useEffect(() => { load(); }, [q]);

  const create = async () => {
    const problem = checkFile(form.file, ALLOWED);
    if (!form.partNumber.trim() || !form.title.trim()) { setError("Part number and title are required"); return; }
    if (problem) { setError(problem); return; }
    try {
      setError("");
      await api.createDrawing(form);
      setForm({ partNumber: "", title: "", changeNote: "", file: null });
      setFormKey(formKey + 1); // clears the file box
      await load();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <>
      {canManage && (
        <div className="panel mb-3">
          <h2 className="h6">Register a new drawing</h2>
          <div className="row g-2" key={formKey}>
            <div className="col-md-3"><input className="form-control form-control-sm" placeholder="Part number" value={form.partNumber} onChange={(e) => setForm({ ...form, partNumber: e.target.value })} /></div>
            <div className="col-md-4"><input className="form-control form-control-sm" placeholder="Drawing title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
            <div className="col-md-5"><input className="form-control form-control-sm" placeholder="Note (optional)" value={form.changeNote} onChange={(e) => setForm({ ...form, changeNote: e.target.value })} /></div>
            <div className="col-md-9"><input type="file" accept={ACCEPT} className="form-control form-control-sm" onChange={(e) => setForm({ ...form, file: e.target.files[0] || null })} /></div>
            <div className="col-md-3"><button className="btn btn-sm btn-success w-100" onClick={create}>Register drawing</button></div>
          </div>
          <div className="form-text">PDF, PNG, JPG, DWG or DXF, up to 10 MB. One drawing per part number. Later changes become new revisions.</div>
        </div>
      )}

      {error && <div className="alert alert-danger">{error}</div>}
      <input className="form-control form-control-sm mb-3" placeholder="Search by part number or title" value={q} onChange={(e) => setQ(e.target.value)} />
      {list.length === 0 && <div className="panel text-secondary">No drawings found.</div>}
      {list.map((d) => <DrawingCard key={d.id} drawing={d} canManage={canManage} onChanged={load} onError={setError} />)}
    </>
  );
}

function DrawingCard({ drawing, canManage, onChanged, onError }) {
  const [open, setOpen] = useState(false);
  const [note, setNote] = useState("");
  const [file, setFile] = useState(null);
  const [key, setKey] = useState(0);
  const current = drawing.revisions[0];

  const view = async (r) => {
    try { openBlob(await api.drawingBlob(drawing.id, r.id), r.originalName, r.contentType); }
    catch (e) { onError(e.message); }
  };

  const upload = async () => {
    const problem = checkFile(file, ALLOWED);
    if (problem) { onError(problem); return; }
    try {
      onError("");
      await api.addRevision(drawing.id, { changeNote: note, file });
      setNote(""); setFile(null); setKey(key + 1);
      await onChanged();
    } catch (e) {
      onError(e.message);
    }
  };

  return (
    <div className="panel mb-3">
      <div className="d-flex justify-content-between align-items-start flex-wrap gap-2">
        <div>
          <h2 className="h6 m-0">{drawing.partNumber} <span className="text-secondary fw-normal">{drawing.title}</span></h2>
          <small className="text-secondary">
            Current: Rev {current.revisionNumber}, by {current.uploadedBy}, {current.uploadedAt.slice(0, 10)}, {formatSize(current.sizeBytes)}
          </small>
        </div>
        <div className="d-flex gap-2">
          <button className="btn btn-sm btn-success" onClick={() => view(current)}>Open current</button>
          <button className="btn btn-sm btn-outline-secondary" onClick={() => setOpen(!open)}>
            {open ? "Hide history" : "History (" + drawing.revisions.length + ")"}
          </button>
        </div>
      </div>

      {open && (
        <div className="table-responsive mt-3">
          <table className="table table-sm m-0">
            <thead><tr><th>Rev</th><th>Change note</th><th>By</th><th>Date</th><th></th></tr></thead>
            <tbody>
              {drawing.revisions.map((r) => (
                <tr key={r.id}>
                  <td>{r.revisionNumber}{r.id === current.id ? " (current)" : ""}</td>
                  <td>{r.changeNote || "-"}</td>
                  <td>{r.uploadedBy}</td>
                  <td>{r.uploadedAt.slice(0, 10)}</td>
                  <td><button className="btn btn-sm btn-outline-secondary" onClick={() => view(r)}>Open</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {canManage && (
        <div className="row g-2 mt-2" key={key}>
          <div className="col-md-4"><input className="form-control form-control-sm" placeholder="What changed?" value={note} onChange={(e) => setNote(e.target.value)} /></div>
          <div className="col-md-5"><input type="file" accept={ACCEPT} className="form-control form-control-sm" onChange={(e) => setFile(e.target.files[0] || null)} /></div>
          <div className="col-md-3"><button className="btn btn-sm btn-outline-success w-100" onClick={upload}>Upload new revision</button></div>
        </div>
      )}
    </div>
  );
}
