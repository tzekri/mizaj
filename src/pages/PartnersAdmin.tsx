import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { ApiError, api, type Partner, type PartnerInput } from '../api';
import { SHOPS } from '../config';
import AdSlide from '../components/AdSlide';

const EMPTY: PartnerInput = { name: '', offer: '', details: '', shops: [], startsOn: null, endsOn: null, active: true };

/** Lien de l'écran géant d'un coffee shop : identifiant fixe (QR code stable) et boutique (pubs et comptage). */
export function tvLink(shop: string): string {
  const id = `${shop.normalize('NFD').replace(/[^A-Za-z0-9]/g, '').toUpperCase()}TV`.slice(0, 16);
  return `/defi/ecran?id=${id}&boutique=${encodeURIComponent(shop)}`;
}

/** Aujourd'hui au Maroc, au format AAAA-MM-JJ (comme les dates des partenaires). */
const today = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'Africa/Casablanca' });
const day = (iso: string) => new Date(`${iso}T12:00:00`).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });

function status(p: Partner): { label: string; css: string } {
  if (!p.active) return { label: 'En pause', css: 'badge--expired' };
  if (p.live) return { label: 'Diffusé', css: 'badge--active' };
  if (p.startsOn && p.startsOn > today()) return { label: `Programmé · dès le ${day(p.startsOn)}`, css: 'badge--none' };
  return { label: 'Terminé', css: 'badge--expired' };
}

const toInput = (p: Partner): PartnerInput => ({
  name: p.name,
  offer: p.offer,
  details: p.details ?? '',
  shops: p.shops,
  startsOn: p.startsOn ?? null,
  endsOn: p.endsOn ?? null,
  active: p.active,
});

/** Réduit la photo (1600 px de large au plus, JPEG) : légère pour la base de données et rapide à afficher sur la TV. */
function shrink(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const scale = Math.min(1, 1600 / img.width);
      const canvas = document.createElement('canvas');
      canvas.width = Math.round(img.width * scale);
      canvas.height = Math.round(img.height * scale);
      canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(img.src);
      resolve(canvas.toDataURL('image/jpeg', 0.85));
    };
    img.onerror = () => reject(new Error('Image illisible : choisissez une photo JPEG, PNG ou WebP.'));
    img.src = URL.createObjectURL(file);
  });
}

