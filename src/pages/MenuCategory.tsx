import { Link, Navigate, NavLink, useParams } from 'react-router-dom';
import { MENU, img, type MenuItem } from '../config';
import { PageHero } from '../components/Layout';
import { Cup, Arrow } from '../components/Icons';

export function ItemCard({ item }: { item: MenuItem }) {
  return (
    <article className="item-card">
      <div className="item-card__img">
        <img src={img(item.photo, 700)} alt={item.name} loading="lazy" />
        <span className="item-card__price">{item.price} DH</span>
      </div>
      <div className="item-card__body">
        <h3>{item.name}</h3>
        <p>{item.desc}</p>
        {(item.intensity || item.volume) && (
          <div className="item-card__meta">
            {item.intensity ? (
              <span className="intensity" aria-label={`Intensité ${item.intensity} sur 5`}>
                Intensité :
                {[1, 2, 3, 4, 5].map((n) => (
                  <Cup key={n} on={n <= item.intensity!} />
                ))}
              </span>
            ) : (
              <span />
            )}
            {item.volume && <span className="volume">{item.volume}</span>}
          </div>
        )}
      </div>
    </article>
  );
}

export default function MenuCategory() {
  const { slug } = useParams();
  const cat = MENU.find((c) => c.slug === slug);
  if (!cat) return <Navigate to="/menu" replace />;
  const idx = MENU.indexOf(cat);
  const next = MENU[(idx + 1) % MENU.length];

  return (
    <>
      <PageHero title={`${cat.light} ${cat.bold}`} subtitle={<span lang="ar" className="ar">{cat.arabic}</span>} photo={img(cat.photo, 1920)} />
      <nav className="cat-pills container" aria-label="Catégories du menu">
        {MENU.map((c) => (
          <NavLink key={c.slug} to={`/menu/${c.slug}`}>
            {c.light} {c.bold}
          </NavLink>
        ))}
      </nav>
      <section className="section container">
        <div className="item-grid">
          {cat.items.map((i) => (
            <ItemCard key={i.name} item={i} />
          ))}
        </div>
        <div className="cat-footer">
          <Link to="/menu" className="btn btn--outline">← Tout le menu</Link>
          <Link to={`/menu/${next.slug}`} className="btn btn--accent">
            {next.light} {next.bold} <Arrow size={20} />
          </Link>
        </div>
      </section>
    </>
  );
}
