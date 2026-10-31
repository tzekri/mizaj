import { useEffect, useState, type FormEvent, type WheelEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, api, type ChronoSettings, type ChronoStats, type ChronoWinner } from '../api';

const STATUS: Record<ChronoWinner['status'], { label: string; css: string }> = {
  PENDING: { label: 'À remettre', css: 'badge--none' },
  REDEEMED: { label: 'Remis', css: 'badge--active' },
  EXPIRED: { label: 'Expiré', css: 'badge--expired' },
};

/** La molette de la souris ne doit jamais changer un chiffre par erreur en faisant défiler la page. */
const noWheel = (e: WheelEvent<HTMLInputElement>) => e.currentTarget.blur();

const when = (iso: string) =>
  new Date(iso).toLocaleString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Africa/Casablanca' });

/** Administration du défi « 10 secondes » : marche / pause, lot, validité, plafond de gagnants, chiffres et gagnants. */
export default function ChronoAdmin() {
  const [form, setForm] = useState<ChronoSettings | null>(null);
  /** Réglages tels qu'enregistrés sur le serveur : c'est eux que montre l'indicateur, pas le formulaire en cours. */
  const [current, setCurrent] = useState<ChronoSettings | null>(null);
  const [stats, setStats] = useState<ChronoStats | null>(null);
  const [winners, setWinners] = useState<ChronoWinner[] | null>(null);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.chronoSettings().then((s) => { setForm(s); setCurrent(s); }).catch((e) => setError((e as ApiError).message));
    api.chronoStats().then(setStats).catch(() => {});
    api.chronoWinners(30).then(setWinners).catch(() => setWinners([]));
  }, []);

  async function persist(next: ChronoSettings) {
    setError(null);
    setSaved(false);
    try {
      const s = await api.saveChronoSettings(next);
      setForm(s);
      setCurrent(s);
      setSaved(true);
    } catch (err) {
      setError((err as ApiError).message);
    }
  }

  function save(e: FormEvent) {
    e.preventDefault();
    if (form) persist(form);
  }

  const testing = current ? current.attemptsPerDay === 0 || current.attemptsPerDay > 10 : false;

  return (
    <>
      <h2>Défi 10 secondes</h2>
      <p className="dash-note">
        Le client arrête le chrono pile sur 10.00 s et gagne un bon à montrer en caisse.{' '}
        <Link to="/defi">Voir la page du jeu</Link> ·{' '}
        <a href="/defi/ecran" target="_blank" rel="noreferrer">Ouvrir l’écran géant (TV)</a> ·{' '}
        <Link to="/affiche/defi">Imprimer l’affiche du jeu</Link>
      </p>
      <p className="dash-note">
        Écran géant : ouvrez ce lien dans le navigateur de la TV (ou d’un boîtier relié à la TV), puis le bouton ⛶ pour le plein écran.
        Les clients scannent le QR code affiché : leur téléphone devient la manette et le chrono défile en grand sur la TV.
      </p>

      {current && (
        <div className={`game-state ${current.enabled ? 'game-state--on' : 'game-state--off'}`} role="status">
          <span className="game-state__dot" aria-hidden="true" />
          <div>
            <strong>{current.enabled ? 'Jeu ouvert aux clients' : 'Jeu en pause'}</strong>
            <span>
              {current.enabled
                ? `${current.attemptsPerDay === 0 ? 'Essais illimités' : `${current.attemptsPerDay} essai${current.attemptsPerDay > 1 ? 's' : ''} par jour et par téléphone`} · lot : ${current.prize}`
                : 'Les clients voient « Le défi est en pause pour le moment ».'}
            </span>
          </div>
          <button type="button" className={`btn ${current.enabled ? 'btn--outline' : 'btn--accent'}`} onClick={() => persist({ ...current, enabled: !current.enabled })}>
            {current.enabled ? 'Mettre en pause' : 'Ouvrir le jeu'}
          </button>
        </div>
      )}
      {current?.enabled && testing && (
        <p className="notice">
          ⚠️ Réglage de test : {current.attemptsPerDay === 0 ? 'essais illimités' : `${current.attemptsPerDay} essais par jour`}. Pensez à remettre 3 essais
          avant d’ouvrir le jeu aux clients.
        </p>
      )}

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
            <input type="number" min={1} max={90} value={form.validityDays} onWheel={noWheel} onChange={(e) => setForm({ ...form, validityDays: Number(e.target.value) })} />
          </label>
          <label>
            Essais par jour et par téléphone (0 = illimité, pour les tests)
            <input type="number" min={0} max={100} value={form.attemptsPerDay} onWheel={noWheel} onChange={(e) => setForm({ ...form, attemptsPerDay: Number(e.target.value) })} />
          </label>
          <label>
            Gagnants au maximum par jour
            <input type="number" min={1} max={1000} value={form.maxWinnersPerDay} onWheel={noWheel} onChange={(e) => setForm({ ...form, maxWinnersPerDay: Number(e.target.value) })} />
          </label>
          {error && <p className="form-error form__full">{error}</p>}
          {saved && <p className="notice notice--ok form__full">Réglages enregistrés ✓</p>}
          <div className="form__full pair-actions">
            <button className="btn btn--accent">Enregistrer</button>
          </div>
        </form>
      )}

      <h3 style={{ marginTop: 36 }}>Gagnants des 30 derniers jours</h3>
      {winners && winners.length === 0 && <p className="dash-note">Aucun gagnant pour le moment.</p>}
      {winners && winners.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Gagné le</th>
                <th>Temps</th>
                <th>Code</th>
                <th>Lot</th>
                <th>État</th>
                <th>Remis</th>
              </tr>
            </thead>
            <tbody>
              {winners.map((w) => (
                <tr key={w.code}>
                  <td>{when(w.createdAt)}</td>
                  <td>{(w.elapsedMs / 1000).toFixed(3)} s</td>
                  <td><code>{w.code.slice(0, 3)} {w.code.slice(3)}</code></td>
                  <td>{w.label}</td>
                  <td><span className={`badge ${STATUS[w.status].css}`}>{STATUS[w.status].label}</span></td>
                  <td>
                    {w.redeemedAt ? (
                      <>
                        {when(w.redeemedAt)}
                        <br />
                        <small>{[w.redeemedBy, w.shop].filter(Boolean).join(' · ')}</small>
                      </>
                    ) : (
                      <small>valable jusqu’au {new Date(w.expiresAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', timeZone: 'Africa/Casablanca' })}</small>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
