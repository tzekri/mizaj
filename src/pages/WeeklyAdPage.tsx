import { useEffect, useState } from 'react';
import { api, type WeeklyAdPublic } from '../api';
import { BRAND } from '../config';

/** Relue toutes les 5 minutes : la pub de la semaine suivante s'affiche toute seule le lundi. */
const REFRESH_MS = 5 * 60_000;

/** Pub de la semaine : l'affiche du produit mis en avant, en plein écran (TV du coffee shop ou lien partagé). */
export default function WeeklyAdPage() {
  const [ad, setAd] = useState<WeeklyAdPublic | null | undefined>(undefined);

  useEffect(() => {
    const load = () =>
      api.weeklyAd().then((a) => setAd((old) => (JSON.stringify(old) === JSON.stringify(a) ? old : a))).catch(() => {});
    load();
    const t = setInterval(load, REFRESH_MS);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    document.title = ad ? `${ad.title} · ${BRAND.name}` : `${BRAND.name} Coffee House`;
  }, [ad]);

  return (
    <div className={`promo ${ad?.description ? 'promo--caption' : ''}`}>
      {ad ? (
        <>
          <img className="promo__bg" src={ad.image} alt="" aria-hidden />
          <img className="promo__poster" key={ad.id} src={ad.image} alt={ad.description ? `${ad.title} : ${ad.description}` : ad.title} />
          {ad.description && <p className="promo__caption">{ad.description}</p>}
        </>
      ) : (
        ad === null && (
          <div className="promo__empty">
            <img src="/brand/logo.png" alt={`${BRAND.name} Coffee House`} />
            <p>La pub de la semaine arrive bientôt…</p>
          </div>
        )
      )}
      <button className="tv__fullscreen" onClick={() => document.documentElement.requestFullscreen?.().catch(() => {})} aria-label="Plein écran">⛶</button>
    </div>
  );
}
