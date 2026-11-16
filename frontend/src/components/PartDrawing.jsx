import { useEffect, useState } from "react";
import { api } from "../api.js";
import { openBlob } from "../files.js";

// Shows the current drawing of the defect's part, if the drawing register has one
export default function PartDrawing({ partNumber }) {
  const [drawing, setDrawing] = useState(undefined); // undefined = loading, null = none
  const [error, setError] = useState("");

  useEffect(() => {
    if (!partNumber) { setDrawing(null); return; }
    api.drawingForPart(partNumber).then(setDrawing).catch(() => setDrawing(null));
  }, [partNumber]);

  if (drawing === undefined) return null;
  if (drawing === null) return <p className="text-secondary small">No drawing registered for this part yet.</p>;

  const current = drawing.revisions[0];
  const open = async () => {
    try { openBlob(await api.drawingBlob(drawing.id, current.id), current.originalName, current.contentType); }
    catch (e) { setError(e.message); }
  };

  return (
    <div className="mb-3">
      <div className="d-flex align-items-center flex-wrap gap-2">
        <span className="badge-soft badge-good">Drawing Rev {current.revisionNumber}</span>
        <span>{drawing.title}</span>
        <button className="btn btn-sm btn-outline-secondary" onClick={open}>Open current drawing</button>
      </div>
      {error && <div className="text-danger small">{error}</div>}
    </div>
  );
}
