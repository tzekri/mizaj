import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ApiError,
  api,
  customerToken,
  type JuiceCatalog,
  type JuiceCategory,
  type JuiceIngredient,
  type JuiceOrder,
  type JuiceSize,
} from '../api';
import { PageHero } from '../components/Layout';
import { IngredientThumb, JuiceGlass, JuiceTicket } from '../components/juice';
import { img, PHOTOS } from '../config';

/** Idées de départ (codes des ingrédients de départ ; ignorées si un ingrédient a été retiré). */
const IDEAS: { name: string; size: JuiceSize; codes: string[] }[] = [
  { name: 'Réveil vitaminé', size: 'S33', codes: ['orange', 'mangue', 'gingembre'] },
  { name: 'Douceur tropicale', size: 'S50', codes: ['ananas', 'mangue', 'banane'] },
  { name: 'Avocat royal', size: 'S50', codes: ['lait', 'avocat', 'amandes', 'miel'] },
  { name: 'Fraîcheur menthe', size: 'S33', codes: ['citronnade', 'fraise', 'menthe'] },
];

const STEP: Record<JuiceCategory, { n: string; title: string; hint: (max: number) => string }> = {
  BASE: { n: '1', title: 'Choisissez votre base', hint: () => 'Une base, elle fait la moitié du jus' },
  FRUIT: { n: '2', title: 'Ajoutez des fruits', hint: (max) => `Jusqu’à ${max} fruits` },
  EXTRA: { n: '3', title: 'La touche Mizaj', hint: (max) => `Jusqu’à ${max} extras` },
};

