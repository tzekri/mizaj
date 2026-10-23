import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { PHOTOS, img } from '../config';
import { menuPhoto, useMenu } from '../menu';
import { PageHero } from '../components/Layout';
import { Arrow, Search } from '../components/Icons';
import { ItemCard } from './MenuCategory';

export default function Menu() {
  const [q, setQ] = useState('');
  const { menu, error } = useMenu();
  const results = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term || !menu) return null;
    return menu.flatMap((c) => c.items.filter((i) => `${i.name} ${i.description ?? ''}`.toLowerCase().includes(term)));
  }, [q, menu]);

  return (
    <>
      <PageHero
        eyebrow="La carte"
        title="Kidima Menu"
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
                <ItemCard key={i.id} item={i} />
              ))}
            </div>
          )
        ) : (
          <>
            {!menu && <p className="empty">{error ?? 'Chargement du menu…'}</p>}
            <div className="cat-grid">
              {menu?.map((c) => (
                <Link key={c.slug} to={`/menu/${c.slug}`} className="cat-card">
                  <img src={menuPhoto(c.photo, 700)} alt="" loading="lazy" />
                  <div className="cat-card__body">
                    {c.arabic && <span className="cat-card__ar" lang="ar">{c.arabic}</span>}
                    <h2>{c.light} <strong>{c.bold}</strong></h2>
                    <span className="cat-card__count">{c.items.length} produits <Arrow size={18} /></span>
                  </div>
                </Link>
              ))}
              <Link to="/jus" className="cat-card cat-card--juice">
                <img src={img('1613478223719-2ab802602423', 700)} alt="" loading="lazy" />
                <div className="cat-card__body">
                  <span className="cat-card__ar" lang="ar">عصير على ذوقك</span>
                  <h2>Compose <strong>ton jus</strong></h2>
                  <span className="cat-card__count">Base, fruits, touche Kidima <Arrow size={18} /></span>
                </div>
              </Link>
            </div>
          </>
        )}
        <p className="menu-note">Lait végétal (avoine, amande) : +4 DH · Shot supplémentaire : +5 DH · Prix TTC.</p>
      </section>
    </>
  );
}
