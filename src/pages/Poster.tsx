import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { api, type Plan } from '../api';
import { BRAND } from '../config';
import { Header } from '../components/Layout';
import { Whatsapp } from '../components/Icons';

/**
 * Affiche à imprimer pour la caisse : QR code vers la page Abonnement.
 * Le lien part de l'adresse du site ouvert (localhost en dev, domaine Render/réel en production).
 */
export default function Poster() {
  const [url, setUrl] = useState(`${window.location.origin}/abonnement`);
  const [qr, setQr] = useState('');
  const [plans, setPlans] = useState<Plan[]>([]);

  useEffect(() => {
    api.plans().then(setPlans).catch(() => {});
  }, []);
  const from = plans.length ? Math.min(...plans.map((p) => p.price)) : null;
  const shareText = `☕ Marokoffy Coffee House lance ses abonnements${from ? ` dès ${from} DH` : ''} : votre café du quotidien à prix doux. Inscrivez-vous en 30 secondes : ${url}`;

  useEffect(() => {
    QRCode.toDataURL(url, { width: 1200, margin: 2, errorCorrectionLevel: 'H', color: { dark: '#14100d', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''));
  }, [url]);

  const local = /localhost|127\.0\.0\.1|192\.168\./.test(url);

  return (
    <>
      <section className="app-top no-print">
        <Header small />
      </section>
      <section className="section container poster-page">
        <div className="poster-tools no-print">
          <h1>Affiche « Abonnement »</h1>
          <p>À imprimer (A4 ou A5) et à poser en caisse. Les clients scannent le QR avec l’appareil photo de leur téléphone.</p>
          <label>
            Lien du QR code
            <input value={url} onChange={(e) => setUrl(e.target.value.trim())} />
          </label>
          {local && (
            <p className="form-error">
              Ce lien pointe vers votre ordinateur : il ne fonctionnera pas sur le téléphone des clients. Ouvrez cette page
              sur le site en ligne, ou saisissez l’adresse publique.
            </p>
          )}
          <div className="poster-tools__actions">
            <button className="btn btn--accent" onClick={() => window.print()}>Imprimer l’affiche</button>
            {qr && (
              <a className="btn btn--outline" href={qr} download="marokoffy-qr-abonnement.png">Télécharger le QR (PNG)</a>
            )}
            <a
              className="btn btn--whatsapp"
              href={`https://wa.me/?text=${encodeURIComponent(shareText)}`}
              target="_blank"
              rel="noreferrer"
            >
              <Whatsapp size={18} /> Partager sur WhatsApp
            </a>
          </div>
        </div>

        <article className="poster">
          <img className="poster__brand" src="/brand/logo.png" alt={`Marokoffy Coffee House — ${BRAND.arabicTagline}`} />
          <p className="poster__eyebrow">Carte d’abonnement</p>
          <h2>
            Votre café
            <br />
            {from ? <>dès <strong>{from} DH</strong></> : 'à prix doux'}
          </h2>
          {plans.length > 0 && (
            <ul className="poster__plans">
              {plans.map((p) => (
                <li key={p.code}>
                  <b>{p.name}</b>
                  <span>{p.drinks} {p.unitPlural} · {p.days} j</span>
                  <strong>{p.price} DH</strong>
                </li>
              ))}
            </ul>
          )}
          <div className="poster__qr">{qr && <img src={qr} alt="QR code vers la page Abonnement" />}</div>
          <p className="poster__cta">Scannez pour créer votre carte</p>
          <ol className="poster__steps">
            <li>Choisissez votre formule</li>
            <li>Entrez votre numéro</li>
            <li>Recevez votre carte QR</li>
            <li>Présentez-la en caisse</li>
          </ol>
          <p className="poster__url">{url.replace(/^https?:\/\//, '')}</p>
        </article>
      </section>
    </>
  );
}
