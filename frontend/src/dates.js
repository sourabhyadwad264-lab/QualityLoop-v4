// How many days back "Detected on" may go. Keep it equal to
// app.defect.max-backdate-days in application.properties
export const MAX_BACKDATE_DAYS = 7;

const toLocalIso = (d) => {
  const x = new Date(d);
  x.setMinutes(x.getMinutes() - x.getTimezoneOffset());
  return x.toISOString().slice(0, 10);
};

// Today's date in the user's own timezone, as YYYY-MM-DD
export const todayLocal = () => toLocalIso(new Date());

// The date n days ago
export const daysAgoLocal = (n) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return toLocalIso(d);
};
