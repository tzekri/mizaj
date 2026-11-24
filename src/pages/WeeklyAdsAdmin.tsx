import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { ApiError, api, type WeeklyAd, type WeeklyAdInput } from '../api';
import { shrink } from '../images';

const STATUS: Record<WeeklyAd['status'], { label: string; css: string }> = {
  CURRENT: { label: 'Affichée cette semaine', css: 'badge--active' },
  OVERLAP: { label: 'En cours, mais une plus récente passe avant', css: 'badge--none' },
  SCHEDULED: { label: 'Programmée', css: 'badge--none' },
  ENDED: { label: 'Terminée', css: 'badge--expired' },
  PAUSED: { label: 'En pause', css: 'badge--expired' },
  NO_IMAGE: { label: 'Sans affiche', css: 'badge--expired' },
};

/** Date au format AAAA-MM-JJ, heure du Maroc. */
const iso = (d: Date) => d.toLocaleDateString('sv-SE', { timeZone: 'Africa/Casablanca' });
const day = (s: string) => new Date(`${s}T12:00:00`).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' });

/** Semaine qui suit la dernière programmée (lundi → dimanche) ; à défaut, la semaine en cours. */
function nextWeek(ads: WeeklyAd[]): { startsOn: string; endsOn: string } {
  const today = new Date(`${iso(new Date())}T12:00:00`);
  const monday = new Date(today);
  monday.setDate(today.getDate() - ((today.getDay() + 6) % 7));
  const last = ads.map((a) => a.endsOn).sort().pop();
  const start = last && new Date(`${last}T12:00:00`) >= monday ? new Date(`${last}T12:00:00`) : null;
  if (start) start.setDate(start.getDate() + 1);
  const from = start ?? monday;
  const to = new Date(from);
  to.setDate(from.getDate() + 6);
  return { startsOn: iso(from), endsOn: iso(to) };
}

type Editing = { id?: number; form: WeeklyAdInput; preview?: string };

