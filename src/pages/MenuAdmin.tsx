import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  ApiError,
  api,
  type JuiceFormat,
  type MenuCategory,
  type MenuCategoryInput,
  type MenuItem,
  type MenuItemInput,
  type MenuTone,
} from '../api';
import { invalidateMenu, menuPhoto } from '../menu';

const TONE_LABEL: Record<MenuTone, string> = { CREAM: 'Crème', ACCENT: 'Bleu', DARK: 'Sombre' };
const EMPTY_CATEGORY: MenuCategoryInput = { light: '', bold: '', arabic: '', tone: 'CREAM', photo: '', visible: true };
const emptyItem = (categoryId: number): MenuItemInput => ({
  categoryId,
  name: '',
  description: '',
  price: 20,
  volume: '',
  photo: '',
  featured: false,
  tag: '',
  available: true,
});

/** Onglet « Menu » de l'administration : prix des jus composés, produits et catégories du menu. */
export default function MenuAdmin() {
  return (
    <div className="menu-admin">
      <JuiceFormats />
      <MenuEditor />
    </div>
  );
}

/** Prix des jus composés : prix fixe par format, ou prix selon les ingrédients (prix de départ + suppléments × coefficient). */
function JuiceFormats() {
  const [formats, setFormats] = useState<JuiceFormat[]>([]);
  const [fixed, setFixed] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);

  useEffect(() => {
    api.juiceFormats().then(setFormats).catch((e) => setError((e as ApiError).message));
    api.juicePricing().then((p) => setFixed(p.fixed)).catch((e) => setError((e as ApiError).message));
  }, []);

  async function switchMode(value: boolean) {
    setError(null);
    try {
      setFixed((await api.setJuicePricing(value)).fixed);
      setSaved(value ? 'Prix fixe activé ✓' : 'Prix selon les ingrédients activé ✓');
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  function change(code: JuiceFormat['code'], patch: Partial<JuiceFormat>) {
    setSaved(null);
    setFormats(formats.map((f) => (f.code === code ? { ...f, ...patch } : f)));
  }

  async function save(f: JuiceFormat) {
    setError(null);
    try {
      setFormats(await api.saveJuiceFormat(f.code, { basePrice: f.basePrice, factor: f.factor }));
      setSaved(`${f.label} enregistré ✓`);
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  return (
    <>
      <h2>Prix des jus composés</h2>
      <div className="seg seg--wrap" role="group" aria-label="Mode de prix des jus">
        <button className={fixed === true ? 'active' : ''} onClick={() => switchMode(true)}>Prix fixe par format</button>
        <button className={fixed === false ? 'active' : ''} onClick={() => switchMode(false)}>Prix selon les ingrédients</button>
      </div>
      <p className="dash-note">
        {fixed === false
          ? 'Prix d’un jus = prix du format + suppléments des ingrédients × coefficient. Les suppléments se règlent dans l’onglet Administration, section « Jus composés ».'
          : 'Le jus coûte le prix du format, quels que soient les fruits choisis. Les suppléments restent enregistrés pour repasser plus tard au prix selon les ingrédients.'}
      </p>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Format</th><th>{fixed === false ? 'Prix de départ (DH)' : 'Prix du jus (DH)'}</th>
              {fixed === false && <><th>Coefficient des suppléments</th><th>Exemple : orange + fraise (+5 DH)</th></>}
              <th />
            </tr>
          </thead>
          <tbody>
            {formats.map((f) => (
              <tr key={f.code}>
                <td><b>{f.label}</b></td>
                <td>
                  <input className="cell-input" type="number" min={0} max={500} value={f.basePrice}
                    onChange={(e) => change(f.code, { basePrice: Number(e.target.value) })} aria-label={`Prix de départ ${f.label}`} />
                </td>
                {fixed === false && (
                  <>
                    <td>
                      <input className="cell-input" type="number" min={0.5} max={5} step={0.1} value={f.factor}
                        onChange={(e) => change(f.code, { factor: Number(e.target.value) })} aria-label={`Coefficient ${f.label}`} />
                    </td>
                    <td>{f.basePrice + Math.round(5 * f.factor)} DH</td>
                  </>
                )}
                <td><button className="btn btn--accent btn--sm" onClick={() => save(f)}>Enregistrer</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {saved && <p className="notice notice--ok">{saved}</p>}
      {error && <p className="form-error">{error}</p>}
    </>
  );
}

type Editing = { kind: 'item'; id?: number; form: MenuItemInput } | { kind: 'category'; id?: number; form: MenuCategoryInput } | null;

function MenuEditor() {
  const [menu, setMenu] = useState<MenuCategory[]>([]);
  const [editing, setEditing] = useState<Editing>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api.adminMenu().then(setMenu).catch((e) => setError((e as ApiError).message));
  }, []);
  useEffect(load, [load]);

  /** Toute modification : le site public rechargera le menu, puis la liste est rafraîchie. */
  async function run(action: () => Promise<unknown>) {
    setError(null);
    try {
      await action();
      invalidateMenu();
      load();
      return true;
    } catch (e) {
      setError((e as ApiError).message);
      return false;
    }
  }

  const itemInput = (i: MenuItem): MenuItemInput => ({ ...i, description: i.description ?? '', volume: i.volume ?? '', tag: i.tag ?? '' });

  function savePrice(i: MenuItem, price: number) {
    if (Number.isFinite(price) && price >= 0 && price !== i.price) run(() => api.saveMenuItem({ ...itemInput(i), price }, i.id));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!editing) return;
    const ok =
      editing.kind === 'item'
        ? await run(() => api.saveMenuItem({ ...editing.form, intensity: editing.form.intensity || undefined }, editing.id))
        : await run(() => api.saveMenuCategory(editing.form, editing.id));
    if (ok) setEditing(null);
  }

  function remove(label: string, action: () => Promise<unknown>) {
    if (window.confirm(`Supprimer « ${label} » du menu ? Cette action est définitive.`)) run(action);
  }

  const form = editing && (
    <form className="form new-staff menu-admin__form" onSubmit={submit}>
      {editing.kind === 'item' ? (
        <ItemFields value={editing.form} categories={menu} isNew={!editing.id} onChange={(form) => setEditing({ ...editing, form })} />
      ) : (
        <CategoryFields value={editing.form} isNew={!editing.id} onChange={(form) => setEditing({ ...editing, form })} />
      )}
      {error && <p className="form-error form__full">{error}</p>}
      <div className="form__full pair-actions">
        <button className="btn btn--accent">Enregistrer</button>
        <button type="button" className="btn btn--outline" onClick={() => setEditing(null)}>Annuler</button>
      </div>
    </form>
  );

  return (
    <>
      <h2>Menu du coffee shop</h2>
      <p className="dash-note">
        Modifiez un prix directement dans le tableau (validé en quittant la case ou avec Entrée). Les changements sont visibles tout de suite sur le site.
      </p>
      {error && !editing && <p className="form-error">{error}</p>}
      {menu.map((c, ci) => (
        <section key={c.id} className={`menu-admin__cat ${c.visible ? '' : 'muted'}`}>
          <div className="menu-admin__head">
            <img className="thumb thumb--sq" src={menuPhoto(c.photo, 120)} alt="" style={{ width: 48, height: 48 }} />
            <div>
              <h3>{c.light} {c.bold}{!c.visible && <small> · masquée</small>}</h3>
              <small>{c.items.length} produit{c.items.length > 1 ? 's' : ''} · /menu/{c.slug}</small>
            </div>
            <div className="menu-admin__actions">
              <button className="icon-btn" disabled={ci === 0} onClick={() => run(() => api.moveMenuCategory(c.id, 'up'))} aria-label="Monter la catégorie">↑</button>
              <button className="icon-btn" disabled={ci === menu.length - 1} onClick={() => run(() => api.moveMenuCategory(c.id, 'down'))} aria-label="Descendre la catégorie">↓</button>
              <button className="link" onClick={() => setEditing({ kind: 'category', id: c.id, form: { light: c.light, bold: c.bold, arabic: c.arabic ?? '', tone: c.tone, photo: c.photo, visible: c.visible } })}>Modifier</button>
              {c.items.length === 0 && <button className="link link--danger" onClick={() => remove(`${c.light} ${c.bold}`, () => api.deleteMenuCategory(c.id))}>Supprimer</button>}
            </div>
          </div>
          {editing?.kind === 'category' && editing.id === c.id && form}
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Produit</th><th className="num">Prix (DH)</th><th>Disponible</th><th>Ordre</th><th /></tr>
              </thead>
              <tbody>
                {c.items.map((i, ii) => (
                  <tr key={i.id} className={i.available ? '' : 'muted'}>
                    <td className="ing-cell">
                      <img className="thumb thumb--sq" src={menuPhoto(i.photo, 120)} alt="" style={{ width: 36, height: 36 }} loading="lazy" />
                      <span>
                        {i.name}
                        {i.featured && <span className="badge badge--star" title="Affiché dans « Produits populaires » sur l’accueil">★ Accueil</span>}
                        {i.tag && <span className="badge badge--soft">{i.tag}</span>}
                      </span>
                    </td>
                    <td className="num">
                      <input key={i.price} className="cell-input" type="number" min={0} max={5000} defaultValue={i.price} aria-label={`Prix de ${i.name}`}
                        onBlur={(e) => savePrice(i, Number(e.target.value))}
                        onKeyDown={(e) => e.key === 'Enter' && (e.target as HTMLInputElement).blur()} />
                    </td>
                    <td>
                      <button className="link" onClick={() => run(() => api.saveMenuItem({ ...itemInput(i), available: !i.available }, i.id))}>
                        {i.available ? 'Oui — masquer' : 'Non — proposer'}
                      </button>
                    </td>
                    <td className="nowrap">
                      <button className="icon-btn" disabled={ii === 0} onClick={() => run(() => api.moveMenuItem(i.id, 'up'))} aria-label={`Monter ${i.name}`}>↑</button>
                      <button className="icon-btn" disabled={ii === c.items.length - 1} onClick={() => run(() => api.moveMenuItem(i.id, 'down'))} aria-label={`Descendre ${i.name}`}>↓</button>
                    </td>
                    <td className="nowrap">
                      <button className="link" onClick={() => setEditing({ kind: 'item', id: i.id, form: itemInput(i) })}>Modifier</button>{' '}
                      <button className="link link--danger" onClick={() => remove(i.name, () => api.deleteMenuItem(i.id))}>Supprimer</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {editing?.kind === 'item' && editing.form.categoryId === c.id && form}
          {!(editing?.kind === 'item' && editing.form.categoryId === c.id) && (
            <p><button className="btn btn--outline btn--sm" onClick={() => setEditing({ kind: 'item', form: emptyItem(c.id) })}>+ Ajouter un produit</button></p>
          )}
        </section>
      ))}
      {editing?.kind === 'category' && !editing.id ? (
        form
      ) : (
        <p><button className="btn btn--outline btn--sm" onClick={() => setEditing({ kind: 'category', form: EMPTY_CATEGORY })}>+ Nouvelle catégorie</button></p>
      )}
    </>
  );
}

function PhotoField({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <label className="form__full">
      Photo — identifiant Unsplash (ex. 1558220829-4694a46bb01f) ou adresse https
      <span className="pair">
        <input value={value} required maxLength={300} placeholder="1558220829-4694a46bb01f" onChange={(e) => onChange(e.target.value.trim())} />
        {value && <img className="thumb thumb--sq" src={menuPhoto(value, 120)} alt="" style={{ width: 44, height: 44 }} />}
      </span>
    </label>
  );
}

function ItemFields({ value: f, categories, isNew, onChange }: {
  value: MenuItemInput;
  categories: MenuCategory[];
  isNew: boolean;
  onChange: (f: MenuItemInput) => void;
}) {
  return (
    <>
      <h3 className="form__full">{isNew ? 'Nouveau produit' : `Modifier « ${f.name} »`}</h3>
      <label>Nom<input value={f.name} required maxLength={60} onChange={(e) => onChange({ ...f, name: e.target.value })} /></label>
      <label>Prix (DH)<input type="number" min={0} max={5000} value={f.price} required onChange={(e) => onChange({ ...f, price: Number(e.target.value) })} /></label>
      <label className="form__full">
        Description
        <input value={f.description ?? ''} maxLength={200} onChange={(e) => onChange({ ...f, description: e.target.value })} />
      </label>
      <label>
        Catégorie
        <select value={f.categoryId} onChange={(e) => onChange({ ...f, categoryId: Number(e.target.value) })}>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.light} {c.bold}</option>)}
        </select>
      </label>
      <label>Contenance (ex. 250 ml)<input value={f.volume ?? ''} maxLength={30} onChange={(e) => onChange({ ...f, volume: e.target.value })} /></label>
      <label>
        Intensité (cafés)
        <select value={f.intensity ?? 0} onChange={(e) => onChange({ ...f, intensity: Number(e.target.value) || undefined })}>
          <option value={0}>Non affichée</option>
          {[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n} / 5</option>)}
        </select>
      </label>
      <label>Pastille (ex. Nouveau, Best-seller)<input value={f.tag ?? ''} maxLength={30} onChange={(e) => onChange({ ...f, tag: e.target.value })} /></label>
      <PhotoField value={f.photo} onChange={(photo) => onChange({ ...f, photo })} />
      <label className="check form__full">
        <input type="checkbox" checked={f.featured} onChange={(e) => onChange({ ...f, featured: e.target.checked })} />
        Afficher dans « Produits populaires » sur l’accueil
      </label>
      <label className="check form__full">
        <input type="checkbox" checked={f.available} onChange={(e) => onChange({ ...f, available: e.target.checked })} />
        Disponible (affiché sur le site)
      </label>
    </>
  );
}

function CategoryFields({ value: f, isNew, onChange }: { value: MenuCategoryInput; isNew: boolean; onChange: (f: MenuCategoryInput) => void }) {
  return (
    <>
      <h3 className="form__full">{isNew ? 'Nouvelle catégorie' : `Modifier « ${f.light} ${f.bold} »`}</h3>
      <label>Titre — mot fin (ex. Hot)<input value={f.light} required maxLength={40} onChange={(e) => onChange({ ...f, light: e.target.value })} /></label>
      <label>Titre — mot gras (ex. Coffee)<input value={f.bold} required maxLength={40} onChange={(e) => onChange({ ...f, bold: e.target.value })} /></label>
      <label>Titre en arabe<input value={f.arabic ?? ''} dir="rtl" maxLength={60} onChange={(e) => onChange({ ...f, arabic: e.target.value })} /></label>
      <label>
        Couleur du bandeau
        <select value={f.tone} onChange={(e) => onChange({ ...f, tone: e.target.value as MenuTone })}>
          {(Object.keys(TONE_LABEL) as MenuTone[]).map((t) => <option key={t} value={t}>{TONE_LABEL[t]}</option>)}
        </select>
      </label>
      <PhotoField value={f.photo} onChange={(photo) => onChange({ ...f, photo })} />
      <label className="check form__full">
        <input type="checkbox" checked={f.visible} onChange={(e) => onChange({ ...f, visible: e.target.checked })} />
        Visible sur le site
      </label>
    </>
  );
}
