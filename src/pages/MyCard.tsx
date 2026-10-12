import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import QRCode from 'qrcode';
import { ApiError, STATUS_LABEL, api, customerToken, daysLeft, fmtDate, fmtDateTime, type Card } from '../api';
import { BRAND } from '../config';
import { Header } from '../components/Layout';

/** Rafraîchit la carte pendant qu'elle est affichée : le décompte change dès que le barista valide. */
const REFRESH_MS = 5000;

export default function MyCard() {
  const [card, setCard] = useState<Card | null>(null);
  const [qr, setQr] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [loggedOut, setLoggedOut] = useState(!customerToken.get());
  const [flash, setFlash] = useState(false);

  const lastLeft = useRef<number | null>(null);

  const load = useCallback(async () => {
    try {
      const next = await api.me();
      if (lastLeft.current !== null && next.drinksLeft !== lastLeft.current) {
        setFlash(true);
        setTimeout(() => setFlash(false), 1800);
      }
      lastLeft.current = next.drinksLeft;
      setCard(next);
      setError(null);
    } catch (e) {
      const err = e as ApiError;
      if (err.status === 401 || err.status === 403) {
        customerToken.set(null);
        setLoggedOut(true);
      } else {
        setError(err.message);
      }
    }
  }, []);

  useEffect(() => {
    if (loggedOut) return;
    load();
    const t = setInterval(() => document.visibilityState === 'visible' && load(), REFRESH_MS);
    return () => clearInterval(t);
  }, [load, loggedOut]);

  useEffect(() => {
    if (!card) return;
    QRCode.toDataURL(`MIZAJ:${card.qrToken}`, { width: 560, margin: 2, errorCorrectionLevel: 'M', color: { dark: '#14100d', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''));
  }, [card?.qrToken]);

  if (loggedOut) return <Navigate to="/abonnement" replace />;

  async function rotate() {
    if (!confirm('Générer un nouveau QR code ? L’ancien (capture d’écran comprise) ne fonctionnera plus.')) return;
    try {
      setCard(await api.rotateQr());
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  function logout() {
    customerToken.set(null);
    setLoggedOut(true);
  }

  return (
    <>
      <section className="app-top">
        <Header small />
      </section>
      <section className="section container mycard">
        {!card ? (
          <p className="empty">{error ?? 'Chargement de votre carte…'}</p>
        ) : (
          <>
            <div className={`member-card member-card--${card.status.toLowerCase()} ${flash ? 'member-card--flash' : ''}`}>
              <div className="member-card__top">
                <div>
                  <span className="member-card__brand">{BRAND.arabic}</span>
                  <span className="member-card__label">Carte abonné</span>
                </div>
                <span className={`badge badge--${card.status.toLowerCase()}`}>{STATUS_LABEL[card.status]}</span>
              </div>
              <div className="member-card__qr">{qr ? <img src={qr} alt="Votre QR code Mizaj" /> : <div className="qr-placeholder" />}</div>
              <div className="member-card__name">
                <strong>{card.firstName || 'Abonné Mizaj'}</strong>
                <span>{card.phone}</span>
              </div>
              {card.status === 'ACTIVE' && (
                <div className="member-card__count">
                  <div className="count-big">
                    <strong>{card.drinksLeft}</strong>
                    <span>/ {card.drinksTotal} espressos</span>
                  </div>
                  <div className="count-bar" aria-hidden>
                    <span style={{ width: `${(card.drinksLeft / card.drinksTotal) * 100}%` }} />
                  </div>
                  <p>Valable jusqu’au {fmtDate(card.expiresAt)} · {daysLeft(card.expiresAt)} jour(s)</p>
                </div>
              )}
            </div>

            <div className="mycard__info">
              <StatusHelp card={card} />
              {error && <p className="form-error">{error}</p>}
              <p className="mycard__tip">Astuce : augmentez la luminosité de l’écran pour un scan plus rapide.</p>

              {card.history.length > 0 && (
                <div className="history">
                  <h3>Derniers espressos</h3>
                  <ul>
                    {card.history.map((h) => (
                      <li key={h.at}>
                        <span>{fmtDateTime(h.at)}</span>
                        <span>{h.shop ? `Mizaj ${h.shop}` : 'Mizaj'}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="mycard__actions">
                <button className="link" onClick={rotate}>Générer un nouveau QR code</button>
                <button className="link" onClick={logout}>Se déconnecter</button>
              </div>
            </div>
          </>
        )}
      </section>
    </>
  );
}

function StatusHelp({ card }: { card: Card }) {
  const { price, drinks, days } = card.plan;
  switch (card.status) {
    case 'NONE':
      return (
        <div className="status-help">
          <h2>Dernière étape : l’activation</h2>
          <p>
            Présentez ce QR code au barista et réglez <b>{price} DH</b> en caisse. Vos <b>{drinks} espressos</b> sont activés
            immédiatement, pour {days} jours.
          </p>
          <Link to="/nos-cafes" className="btn btn--outline btn--sm">Trouver un coffee shop</Link>
        </div>
      );
    case 'ACTIVE':
      return (
        <div className="status-help">
          <h2>Bonne dégustation !</h2>
          <p>À chaque espresso, montrez ce QR code : le barista le scanne et un espresso est décompté.</p>
        </div>
      );
    case 'EMPTY':
      return (
        <div className="status-help">
          <h2>Vous avez savouré vos {card.drinksTotal} espressos</h2>
          <p>Rechargez votre carte en caisse ({price} DH) pour {drinks} nouveaux espressos valables {days} jours.</p>
        </div>
      );
    case 'EXPIRED':
      return (
        <div className="status-help">
          <h2>Votre abonnement a expiré</h2>
          <p>
            Il a pris fin le {fmtDate(card.expiresAt)}. Rechargez en caisse ({price} DH) pour {drinks} nouveaux espressos.
          </p>
        </div>
      );
  }
}
