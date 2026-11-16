// Small helpers for showing and downloading files

export function formatSize(bytes) {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

// Images and PDFs open in a new tab. Other files (DWG, DXF) are downloaded.
export function openBlob(blob, name, type) {
  const url = URL.createObjectURL(blob);
  if (type.startsWith("image/") || type === "application/pdf") {
    window.open(url, "_blank");
  } else {
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    a.click();
  }
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

// Checks a chosen file in the browser before uploading (the server checks again)
export function checkFile(file, allowedExt, maxMb = 10) {
  if (!file) return "Please choose a file";
  const ext = file.name.includes(".") ? file.name.split(".").pop().toLowerCase() : "";
  if (!allowedExt.includes(ext)) return "File type not allowed. Allowed: " + allowedExt.join(", ");
  if (file.size > maxMb * 1024 * 1024) return "The file is too large. The maximum size is " + maxMb + " MB";
  return "";
}