/** Onglet « Partenaires » : pubs des commerces partenaires diffusées sur l'écran géant entre deux parties du défi. */
export default function PartnersAdmin() {
  const [partners, setPartners] = useState<Partner[] | null>(null);
  const [editing, setEditing] = useState<{ id?: number; form: PartnerInput; preview?: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  const load = useCallback(() => api.partners().then(setPartners).catch((e) => setError((e as ApiError).message)), []);
  useEffect(() => {
    load();
  }, [load]);

  async function run(action: () => Promise<unknown>, done?: string) {
    setError(null);
    setSaved(null);
    try {
      await action();
      await load();
      if (done) setSaved(done);
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  return (
    <div className="partners-admin">
      <h2>Partenaires · pubs sur l’écran géant</h2>
      <p className="dash-note">
        Quand personne ne joue, la TV du défi alterne : 20 secondes de jeu, puis 10 secondes de pub d’un partenaire, chacun son tour.
        Le QR code « Scannez pour jouer » reste toujours affiché. Chaque affichage est compté, pour montrer les chiffres à vos partenaires.
      </p>
      <p className="dash-note">
        Écran de chaque coffee shop (à ouvrir sur la TV, puis ⛶ pour le plein écran) :{' '}
        {SHOPS.map((s, k) => (
          <span key={s.name}>
            {k > 0 && ' · '}
            <a href={tvLink(s.name)} target="_blank" rel="noreferrer">{s.name}</a>
          </span>
        ))}
      </p>
      {error && !editing && <p className="form-error">{error}</p>}
      {saved && <p className="notice notice--ok">{saved}</p>}

      {partners && partners.length === 0 && !editing && (
        <p className="dash-note">Aucun partenaire pour le moment : ajoutez le premier ci-dessous.</p>
      )}
      <div className="partner-list">
        {partners?.map((p, k) => {
          const s = status(p);
          return (
            <article key={p.id} className={`partner ${p.live ? '' : 'muted'}`}>
              <div className="partner__thumb">{p.image ? <img src={p.image} alt="" /> : <span>{p.name.slice(0, 1)}</span>}</div>
              <div className="partner__body">
                <h3>
                  {p.name} <span className={`badge ${s.css}`}>{s.label}</span>
                </h3>
                <p className="partner__offer">{p.offer}</p>
                <small>
                  {p.shops.length ? p.shops.join(', ') : 'Tous les coffee shops'}
                  {p.startsOn || p.endsOn
                    ? ` · ${p.startsOn ? `du ${day(p.startsOn)}` : ''}${p.endsOn ? ` au ${day(p.endsOn)}` : ' sans fin'}`
                    : ''}
                </small>
              </div>
              <div className="partner__stats">
                <strong>{p.shownToday}</strong> aujourd’hui
                <br />
                <strong>{p.shown30}</strong> sur 30 jours
                <br />
                <small>{p.shownTotal} au total</small>
              </div>
              <div className="partner__actions">
                <button className="icon-btn" disabled={k === 0} onClick={() => run(() => api.movePartner(p.id, 'up'))} aria-label={`Passer ${p.name} avant`}>↑</button>
                <button className="icon-btn" disabled={k === partners.length - 1} onClick={() => run(() => api.movePartner(p.id, 'down'))} aria-label={`Passer ${p.name} après`}>↓</button>
                <button className="link" onClick={() => run(() => api.updatePartner(p.id, { ...toInput(p), active: !p.active }), p.active ? `${p.name} : pub en pause` : `${p.name} : pub réactivée ✓`)}>
                  {p.active ? 'Mettre en pause' : 'Réactiver'}
                </button>
                <button className="link" onClick={() => { setSaved(null); setError(null); setEditing({ id: p.id, form: toInput(p), preview: p.image }); }}>Modifier</button>
                <button
                  className="link link--danger"
                  onClick={() => window.confirm(`Supprimer ${p.name} et ses chiffres d’affichage ?`) && run(() => api.deletePartner(p.id), `${p.name} supprimé`)}
                >
                  Supprimer
                </button>
              </div>
              {editing?.id === p.id && (
                <PartnerForm editing={editing} setEditing={setEditing} error={error} onSaved={(msg) => { setEditing(null); load(); setSaved(msg); }} setError={setError} />
              )}
            </article>
          );
        })}
      </div>

      {editing && !editing.id ? (
        <PartnerForm editing={editing} setEditing={setEditing} error={error} onSaved={(msg) => { setEditing(null); load(); setSaved(msg); }} setError={setError} />
      ) : (
        <p>
          <button className="btn btn--outline btn--sm" onClick={() => { setSaved(null); setError(null); setEditing({ form: EMPTY }); }}>+ Nouveau partenaire</button>
        </p>
      )}
    </div>
  );
}

type Editing = { id?: number; form: PartnerInput; preview?: string };

function PartnerForm({ editing, setEditing, error, setError, onSaved }: {
  editing: Editing;
  setEditing: (e: Editing | null) => void;
  error: string | null;
  setError: (e: string | null) => void;
  onSaved: (message: string) => void;
}) {
  const { id, form: f, preview } = editing;
  const [busy, setBusy] = useState(false);
  const set = (patch: Partial<PartnerInput>) => setEditing({ ...editing, form: { ...f, ...patch } });

  async function pick(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const data = await shrink(file);
      setEditing({ ...editing, form: { ...f, image: data }, preview: data });
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    const body = { ...f, startsOn: f.startsOn || null, endsOn: f.endsOn || null };
    try {
      if (id) await api.updatePartner(id, body);
      else await api.createPartner(body);
      onSaved(`${f.name} enregistré ✓`);
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setBusy(false);
    }
  }

  const toggleShop = (shop: string) => set({ shops: f.shops.includes(shop) ? f.shops.filter((s) => s !== shop) : [...f.shops, shop] });

  return (
    <form className="form new-staff partner-form" onSubmit={submit}>
      <h3 className="form__full">{id ? `Modifier « ${f.name} »` : 'Nouveau partenaire'}</h3>
      <label>
        Nom du partenaire
        <input value={f.name} required maxLength={60} placeholder="Sushi Lissasfa" onChange={(e) => set({ name: e.target.value })} />
      </label>
      <label>
        Offre (affichée en grand)
        <input value={f.offer} required maxLength={120} placeholder="-10 % sur présentation de votre carte Kidima" onChange={(e) => set({ offer: e.target.value })} />
      </label>
      <label className="form__full">
        Ligne en plus (adresse, conditions… facultatif)
        <input value={f.details ?? ''} maxLength={200} placeholder="À 2 minutes, bd Lissasfa · ouvert jusqu’à minuit" onChange={(e) => set({ details: e.target.value })} />
      </label>
      <fieldset className="form__full partner-form__shops">
        <legend>Coffee shops (aucune case cochée : tous)</legend>
        {SHOPS.map((s) => (
          <label key={s.name} className="check">
            <input type="checkbox" checked={f.shops.includes(s.name)} onChange={() => toggleShop(s.name)} /> {s.name}
          </label>
        ))}
      </fieldset>
      <label>
        Début de diffusion (facultatif)
        <input type="date" value={f.startsOn ?? ''} onChange={(e) => set({ startsOn: e.target.value || null })} />
      </label>
      <label>
        Fin de diffusion (facultatif)
        <input type="date" value={f.endsOn ?? ''} min={f.startsOn ?? undefined} onChange={(e) => set({ endsOn: e.target.value || null })} />
      </label>
      <div className="form__full partner-form__image">
        <span>Photo ou visuel (format paysage conseillé)</span>
        <div className="pair-actions">
          <label className="btn btn--outline btn--sm">
            {preview ? 'Changer l’image' : 'Choisir une image'}
            <input type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={pick} />
          </label>
          {preview && (
            <button type="button" className="link link--danger" onClick={() => setEditing({ ...editing, form: { ...f, image: '' }, preview: undefined })}>
              Retirer l’image
            </button>
          )}
        </div>
      </div>
      <label className="check form__full">
        <input type="checkbox" checked={f.active} onChange={(e) => set({ active: e.target.checked })} /> Pub active
      </label>
      <div className="form__full">
        <span className="partner-form__label">Aperçu sur la TV</span>
        <div className="ad-preview">
          <AdSlide ad={{ name: f.name, offer: f.offer, details: f.details, image: preview }} />
        </div>
      </div>
      {error && <p className="form-error form__full">{error}</p>}
      <div className="form__full pair-actions">
        <button className="btn btn--accent" disabled={busy}>{busy ? 'Enregistrement…' : 'Enregistrer'}</button>
        <button type="button" className="btn btn--outline" onClick={() => { setError(null); setEditing(null); }}>Annuler</button>
      </div>
    </form>
  );
}