/** Onglet « Pub de la semaine » : une affiche par semaine, affichée sur la page /promo. */
export default function WeeklyAdsAdmin() {
  const [ads, setAds] = useState<WeeklyAd[] | null>(null);
  const [editing, setEditing] = useState<Editing | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const load = useCallback(() => api.weeklyAds().then(setAds).catch((e) => setError((e as ApiError).message)), []);
  useEffect(() => {
    load();
  }, [load]);

  function startNew() {
    setError(null);
    setSaved(null);
    setEditing({ form: { title: '', description: '', active: true, ...nextWeek(ads ?? []) } });
  }

  async function remove(a: WeeklyAd) {
    if (!window.confirm(`Supprimer la pub « ${a.title} » ?`)) return;
    try {
      await api.deleteWeeklyAd(a.id);
      setSaved(`« ${a.title} » supprimée`);
      load();
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  return (
    <div className="weekly-admin">
      <h2>Pub de la semaine</h2>
      <p className="dash-note">
        Une affiche par semaine pour mettre un produit en avant. Elle s’affiche en plein écran sur{' '}
        <a href="/promo" target="_blank" rel="noreferrer">la page /promo</a> (TV du coffee shop, lien à partager sur WhatsApp ou Instagram).
        Programmez les semaines à l’avance : la page passe toute seule à la suivante.
      </p>
      {error && !editing && <p className="form-error">{error}</p>}
      {saved && <p className="notice notice--ok">{saved}</p>}

      {editing && !editing.id && <WeeklyForm editing={editing} setEditing={setEditing} onSaved={(m) => { setEditing(null); setSaved(m); load(); }} />}
      {!editing && (
        <p><button className="btn btn--accent btn--sm" onClick={startNew}>+ Nouvelle pub</button></p>
      )}

      {ads && ads.length === 0 && !editing && <p className="dash-note">Aucune pub pour le moment.</p>}
      <div className="weekly-admin__list">
        {ads?.map((a) =>
          editing?.id === a.id ? (
            <div key={a.id} style={{ gridColumn: '1 / -1' }}>
              <WeeklyForm editing={editing} setEditing={setEditing} onSaved={(m) => { setEditing(null); setSaved(m); load(); }} />
            </div>
          ) : (
            <article key={a.id} className={`weekly-ad ${a.status === 'CURRENT' ? 'weekly-ad--current' : ''}`}>
              <div className="weekly-ad__img">{a.image ? <img src={a.image} alt={a.title} /> : <span>Pas d’affiche</span>}</div>
              <div className="weekly-ad__body">
                <h3>{a.title}</h3>
                <small>du {day(a.startsOn)} au {day(a.endsOn)}</small>
                <span><span className={`badge ${STATUS[a.status].css}`}>{STATUS[a.status].label}</span></span>
                <div className="weekly-ad__actions">
                  <button className="link" onClick={() => { setSaved(null); setError(null); setEditing({ id: a.id, form: { title: a.title, description: a.description ?? '', startsOn: a.startsOn, endsOn: a.endsOn, active: a.active }, preview: a.image }); }}>
                    Modifier
                  </button>
                  <button className="link link--danger" onClick={() => remove(a)}>Supprimer</button>
                </div>
              </div>
            </article>
          ),
        )}
      </div>
    </div>
  );
}

function WeeklyForm({ editing, setEditing, onSaved }: { editing: Editing; setEditing: (e: Editing | null) => void; onSaved: (message: string) => void }) {
  const { id, form: f, preview } = editing;
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const set = (patch: Partial<WeeklyAdInput>) => setEditing({ ...editing, form: { ...f, ...patch } });

  async function pick(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const data = await shrink(file, 1800);
      setEditing({ ...editing, form: { ...f, image: data }, preview: data });
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (id) await api.updateWeeklyAd(id, f);
      else await api.createWeeklyAd(f);
      onSaved(`« ${f.title} » enregistrée ✓`);
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="form new-staff weekly-form" onSubmit={submit}>
      <h3 className="form__full">{id ? `Modifier « ${f.title} »` : 'Nouvelle pub de la semaine'}</h3>
      <label>
        Produit mis en avant
        <input value={f.title} required maxLength={60} placeholder="Affogato" onChange={(e) => set({ title: e.target.value })} />
      </label>
      <label>
        Accroche (facultatif, sous l’affiche)
        <input value={f.description ?? ''} maxLength={160} placeholder="Nouveau : glace vanille et espresso chaud" onChange={(e) => set({ description: e.target.value })} />
      </label>
      <label>
        Du
        <input type="date" required value={f.startsOn} onChange={(e) => set({ startsOn: e.target.value })} />
      </label>
      <label>
        Au
        <input type="date" required min={f.startsOn} value={f.endsOn} onChange={(e) => set({ endsOn: e.target.value })} />
      </label>
      <div className="form__full">
        <span className="partner-form__label">Affiche (image verticale conseillée, comme un poster)</span>
        <div className="pair-actions">
          <label className="btn btn--outline btn--sm">
            {preview ? 'Changer l’affiche' : 'Choisir l’affiche'}
            <input type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={pick} />
          </label>
        </div>
        {preview && <img className="weekly-form__preview" src={preview} alt="Aperçu de l’affiche" style={{ marginTop: 12 }} />}
      </div>
      <label className="check form__full">
        <input type="checkbox" checked={f.active} onChange={(e) => set({ active: e.target.checked })} /> Pub active
      </label>
      {error && <p className="form-error form__full">{error}</p>}
      <div className="form__full pair-actions">
        <button className="btn btn--accent" disabled={busy || (!id && !f.image)}>{busy ? 'Enregistrement…' : 'Enregistrer'}</button>
        <button type="button" className="btn btn--outline" onClick={() => setEditing(null)}>Annuler</button>
      </div>
    </form>
  );
}
