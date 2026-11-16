# What v4 adds, and the concepts each feature needs

Technology boundary kept: React, HTML, CSS, JavaScript, Bootstrap, MySQL, Java, Spring Boot and Spring features, Postman, Git/GitHub. Nothing new was added to `pom.xml` or `package.json`.

## 1. Photos and proof files on a defect
**What it does.** Upload PNG, JPG or PDF to a defect (max 10 files, 10 MB each). Thumbnails show on the defect screen. A "Show to customer" tick puts an image on the shared report. Files are locked once the defect is closed. Quality Head or Admin can remove a file.

**Concepts needed.**
- Multipart upload (`MultipartFile`, `@RequestParam`, `multipart/form-data`) in Spring Boot
- Saving files on disk and describing them in MySQL (`attachment` table)
- File safety: allowed extensions, checking the real file bytes ("magic number"), random UUID file names, size limits
- Authenticated download: the browser cannot send the token from an `<img>` tag, so React fetches a `Blob` and shows it with `URL.createObjectURL`
- Business rules reused: closed defects are locked, audit entries record file added and removed

## 2. Drawing register with revision control
**What it does.** One drawing per part number. Quality Head or Admin registers a drawing and uploads a new revision when it changes. Old revisions stay forever, with who, when and why. A defect shows the current drawing of its part.

**Concepts needed.**
- One-to-many with `@OneToMany` and `@OrderBy` (newest revision first)
- Versioning: never overwrite, never delete, add the next revision number
- Role rule with `@PreAuthorize` (everyone views, only Head or Admin changes)
- Uniqueness with a database constraint and a service check
- Correlated subquery in SQL (query 14) to find the latest revision

## 3. Customer report with photos
**What it does.** The shared report page shows images that were ticked "Show to customer". The link token also opens only those files.

**Concepts needed.** Token-based public access, a limited view (a separate `ReportView` object), checking that a file belongs to the defect and is marked as shared.

## 4. Performance and safety settings
- `server.compression.enabled=true` compresses JSON answers
- Upload size limits stop huge files
- Index on `attachment.defect_id`, unique index on `drawing.part_number`
- `MaxUploadSizeExceededException` becomes a clear message (HTTP 413)

## 5. Rules carried in from the calendar change
- Detected-on window (default 7 days) enforced in `DefectService`, setting `app.defect.max-backdate-days`
- Corrective action due date cannot be in the past (`@FutureOrPresent`)

## Test with Postman (login first, then Bearer token)
| Request | Expect |
|---|---|
| POST `/api/defects/{id}/attachments`, Body form-data: `file` (File), `shared` (Text: true) | 201 |
| Same with a `.exe` renamed to `.png` | 400 "content does not match" |
| Same with a file over 10 MB | 413 |
| GET `/api/defects/{id}/attachments` | list without `storedName` |
| POST `/api/drawings`, form-data: `partNumber`, `title`, `file` | 201, revision 1 |
| POST `/api/drawings/{id}/revisions`, form-data: `changeNote`, `file` | 201, revision 2 on top |
| POST `/api/drawings` as a Quality Engineer | 403 |
| GET `/api/drawings/by-part?partNumber=BRK-1042` | the drawing, or 204 if none |
