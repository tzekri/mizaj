import { PHOTOS, SHOPS, img } from '../config';
import { PageHero } from '../components/Layout';
import { Clock, Pin } from '../components/Icons';
import { isOpenNow } from '../hours';

export default function Shops() {
  return (
    <>
      <PageHero
        eyebrow="Venez nous voir"
        title="Nos coffee shops"
        subtitle="Trois adresses à Casablanca pour savourer votre café, sur place ou à emporter."
        photo={img(PHOTOS.barDark, 1920)}
      />
      <section className="section container shop-list">
        {SHOPS.map((s, k) => {
          const open = isOpenNow(s.hours);
          return (
          <article key={s.name} className={`shop-row ${k % 2 ? 'shop-row--rev' : ''}`}>
            <div className="shop-row__img">
              <img src={img(s.photo, 1000)} alt={`Coffee shop Kidima ${s.name}`} loading="lazy" />
              {open !== null && <span className={`shop__status ${open ? 'shop__status--open' : ''}`}>{open ? 'Ouvert maintenant' : 'Fermé pour le moment'}</span>}
            </div>
            <div>
              <p className="eyebrow">Kidima Coffee House</p>
              <h2>{s.name}</h2>
              <p><Pin /> {s.address}<br />{s.city}</p>
              <p><Clock /> Tous les jours, {s.hours}</p>
              <a
                className="btn btn--accent"
                href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${s.address}, ${s.city}`)}`}
                target="_blank"
                rel="noreferrer"
              >
                Itinéraire
              </a>
            </div>
          </article>
          );
        })}
      </section>
    </>
  );
}
