import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { BRAND, SITE_URL } from '../config';
import { Header } from '../components/Layout';
import { GROUPS, type UsefulLink } from './LinksAdmin';

/** Phrase d'appel sous chaque QR code, à la place de la description de l'onglet admin. */
const CTA: Record<string, string> = {
  accueil: 'Découvrez Kidima',
  menu: 'Notre carte et nos prix',
  abonnement: 'Votre café du quotidien à prix doux',
  'ma-carte': 'Votre carte et vos boissons restantes',
  smoothie: 'Composez votre smoothie',
  defi: 'Arrêtez le chrono pile sur 10.00 s',
  promo: 'Le produit de la semaine',
  'nos-cafes': 'Adresses et horaires',
};

/** Planche A4 des QR codes destinés aux clients (liens du groupe « Pour les clients »), à imprimer et découper. */
export default function QrSheet() {
  const links = GROUPS[0].links;

  useEffect(() => {
    document.title = `QR codes clients · ${BRAND.name}`;
  }, []);

  return (
    <>
      <section className="app-top on-dark no-print">
        <Header small />
      </section>
      <section className="section container qr-sheet-page">
        <div className="poster-tools no-print">
          <h1>QR codes clients</h1>
          <p>Une planche A4 avec les {links.length} QR codes pour les clients : à imprimer, découper et poser sur les tables ou en caisse.</p>
          <div className="poster-tools__actions">
            <button className="btn btn--accent" onClick={() => window.print()}>Imprimer la planche</button>
          </div>
        </div>
        <div className="qr-sheet">
          <header className="qr-sheet__head">
            <img src="/brand/logo.png" alt={`${BRAND.name} Coffee House`} />
            <p>Scannez avec l’appareil photo de votre téléphone</p>
          </header>
          <div className="qr-sheet__grid">
            {links.map((l) => (
              <QrTile key={l.file} link={l} />
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

function QrTile({ link }: { link: UsefulLink }) {
  const url = `${SITE_URL}${link.path}`;
  const [qr, setQr] = useState('');

  useEffect(() => {
    QRCode.toDataURL(url, { width: 900, margin: 1, errorCorrectionLevel: 'M', color: { dark: '#0b2545', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''));
  }, [url]);

  return (
    <article className="qr-tile">
      <div className="qr-tile__code">{qr && <img src={qr} alt={`QR code : ${link.title}`} />}</div>
      <div className="qr-tile__text">
        <h2>{link.title}</h2>
        <p>{CTA[link.file] ?? link.text}</p>
        <span>{url.replace(/^https?:\/\//, '').replace(/\/$/, '')}</span>
      </div>
    </article>
  );
}
