/** « 7h00 – 01h00 » : ouvert maintenant, à l'heure de Casablanca ? (horaires après minuit gérés ; null si illisible) */
export function isOpenNow(hours: string, now = new Date()): boolean | null {
  const m = hours.match(/(\d{1,2})h(\d{2})\D+(\d{1,2})h(\d{2})/);
  if (!m) return null;
  const parts = new Intl.DateTimeFormat('fr-FR', { timeZone: 'Africa/Casablanca', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(now);
  const t = Number(parts.find((p) => p.type === 'hour')!.value) * 60 + Number(parts.find((p) => p.type === 'minute')!.value);
  const open = Number(m[1]) * 60 + Number(m[2]);
  const close = Number(m[3]) * 60 + Number(m[4]);
  return close > open ? t >= open && t < close : t >= open || t < close;
}