export default function Juice() {
  const [catalog, setCatalog] = useState<JuiceCatalog | null>(null);
  const [base, setBase] = useState<string | null>(null);
  const [fruits, setFruits] = useState<string[]>([]);
  const [extras, setExtras] = useState<string[]>([]);
  const [size, setSize] = useState<JuiceSize>('S33');
  const [name, setName] = useState('');
  const [order, setOrder] = useState<JuiceOrder | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<string | null>(null);
  const ticketRef = useRef<HTMLDivElement>(null);
  const loggedIn = !!customerToken.get();

  useEffect(() => {
    api.juiceCatalog().then(setCatalog).catch((e) => setError((e as ApiError).message));
  }, []);

  const byCode = useMemo(() => new Map(catalog?.ingredients.map((i) => [i.code, i]) ?? []), [catalog]);
  const chosen = [base, ...fruits, ...extras].filter(Boolean).map((c) => byCode.get(c!)).filter(Boolean) as JuiceIngredient[];
  const sizeInfo = catalog?.sizes.find((s) => s.code === size);
  const price = sizeInfo && base ? sizeInfo.basePrice + Math.round(chosen.reduce((a, i) => a + i.price, 0) * sizeInfo.factor) : null;
  const label = chosen.map((i) => i.name).join(' · ');

  // Toute modification de la recette invalide le ticket affiché
  useEffect(() => {
    setOrder(null);
    setSaved(null);
  }, [base, fruits, extras, size]);

  function toggle(list: string[], set: (v: string[]) => void, code: string, max: number) {
    if (list.includes(code)) set(list.filter((c) => c !== code));
    else if (list.length < max) set([...list, code]);
    else set([...list.slice(1), code]); // au-delà du maximum, le plus ancien choix est remplacé
  }

  function applyIdea(idea: (typeof IDEAS)[number]) {
    const ok = idea.codes.filter((c) => byCode.has(c));
    setBase(ok.find((c) => byCode.get(c)!.category === 'BASE') ?? null);
    setFruits(ok.filter((c) => byCode.get(c)!.category === 'FRUIT').slice(0, catalog!.maxFruits));
    setExtras(ok.filter((c) => byCode.get(c)!.category === 'EXTRA').slice(0, catalog!.maxExtras));
    setSize(idea.size);
    setName(idea.name);
  }

  const recipe = () => ({ name: name.trim() || undefined, size, ingredients: chosen.map((i) => i.code) });

  async function getCode() {
    setBusy(true);
    setError(null);
    try {
      setOrder(await api.juiceOrder(recipe()));
      setTimeout(() => ticketRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 60);
    } catch (e) {
      setError((e as ApiError).message);
    } finally {
      setBusy(false);
    }
  }

  async function saveFavorite() {
    setError(null);
    try {
      await api.addJuiceFavorite(recipe());
      setSaved('Ajouté à vos favoris ✓ Retrouvez-le dans « Ma carte ».');
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  const group = (cat: JuiceCategory) => catalog?.ingredients.filter((i) => i.category === cat) ?? [];

  return (
    <>
      <PageHero
        title="Compose ton jus"
        subtitle="Fruits pressés à la minute, à ton mizaj. Compose, obtiens ton code, et récupère ton jus en caisse."
        photo={img('1600271886742-f049cd451bba', 1920)}
      />
      <section className="section container juice">
        {!catalog ? (
          <p className="empty">{error ?? 'Chargement des fruits…'}</p>
        ) : (
          <>
            <div className="juice__ideas">
              <span>Envie d’une idée ?</span>
              {IDEAS.filter((i) => i.codes.some((c) => byCode.has(c))).map((idea) => (
                <button key={idea.name} type="button" className="chip" onClick={() => applyIdea(idea)}>{idea.name}</button>
              ))}
            </div>
            <div className="juice__layout">
              <div className="juice__steps">
                {(['BASE', 'FRUIT', 'EXTRA'] as JuiceCategory[]).map((cat) => {
                  const max = cat === 'FRUIT' ? catalog.maxFruits : cat === 'EXTRA' ? catalog.maxExtras : 1;
                  const selected = cat === 'BASE' ? (base ? [base] : []) : cat === 'FRUIT' ? fruits : extras;
                  return (
                    <fieldset key={cat} className="juice-step">
                      <legend>
                        <span className="juice-step__n">{STEP[cat].n}</span>
                        {STEP[cat].title}
                        <small>{STEP[cat].hint(max)} {cat !== 'BASE' && `· ${selected.length}/${max}`}</small>
                      </legend>
                      <div className="juice-step__options">
                        {group(cat).map((i) => {
                          const on = selected.includes(i.code);
                          return (
                            <button
                              key={i.code}
                              type="button"
                              className={`ingredient ${on ? 'ingredient--on' : ''}`}
                              aria-pressed={on}
                              onClick={() =>
                                cat === 'BASE' ? setBase(on ? null : i.code) : toggle(selected, cat === 'FRUIT' ? setFruits : setExtras, i.code, max)
                              }
                            >
                              <IngredientThumb item={i} size={44} />
                              <span>{i.name}</span>
                              <small>{i.price ? `+${i.price} DH` : 'inclus'}</small>
                            </button>
                          );
                        })}
                      </div>
                    </fieldset>
                  );
                })}
                <fieldset className="juice-step">
                  <legend><span className="juice-step__n">4</span>Le format</legend>
                  <div className="seg" role="group" aria-label="Format">
                    {catalog.sizes.map((s) => (
                      <button key={s.code} type="button" className={size === s.code ? 'active' : ''} onClick={() => setSize(s.code)}>
                        {s.label} · dès {s.basePrice} DH
                      </button>
                    ))}
                  </div>
                </fieldset>
              </div>

              <aside className="juice__preview">
                <JuiceGlass items={chosen} size={size} label={label} />
                <label className="juice__name">
                  Nom de votre jus <span className="optional">(facultatif)</span>
                  <input value={name} maxLength={40} placeholder="Mon Mizaj du matin" onChange={(e) => setName(e.target.value)} />
                </label>
                <p className="juice__recipe">{label || 'Choisissez une base pour commencer.'}</p>
                <div className="juice__price">
                  <span>{sizeInfo?.label}</span>
                  <strong>{price !== null ? `${price} DH` : '—'}</strong>
                </div>
                {error && <p className="form-error" role="alert">{error}</p>}
                <button className="btn btn--accent btn--block" disabled={!base || busy} onClick={getCode}>
                  {busy ? 'Un instant…' : 'Obtenir mon code'}
                </button>
                {loggedIn ? (
                  <button className="btn btn--outline btn--block" disabled={!base} onClick={saveFavorite}>★ Enregistrer en favori</button>
                ) : (
                  <p className="juice__hint">
                    <Link to="/abonnement">Créez votre carte Mizaj</Link> pour enregistrer vos jus favoris et les recommander en un geste.
                  </p>
                )}
                {saved && <p className="notice notice--ok">{saved}</p>}
              </aside>
            </div>

            {order && (
              <div className="juice__ticket" ref={ticketRef}>
                <JuiceTicket order={order} />
                <p className="juice__hint">Montrez ce code au barista : il prépare votre jus et vous réglez en caisse.</p>
              </div>
            )}
          </>
        )}
      </section>
    </>
  );
}
