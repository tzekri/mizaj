import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { MENU, PHOTOS, img } from '../config';
import { PageHero } from '../components/Layout';
import { Arrow, Search } from '../components/Icons';
import { ItemCard } from './MenuCategory';

export default function Menu() {
  const [q, setQ] = useState('');
  const results = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return null;
    return MENU.flatMap((c) =>
      c.items.filter((i) => `${i.name} ${i.desc}`.toLowerCase().includes(term)).map((i) => ({ ...i, slug: c.slug })),
    );
  }, [q]);

  return (
    <>
      <PageHero
        title="Mizaje Menu"
        subtitle="Savourez chaque tasse, où équilibre et arômes se rencontrent pour un plaisir unique."
        photo={img(PHOTOS.latteDark, 1920)}
      />
      <section className="section container">
        <label className="menu-search">
          <Search size={16} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Rechercher une boisson, une viennoiserie…" />
        </label>

        {results ? (
          results.length === 0 ? (
            <p className="empty">Aucun produit ne correspond à « {q} ».</p>
          ) : (
            <div className="item-grid">
              {results.map((i) => (
                <ItemCard key={i.slug + i.name} item={i} />
              ))}
            </div>
          )
        ) : (
          <div className="cat-list">
            {MENU.map((c) => (
              <Link key={c.slug} to={`/menu/${c.slug}`} className={`cat-banner cat-banner--${c.tone}`}>
                <div className="cat-banner__text">
                  <span className="cat-banner__ar">{c.arabic}</span>
                  <h2>
                    <span>{c.light}</span>
                    <strong>{c.bold}</strong>
                  </h2>
                  <span className="cat-banner__count">
                    {c.items.length} produits <Arrow size={22} />
                  </span>
                </div>
                <div className="cat-banner__img" style={{ backgroundImage: `url(${img(c.photo, 1000)})` }} />
              </Link>
            ))}
            <Link to="/jus" className="cat-banner cat-banner--accent">
              <div className="cat-banner__text">
                <span className="cat-banner__ar">عصير على مزاجك</span>
                <h2>
                  <span>Compose</span>
                  <strong>ton jus</strong>
                </h2>
                <span className="cat-banner__count">
                  Base, fruits, touche Mizaje : à ton mizaj <Arrow size={22} />
                </span>
              </div>
              <div className="cat-banner__img" style={{ backgroundImage: `url(${img('1613478223719-2ab802602423', 1000)})` }} />
            </Link>
          </div>
        )}
        <p className="menu-note">Lait végétal (avoine, amande) : +4 DH · Shot supplémentaire : +5 DH · Prix TTC.</p>
      </section>
    </>
  );
}
