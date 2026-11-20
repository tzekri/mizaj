import { useEffect, useState } from 'react';
import { api, type MenuCategory, type MenuItem } from '../api';
import { BRAND } from '../config';
import { menuPhoto, prices } from '../menu';

/** Tableaux menu (TV) : les cafés chauds en vignettes, les boissons fraîches en colonnes. */
const BOARDS = [
  { id: 'chauds', slugs: ['coffee', 'hot-coffee'], layout: 'cards' },
  { id: 'frais', slugs: ['ice-coffee', 'ice-trendy', 'frappe', 'cocktail', 'milkshake'], layout: 'sections' },
] as const;
type Board = (typeof BOARDS)[number];

/** Sans ?tableau=…, la TV alterne les deux tableaux. */
const SWITCH_MS = 25_000;
/** Le menu est relu toutes les minutes : un prix changé dans l'administration s'affiche tout seul. */
const REFRESH_MS = 60_000;

const chosen = () => BOARDS.find((b) => b.id === new URLSearchParams(window.location.search).get('tableau')) ?? null;

/** Tableau menu Kidima pour la TV du comptoir, à la façon d'un menu board : bleu foncé, or, prix en pastilles. */
export default function MenuBoard() {
  const [menu, setMenu] = useState<MenuCategory[] | null>(null);
  const [fixed] = useState(chosen);
  const [index, setIndex] = useState(0);

  useEffect(() => {
    const load = () =>
      api.menu().then((m) => setMenu((old) => (JSON.stringify(old) === JSON.stringify(m) ? old : m))).catch(() => {});
    load();
    const t = setInterval(load, REFRESH_MS);
    return () => clearInterval(t);
  }, []);

  // Tableaux qui ont au moins une catégorie affichée
  const boards = BOARDS.filter((b) => menu?.some((c) => (b.slugs as readonly string[]).includes(c.slug)));
  useEffect(() => {
    if (fixed || boards.length < 2) return;
    const t = setInterval(() => setIndex((i) => i + 1), SWITCH_MS);
    return () => clearInterval(t);
  }, [fixed, boards.length]);

  const board = fixed ?? (boards.length ? boards[index % boards.length] : null);
  const categories = board ? board.slugs.map((s) => menu?.find((c) => c.slug === s)).filter(Boolean) as MenuCategory[] : [];

  return (
    <div className="board">
      <header className="board__head">
        <div className="board__brand">
          <img src="/brand/logo.png" alt="" />
          <div>
            <p className="board__name">KIDIMA <span lang="ar">كيديما</span></p>
            <p className="board__sub">COFFEE HOUSE · <span lang="ar">كافي</span></p>
          </div>
        </div>
        <p className="board__slogan" lang="ar">
          {BRAND.slogan}
          <svg viewBox="0 0 200 18" aria-hidden><path d="M4 14 Q90 -2 196 6" /></svg>
        </p>
      </header>

      {!menu ? (
        <p className="board__empty">Chargement du menu…</p>
      ) : board?.layout === 'cards' ? (
        <Cards items={categories.flatMap((c) => c.items)} key={board.id} />
      ) : board ? (
        <Sections categories={categories} key={board.id} />
      ) : (
        <p className="board__empty">Menu bientôt disponible</p>
      )}

      <button className="tv__fullscreen" onClick={() => document.documentElement.requestFullscreen?.().catch(() => {})} aria-label="Plein écran">⛶</button>
    </div>
  );
}

/** Pastille(s) de prix, avec le nom de chaque prix dessous (S / L, Intenso / Dolce crema). */
function Price({ item }: { item: MenuItem }) {
  const list = prices(item);
  return (
    <span className="board-price">
      <span className="board-price__pill">
        {list.map((p, k) => (
          <b key={k}>{p.price}<small>DH</small></b>
        ))}
      </span>
      {list.some((p) => p.label) && (
        <span className="board-price__labels">
          {list.map((p, k) => <i key={k}>{p.label}</i>)}
        </span>
      )}
    </span>
  );
}

/** Tableau des cafés chauds : une vignette par boisson. */
function Cards({ items }: { items: MenuItem[] }) {
  const columns = Math.ceil(items.length / 2);
  return (
    <main className="board__cards" style={{ gridTemplateColumns: `repeat(${Math.max(columns, 4)}, 1fr)` }}>
      {items.map((i) => (
        <article key={i.id} className="board-card">
          <img src={menuPhoto(i.photo, 500)} alt="" />
          <h2>{i.name}</h2>
          <Price item={i} />
        </article>
      ))}
    </main>
  );
}

/** Tableau des boissons fraîches : une colonne par catégorie, la liste des prix et les photos. */
function Sections({ categories }: { categories: MenuCategory[] }) {
  return (
    <main className={`board__sections board__sections--${categories.length}`}>
      {categories.map((c) => (
        <section key={c.id} className="board-sec">
          <h2>
            {c.light} {c.bold}
          </h2>
          <ul>
            {c.items.map((i) => (
              <li key={i.id}>
                <span className="board-sec__name">
                  {i.name}
                  {/* Sous-titres courts seulement (« Vanille & coco ») : le tableau reste lisible de loin */}
                  {i.description && i.description.length <= 28 && <small>{i.description}</small>}
                </span>
                <Price item={i} />
              </li>
            ))}
          </ul>
          <div className="board-sec__photos">
            {c.items.slice(0, 5).map((i) => <img key={i.id} src={menuPhoto(i.photo, 300)} alt="" />)}
          </div>
        </section>
      ))}
    </main>
  );
}
