import type { Ad } from '../api';

/**
 * Pub d'un partenaire telle qu'elle passe sur l'écran géant. Toutes les tailles sont en « em » :
 * le parent fixe l'échelle (grand sur la TV, petit pour l'aperçu de l'administration).
 */
export default function AdSlide({ ad }: { ad: Pick<Ad, 'name' | 'offer' | 'details' | 'image'> }) {
  return (
    <div className={`ad ${ad.image ? '' : 'ad--text'}`}>
      {ad.image && <img className="ad__img" src={ad.image} alt="" />}
      <div className="ad__text">
        <p className="ad__eyebrow">Nos partenaires</p>
        <p className="ad__name">{ad.name || 'Nom du partenaire'}</p>
        <p className="ad__offer">{ad.offer || 'Votre offre ici'}</p>
        {ad.details && <p className="ad__details">{ad.details}</p>}
      </div>
    </div>
  );
}
