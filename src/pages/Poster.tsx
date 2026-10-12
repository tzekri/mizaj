import { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { api, type Plan } from '../api';
import { BRAND } from '../config';
import { Header } from '../components/Layout';

/**
 * Affiche à imprimer pour la caisse : QR code vers la page Abonnement.
 * Le lien part de l'adresse du site ouvert (localhost en dev, domaine Render/réel en production).
 */
export default function Poster() {
  const [url, setUrl] = useState(`${window.location.origin}/abonnement`);
  const [qr, setQr] = useState('');
  const [plan, setPlan] = useState<Plan>({ price: 120, drinks: 30, days: 30 });

  useEffect(() => {
    api.plan().then(setPlan).catch(() => {});
  }, []);

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
              <a className="btn btn--outline" href={qr} download="mizaj-qr-abonnement.png">Télécharger le QR (PNG)</a>
            )}
          </div>
        </div>

        <article className="poster">
          <span className="poster__brand">{BRAND.arabic}</span>
          <p className="poster__eyebrow">Mizaj Coffee · Abonnement</p>
          <h2>
            {plan.drinks} espressos
            <br />
            <strong>{plan.price} DH</strong> / mois
          </h2>
          <div className="poster__qr">{qr && <img src={qr} alt="QR code vers la page Abonnement" />}</div>
          <p className="poster__cta">Scannez pour créer votre carte</p>
          <ol className="poster__steps">
            <li>Entrez votre numéro WhatsApp</li>
            <li>Validez le code reçu</li>
            <li>Présentez votre QR en caisse</li>
          </ol>
          <p className="poster__url">{url.replace(/^https?:\/\//, '')}</p>
        </article>
      </section>
    </>
  );
}
