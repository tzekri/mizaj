import { useEffect, useState, type ReactNode } from 'react';
import QRCode from 'qrcode';
import { api, type Plan } from '../api';
import { BRAND } from '../config';
import { Header } from '../components/Layout';
import { Whatsapp } from '../components/Icons';
import LedDisplay from '../components/LedDisplay';

/**
 * Affiche à imprimer pour la caisse : QR code vers la page Abonnement.
 * Le lien part de l'adresse du site ouvert (localhost en dev, domaine Render/réel en production).
 */
export default function Poster() {
  const [plans, setPlans] = useState<Plan[]>([]);

  useEffect(() => {
    api.plans().then(setPlans).catch(() => {});
  }, []);
  const from = plans.length ? Math.min(...plans.map((p) => p.price)) : null;

  return (
    <PosterPage
      path="/abonnement"
      title="Affiche « Abonnement »"
      file="kidima-qr-abonnement.png"
      qrAlt="QR code vers la page Abonnement"
      share={(url) => `☕ Kidima Coffee House lance ses abonnements${from ? ` dès ${from} DH` : ''} : votre café du quotidien à prix doux. Inscrivez-vous en 30 secondes : ${url}`}
      top={
        <>
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
        </>
      }
      cta="Scannez pour créer votre carte"
      steps={['Choisissez votre formule', 'Entrez votre numéro', 'Recevez votre carte QR', 'Présentez-la en caisse']}
    />
  );
}

/** Affiche du défi « 10 secondes » : QR code vers la page du jeu. */
export function ChronoPoster() {
  const [prize, setPrize] = useState<string | null>(null);

  useEffect(() => {
    api.chronoInfo('affiche').then((i) => setPrize(i.prize)).catch(() => {});
  }, []);

  return (
    <PosterPage
      path="/defi"
      title="Affiche « Défi 10 secondes »"
      file="kidima-qr-defi.png"
      qrAlt="QR code vers le défi 10 secondes"
      share={(url) => `⏱️ Défi Kidima : arrêtez le chrono pile sur 10.00 s et gagnez ${prize ? prize.toLowerCase() : 'un cadeau'} ! Tentez votre chance : ${url}`}
      top={
        <>
          <p className="poster__eyebrow">Jeu · Défi 10 secondes</p>
          <h2>
            Arrêtez le chrono
            <br />
            <strong>pile sur 10.00 s</strong>
          </h2>
          <div className="poster__led">
            <LedDisplay text="10.00" label="10.00 secondes" />
          </div>
          <p className="poster__prize">
            et gagnez <strong>{prize ?? 'un cadeau'}</strong>
          </p>
        </>
      }
      cta="Scannez et tentez votre chance"
      steps={['Scannez le QR code', 'Appuyez sur Démarrer', 'Arrêtez pile à 10.00 s', 'Montrez le bon en caisse']}
    />
  );
}

type PosterPageProps = {
  /** Page du site vers laquelle pointe le QR code. */
  path: string;
  title: string;
  /** Nom du fichier PNG du QR code téléchargé. */
  file: string;
  qrAlt: string;
  share: (url: string) => string;
  /** Haut de l'affiche, entre le logo et le QR code. */
  top: ReactNode;
  cta: string;
  steps: string[];
};

/** Page d'affiche : outils (lien, impression, QR en PNG, partage WhatsApp) et affiche A4. */
function PosterPage({ path, title, file, qrAlt, share, top, cta, steps }: PosterPageProps) {
  const [url, setUrl] = useState(`${window.location.origin}${path}`);
  const [qr, setQr] = useState('');

  useEffect(() => {
    QRCode.toDataURL(url, { width: 1200, margin: 2, errorCorrectionLevel: 'H', color: { dark: '#14100d', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''));
  }, [url]);

  const local = /localhost|127\.0\.0\.1|192\.168\./.test(url);

  return (
    <>
      <section className="app-top on-dark no-print">
        <Header small />
      </section>
      <section className="section container poster-page">
        <div className="poster-tools no-print">
          <h1>{title}</h1>
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
              <a className="btn btn--outline" href={qr} download={file}>Télécharger le QR (PNG)</a>
            )}
            <a
              className="btn btn--whatsapp"
              href={`https://wa.me/?text=${encodeURIComponent(share(url))}`}
              target="_blank"
              rel="noreferrer"
            >
              <Whatsapp size={18} /> Partager sur WhatsApp
            </a>
          </div>
        </div>

        <article className="poster">
          <img className="poster__brand" src="/brand/logo.png" alt={`Kidima Coffee House — ${BRAND.slogan}`} />
          {top}
          <div className="poster__qr">{qr && <img src={qr} alt={qrAlt} />}</div>
          <p className="poster__cta">{cta}</p>
          <ol className="poster__steps">
            {steps.map((s) => <li key={s}>{s}</li>)}
          </ol>
          <p className="poster__url">{url.replace(/^https?:\/\//, '')}</p>
        </article>
      </section>
    </>
  );
}
