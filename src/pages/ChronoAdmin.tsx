import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, api, type ChronoSettings, type ChronoStats } from '../api';

/** Administration du défi « 10 secondes » : marche / pause, lot, validité, plafond de gagnants et chiffres. */
export default function ChronoAdmin() {
  const [form, setForm] = useState<ChronoSettings | null>(null);
  const [stats, setStats] = useState<ChronoStats | null>(null);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.chronoSettings().then(setForm).catch((e) => setError((e as ApiError).message));
    api.chronoStats().then(setStats).catch(() => {});
  }, []);

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    setError(null);
    setSaved(false);
    try {
      setForm(await api.saveChronoSettings(form));
      setSaved(true);
    } catch (err) {
      setError((err as ApiError).message);
    }
  }

  return (
    <>
      <h2>Défi 10 secondes</h2>
      <p className="dash-note">
        Le client arrête le chrono pile sur 10.00 s et gagne un bon à montrer en caisse (3 essais par jour et par téléphone).{' '}
        <Link to="/defi">Voir la page du jeu</Link> ·{' '}
        <a href="/defi/ecran" target="_blank" rel="noreferrer">Ouvrir l’écran géant (TV)</a>
      </p>
      <p className="dash-note">
        Écran géant : ouvrez ce lien dans le navigateur de la TV (ou d’un boîtier relié à la TV), puis le bouton ⛶ pour le plein écran.
        Les clients scannent le QR code affiché : leur téléphone devient la manette et le chrono défile en grand sur la TV.
      </p>
      {stats && (
        <div className="kpis kpis--3">
          <div className="kpi"><strong>{stats.attemptsToday}</strong><span>essais aujourd’hui ({stats.attempts30} sur 30 jours)</span></div>
          <div className="kpi"><strong>{stats.winnersToday}</strong><span>gagnants aujourd’hui ({stats.winners30} sur 30 jours)</span></div>
          <div className="kpi"><strong>{stats.redeemedToday}</strong><span>bons remis aujourd’hui ({stats.redeemed30} sur 30 jours)</span></div>
        </div>
      )}
      {form && (
        <form className="form new-staff" onSubmit={save}>
          <label className="check form__full">
            <input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
            Jeu ouvert (décocher pour le mettre en pause)
          </label>
          <label className="form__full">
            Lot à gagner
            <input value={form.prize} required maxLength={80} placeholder="Un café offert" onChange={(e) => setForm({ ...form, prize: e.target.value })} />
          </label>
          <label>
            Validité du bon (jours)
            <input type="number" min={1} max={90} value={form.validityDays} onChange={(e) => setForm({ ...form, validityDays: Number(e.target.value) })} />
          </label>
          <label>
            Gagnants au maximum par jour
            <input type="number" min={1} max={1000} value={form.maxWinnersPerDay} onChange={(e) => setForm({ ...form, maxWinnersPerDay: Number(e.target.value) })} />
          </label>
          {error && <p className="form-error form__full">{error}</p>}
          {saved && <p className="notice notice--ok form__full">Réglages enregistrés ✓</p>}
          <div className="form__full pair-actions">
            <button className="btn btn--accent">Enregistrer</button>
          </div>
        </form>
      )}
    </>
  );
}
