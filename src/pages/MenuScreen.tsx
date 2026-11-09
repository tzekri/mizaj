import { useEffect, useMemo, useState } from 'react';
import { api, type MenuCategory, type MenuItem } from '../api';
import { BRAND } from '../config';
import { menuPhoto } from '../menu';

/** Durée d'affichage d'une page du menu. */
const SLIDE_MS = 12_000;
/** Le menu est relu toutes les minutes : un prix changé dans l'administration s'affiche sans toucher à la TV. */
const REFRESH_MS = 60_000;
/** Au-delà, une catégorie est coupée en plusieurs pages (lisible de loin). */
const ITEMS_PER_PAGE = 6;

type Slide = { category: MenuCategory; items: MenuItem[]; page: number; pages: number };

/** Catégories choisies dans l'adresse (?categories=hot-coffee,jus) ; aucune : tout le menu. */
const chosen = () =>
  (new URLSearchParams(window.location.search).get('categories') ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

/** Écran menu du coffee shop (TV au-dessus du comptoir) : une catégorie à la fois, prix à jour depuis l'administration. */
export default function MenuScreen() {
  const [menu, setMenu] = useState<MenuCategory[] | null>(null);
  const [index, setIndex] = useState(0);
  const [only] = useState(chosen);

  useEffect(() => {
    const load = () =>
      api.menu().then((m) =>
        // Même menu qu'avant : on garde l'objet pour ne pas relancer le défilement
        setMenu((old) => (JSON.stringify(old) === JSON.stringify(m) ? old : m)),
      ).catch(() => {});
    load();
    const t = setInterval(load, REFRESH_MS);
    return () => clearInterval(t);
  }, []);

  const slides = useMemo<Slide[]>(() => {
    const categories = (menu ?? []).filter((c) => only.length === 0 || only.includes(c.slug));
    return categories.flatMap((category) => {
      const pages = Math.ceil(category.items.length / ITEMS_PER_PAGE);
      return Array.from({ length: pages }, (_, page) => ({
        category,
        items: category.items.slice(page * ITEMS_PER_PAGE, (page + 1) * ITEMS_PER_PAGE),
        page,
        pages,
      }));
    });
  }, [menu, only]);

  useEffect(() => {
    if (slides.length < 2) return;
    const t = setInterval(() => setIndex((i) => (i + 1) % slides.length), SLIDE_MS);
    return () => clearInterval(t);
  }, [slides.length]);

  const slide = slides.length ? slides[index % slides.length] : null;

  return (
    <div className="mscreen">
      <header className="mscreen__top">
        <img className="mscreen__logo" src="/brand/logo.png" alt="Kidima Coffee House" />
        <div>
          <p className="mscreen__brand">{BRAND.name} Coffee House</p>
          <p className="mscreen__slogan">{BRAND.slogan}</p>
        </div>
      </header>

      {!slide ? (
        <p className="mscreen__empty">{menu ? 'Menu bientôt disponible' : 'Chargement du menu…'}</p>
      ) : (
        <main className="mscreen__slide" key={`${slide.category.id}-${slide.page}`}>
          <div className="mscreen__photo">
            <img src={menuPhoto(slide.category.photo, 1400)} alt="" />
          </div>
          <section className="mscreen__list">
            <h1>
              <span>{slide.category.light}</span> {slide.category.bold}
              {slide.category.arabic && <small lang="ar">{slide.category.arabic}</small>}
            </h1>
            <ul>
              {slide.items.map((i) => (
                <li key={i.id}>
                  <div className="mscreen__line">
                    <span className="mscreen__name">
                      {i.name}
                      {i.tag && <em>{i.tag}</em>}
                    </span>
                    <span className="mscreen__dots" aria-hidden />
                    <span className="mscreen__price">{i.price} DH</span>
                  </div>
                  {(i.description || i.volume) && (
                    <p className="mscreen__desc">{[i.description, i.volume].filter(Boolean).join(' · ')}</p>
                  )}
                </li>
              ))}
            </ul>
            {slide.pages > 1 && <p className="mscreen__more">Page {slide.page + 1} / {slide.pages}</p>}
          </section>
        </main>
      )}

      {slides.length > 1 && (
        <nav className="mscreen__dotsnav" aria-hidden>
          {slides.map((s, k) => <i key={k} className={k === index % slides.length ? 'on' : ''} />)}
        </nav>
      )}
      <button className="tv__fullscreen" onClick={() => document.documentElement.requestFullscreen?.().catch(() => {})} aria-label="Plein écran">⛶</button>
    </div>
  );
}
