import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, Navigate, useSearchParams } from 'react-router-dom';
import QRCode from 'qrcode';
import {
  ApiError,
  STATUS_LABEL,
  api,
  customerToken,
  daysLeft,
  fmtDate,
  fmtDateTime,
  type Card,
  type JuiceFavorite,
  type JuiceOrder,
  type Plan,
} from '../api';
import { IngredientThumb, JuiceTicket } from '../components/juice';
import { BRAND } from '../config';
import { Header } from '../components/Layout';
import { Whatsapp } from '../components/Icons';
import { renderCardImage, shareCard } from '../cardImage';

/** Rafraîchit la carte pendant qu'elle est affichée : le décompte change dès que le barista valide. */
const REFRESH_MS = 5000;

export default function MyCard() {
  const [card, setCard] = useState<Card | null>(null);
  const [qr, setQr] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [loggedOut, setLoggedOut] = useState(!customerToken.get());
  const [flash, setFlash] = useState(false);
  const [params] = useSearchParams();
  const welcome = params.has('bienvenue');

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
    QRCode.toDataURL(`MIZAJE:${card.qrToken}`, { width: 560, margin: 2, errorCorrectionLevel: 'M', color: { dark: '#14100d', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''));
  }, [card?.qrToken]);

  if (loggedOut) return <Navigate to="/abonnement" replace />;

  async function rotate() {
    if (!confirm('Générer un nouveau QR code ? L’ancien (capture d’écran ou image envoyée sur WhatsApp comprise) ne fonctionnera plus.')) return;
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
            <div className="mycard__left">
              {welcome && (
                <p className="notice notice--ok">Votre carte est prête ✓ Envoyez-la sur WhatsApp pour l’avoir toujours sous la main.</p>
              )}
              <div className={`member-card member-card--${card.status.toLowerCase()} ${flash ? 'member-card--flash' : ''}`}>
                <div className="member-card__top">
                  <div>
                    <img className="member-card__brand" src="/brand/logo-mark-light.png" alt="Mizaje Coffee" />
                    <span className="member-card__label">Formule {card.plan.name}</span>
                  </div>
                  <span className={`badge badge--${card.status.toLowerCase()}`}>{STATUS_LABEL[card.status]}</span>
                </div>
                <div className="member-card__qr">{qr ? <img src={qr} alt="Votre QR code Mizaje" /> : <div className="qr-placeholder" />}</div>
                <div className="member-card__name">
                  <strong>{card.firstName || 'Abonné Mizaje'}</strong>
                  <span>{card.phone}</span>
                </div>
                {card.status === 'ACTIVE' && (
                  <div className="member-card__count">
                    <div className="count-big">
                      <strong>{card.drinksLeft}</strong>
                      <span>/ {card.drinksTotal} {card.plan.unitPlural}</span>
                    </div>
                    <div className="count-bar" aria-hidden>
                      <span style={{ width: `${(card.drinksLeft / card.drinksTotal) * 100}%` }} />
                    </div>
                    <p>Valable jusqu’au {fmtDate(card.expiresAt)} · {daysLeft(card.expiresAt)} jour(s)</p>
                  </div>
                )}
              </div>
              <ShareCard card={card} />
            </div>

            <div className="mycard__info">
              <StatusHelp card={card} onPlanChange={setCard} />
              {error && <p className="form-error">{error}</p>}
              <HomeScreenTip />
              <JuiceFavorites />

              {card.history.length > 0 && (
                <div className="history">
                  <h3>Dernières consommations</h3>
                  <ul>
                    {card.history.map((h) => (
                      <li key={h.at}>
                        <span>{fmtDateTime(h.at)}</span>
                        <span>{h.shop ? `Mizaje ${h.shop}` : 'Mizaje'}</span>
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

/** Envoi gratuit de la carte sur WhatsApp : l'image est créée dans le téléphone puis partagée par le client lui-même. */
function ShareCard({ card }: { card: Card }) {
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  async function share() {
    setBusy(true);
    setMsg(null);
    try {
      const blob = await renderCardImage({ qrToken: card.qrToken, firstName: card.firstName, planName: card.plan.name });
      const text = `Ma carte Mizaje Coffee 🎫 à présenter en caisse. Solde à jour : ${window.location.origin}/ma-carte`;
      const result = await shareCard(blob, text);
      if (result === 'downloaded') {
        setMsg('Image de la carte téléchargée : envoyez-la-vous sur WhatsApp (conversation « Moi ») ou gardez-la dans vos photos.');
      } else if (result === 'shared') {
        setMsg('Carte partagée ✓');
      }
    } catch {
      setMsg('Impossible de créer l’image de la carte sur cet appareil. Faites plutôt une capture d’écran.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="share-card">
      <button className="btn btn--whatsapp btn--block" onClick={share} disabled={busy}>
        <Whatsapp size={20} /> {busy ? 'Préparation…' : 'Envoyer ma carte sur WhatsApp'}
      </button>
      <p className="share-card__hint">
        Choisissez WhatsApp puis votre propre conversation (« Moi ») : la carte sera toujours dans votre téléphone, même sans
        connexion.
      </p>
      {msg && <p className="share-card__msg">{msg}</p>}
    </div>
  );
}

function HomeScreenTip() {
  const ios = /iphone|ipad|ipod/i.test(navigator.userAgent);
  return (
    <details className="tip">
      <summary>Ajouter la carte à l’écran d’accueil</summary>
      <p>
        {ios
          ? 'Dans Safari, touchez le bouton Partager (carré avec une flèche) puis « Sur l’écran d’accueil ».'
          : 'Dans Chrome, touchez le menu ⋮ puis « Ajouter à l’écran d’accueil ».'}{' '}
        Votre carte s’ouvrira en un geste, toujours à jour. Pour un scan rapide, augmentez la luminosité de l’écran.
      </p>
    </details>
  );
}

function StatusHelp({ card, onPlanChange }: { card: Card; onPlanChange: (c: Card) => void }) {
  const { price, drinks, days, unitPlural, unit } = card.plan;
  const canChange = card.status !== 'ACTIVE';
  switch (card.status) {
    case 'NONE':
      return (
        <div className="status-help">
          <h2>Dernière étape : l’activation</h2>
          <p>
            Présentez ce QR code au barista et réglez <b>{price} DH</b> en caisse. Votre formule est activée immédiatement :
            <b> {drinks} {unitPlural}</b> pendant {days} jours.
          </p>
          {canChange && <PlanSwitcher current={card.plan} onChange={onPlanChange} />}
          <Link to="/nos-cafes" className="btn btn--outline btn--sm">Trouver un coffee shop</Link>
        </div>
      );
    case 'ACTIVE':
      return (
        <div className="status-help">
          <h2>Bonne dégustation !</h2>
          <p>À chaque {unit}, montrez ce QR code : le barista le scanne et une boisson est décomptée.</p>
        </div>
      );
    case 'EMPTY':
      return (
        <div className="status-help">
          <h2>Vous avez savouré vos {card.drinksTotal} {unitPlural}</h2>
          <p>Rechargez votre carte en caisse pour une nouvelle période.</p>
          <PlanSwitcher current={card.plan} onChange={onPlanChange} />
        </div>
      );
    case 'EXPIRED':
      return (
        <div className="status-help">
          <h2>Votre abonnement a expiré</h2>
          <p>Il a pris fin le {fmtDate(card.expiresAt)}. Rechargez en caisse pour une nouvelle période.</p>
          <PlanSwitcher current={card.plan} onChange={onPlanChange} />
        </div>
      );
  }
}

/** Avant l'activation (ou pour la recharge), le client peut changer de formule ; le barista la verra. */
function PlanSwitcher({ current, onChange }: { current: Plan; onChange: (c: Card) => void }) {
  const [plans, setPlans] = useState<Plan[]>([]);
  useEffect(() => {
    api.plans().then(setPlans).catch(() => {});
  }, []);
  if (plans.length < 2) return null;
  return (
    <label className="plan-switch">
      Formule souhaitée
      <select
        value={current.code}
        onChange={(e) => api.updateProfile({ planCode: e.target.value }).then(onChange).catch(() => {})}
      >
        {plans.map((p) => (
          <option key={p.code} value={p.code}>
            {p.name} — {p.price} DH · {p.drinks} {p.unitPlural} · {p.days} j
          </option>
        ))}
      </select>
    </label>
  );
}

/** Jus favoris de l'abonné : un geste pour obtenir un nouveau code à présenter en caisse. */
function JuiceFavorites() {
  const [favorites, setFavorites] = useState<JuiceFavorite[] | null>(null);
  const [order, setOrder] = useState<JuiceOrder | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api.juiceFavorites().then(setFavorites).catch(() => setFavorites([]));
  }, []);
  useEffect(load, [load]);

  async function reorder(id: number) {
    setError(null);
    try {
      setOrder(await api.orderJuiceFavorite(id));
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  async function remove(id: number) {
    if (!confirm('Retirer ce jus de vos favoris ?')) return;
    try {
      await api.deleteJuiceFavorite(id);
      load();
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  return (
    <div className="juice-favs">
      <div className="juice-favs__head">
        <h3>Mes jus favoris</h3>
        <Link to="/jus" className="link">Composer un jus</Link>
      </div>
      {favorites && favorites.length === 0 && (
        <p className="mycard__tip">Composez votre jus et enregistrez-le en favori : il vous attendra ici.</p>
      )}
      <ul>
        {favorites?.map((f) => (
          <li key={f.id} className={f.recipe.available ? '' : 'muted'}>
            <span className="juice-favs__dots" aria-hidden>
              {f.recipe.ingredients.map((i) => <IngredientThumb key={i.code} item={i} size={28} />)}
            </span>
            <span className="juice-favs__text">
              <b>{f.recipe.name}</b>
              <small>{f.recipe.label} · {f.recipe.sizeLabel} · {f.recipe.price} DH</small>
              {!f.recipe.available && <small>Un ingrédient n’est pas disponible en ce moment.</small>}
            </span>
            <button className="btn btn--accent btn--sm" disabled={!f.recipe.available} onClick={() => reorder(f.id)}>Commander</button>
            <button className="link" onClick={() => remove(f.id)} aria-label={`Retirer ${f.recipe.name}`}>Retirer</button>
          </li>
        ))}
      </ul>
      {error && <p className="form-error">{error}</p>}
      {order && <JuiceTicket order={order} />}
    </div>
  );
}
