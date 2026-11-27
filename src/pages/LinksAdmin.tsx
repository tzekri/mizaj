import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { SHOPS, SITE_URL } from '../config';
import { tvLink } from './PartnersAdmin';

export type UsefulLink = { path: string; title: string; text: string; file: string };
type Group = { title: string; note: string; links: UsefulLink[] };

export const GROUPS: Group[] = [
  {
    title: 'Pour les clients',
    note: 'À partager sur WhatsApp, Instagram, ou en QR code sur les tables et en caisse.',
    links: [
      { path: '/', title: 'Accueil', text: 'La page d’accueil du site.', file: 'accueil' },
      { path: '/menu', title: 'Menu', text: 'Toutes les boissons et leurs prix.', file: 'menu' },
      { path: '/abonnement', title: 'Abonnement', text: 'Créer sa carte d’abonnement en 30 secondes.', file: 'abonnement' },
      { path: '/ma-carte', title: 'Ma carte', text: 'Le client retrouve sa carte QR et ses boissons restantes.', file: 'ma-carte' },
      { path: '/smoothie', title: 'Compose ton smoothie', text: 'Le client compose son smoothie et le montre en caisse.', file: 'smoothie' },
      { path: '/defi', title: 'Défi 10 secondes', text: 'Le jeu du chrono, à jouer sur son téléphone.', file: 'defi' },
      { path: '/promo', title: 'Pub de la semaine', text: 'L’affiche du produit de la semaine.', file: 'promo' },
      { path: '/nos-cafes', title: 'Nos cafés', text: 'Adresses et horaires des coffee shops.', file: 'nos-cafes' },
    ],
  },
  {
    title: 'Écrans TV',
    note: 'À ouvrir sur la TV, puis ⛶ pour le plein écran. Tout se met à jour tout seul.',
    links: [
      { path: '/menu/tableau', title: 'Tableau menu', text: 'Cafés chauds puis boissons fraîches, toutes les 25 secondes.', file: 'tv-tableau' },
      { path: '/menu/tableau?tableau=chauds', title: 'Tableau · cafés chauds', text: 'Seulement les cafés chauds.', file: 'tv-chauds' },
      { path: '/menu/tableau?tableau=frais', title: 'Tableau · boissons fraîches', text: 'Seulement les boissons fraîches.', file: 'tv-frais' },
      { path: '/menu/ecran', title: 'Écran menu', text: 'Une catégorie à la fois, avec les photos.', file: 'tv-menu' },
      { path: '/promo', title: 'Pub de la semaine', text: 'L’affiche de la semaine en plein écran.', file: 'tv-promo' },
      ...SHOPS.map((s) => ({
        path: tvLink(s.name),
        title: `Défi · écran ${s.name}`,
        text: 'Le jeu en grand, avec les pubs des partenaires entre deux parties.',
        file: `tv-defi-${s.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
      })),
    ],
  },
  {
    title: 'Affiches à imprimer',
    note: 'Affiches A4 avec QR code, à poser en caisse ou sur les tables.',
    links: [
      { path: '/affiche', title: 'Affiche Abonnement', text: 'QR code vers l’abonnement, avec les formules et les prix.', file: 'affiche-abonnement' },
      { path: '/affiche/defi', title: 'Affiche Défi 10 secondes', text: 'QR code vers le jeu, avec le cadeau à gagner.', file: 'affiche-defi' },
    ],
  },
  {
    title: 'Équipe',
    note: 'Réservé au personnel.',
    links: [{ path: '/barista', title: 'Espace barista', text: 'Comptoir, tableau de bord et administration.', file: 'barista' }],
  },
];

/** Onglet « Liens & QR » : toutes les adresses utiles du site, avec leur QR code à télécharger. */
export default function LinksAdmin() {
  const base = SITE_URL;

  return (
    <div className="links-admin">
      <div className="links-admin__head">
        <div>
          <h2>Liens utiles & QR codes</h2>
          <p className="dash-note">
            Toutes les adresses du site. Cliquez sur un QR code pour le télécharger en grand (PNG), prêt à imprimer.
          </p>
        </div>
        <button className="btn btn--outline btn--sm no-print" onClick={() => window.print()}>Imprimer la page</button>
      </div>
      {GROUPS.map((g) => (
        <section key={g.title} className="links-admin__group">
          <h3>{g.title}</h3>
          <p className="dash-note">
            {g.note}
            {g === GROUPS[0] && <> · <a href="/qr" target="_blank" rel="noreferrer">Planche de QR codes clients à imprimer</a></>}
          </p>
          <div className="links-admin__grid">
            {g.links.map((l) => (
              <LinkCard key={l.file} link={l} url={`${base}${l.path}`} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function LinkCard({ link, url }: { link: UsefulLink; url: string }) {
  const [qr, setQr] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    QRCode.toDataURL(url, { width: 1200, margin: 2, errorCorrectionLevel: 'M', color: { dark: '#0b2545', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''));
  }, [url]);

  async function copy() {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      window.prompt('Copiez le lien :', url);
    }
  }

  return (
    <article className="link-card">
      <a className="link-card__qr" href={qr || undefined} download={`kidima-qr-${link.file}.png`} title="Télécharger le QR code (PNG)">
        {qr && <img src={qr} alt={`QR code : ${link.title}`} />}
      </a>
      <div className="link-card__body">
        <h4>{link.title}</h4>
        <p>{link.text}</p>
        <code>{url.replace(/^https?:\/\//, '')}</code>
        <div className="link-card__actions no-print">
          <a className="link" href={link.path} target="_blank" rel="noreferrer">Ouvrir</a>
          <button className="link" onClick={copy}>{copied ? 'Copié ✓' : 'Copier le lien'}</button>
          {qr && <a className="link" href={qr} download={`kidima-qr-${link.file}.png`}>QR en PNG</a>}
        </div>
      </div>
    </article>
  );
}
