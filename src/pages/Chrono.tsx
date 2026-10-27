import { useCallback, useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';
import { ApiError, api, type ChronoInfo, type ChronoPrize } from '../api';
import { fmtChrono as fmt, syncClock, useScreen, verdict } from '../chronoLive';
import { PageHero } from '../components/Layout';
import { img } from '../config';

const DEVICE_KEY = 'kidima.device';
const PRIZE_KEY = 'kidima.prize';

/** Identifiant aléatoire de l'appareil (3 essais par jour et par appareil). */
function deviceId(): string {
  try {
    let id = localStorage.getItem(DEVICE_KEY);
    if (!id) {
      id = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
      localStorage.setItem(DEVICE_KEY, id);
    }
    return id;
  } catch {
    return `nav-${Date.now()}`;
  }
}

/** Écran géant rejoint en scannant le QR code de la TV (?ecran=…) : le téléphone devient la manette. */
function screenParam(): string | null {
  const s = new URLSearchParams(window.location.search).get('ecran');
  return s && /^[A-Za-z0-9]{6,16}$/.test(s) ? s : null;
}

type Phase = 'idle' | 'starting' | 'running' | 'stopping';

export default function Chrono() {
  const [screen] = useState(screenParam);
  const live = useScreen(screen);
  const [info, setInfo] = useState<ChronoInfo | null>(null);
  const [phase, setPhase] = useState<Phase>('idle');
  const [display, setDisplay] = useState(0);
  const [last, setLast] = useState<{ ms: number; win: boolean } | null>(null);
  const [prize, setPrize] = useState<ChronoPrize | null>(() => {
    try {
      const p = JSON.parse(localStorage.getItem(PRIZE_KEY) ?? 'null') as ChronoPrize | null;
      return p && new Date(p.expiresAt) > new Date() ? p : null;
    } catch {
      return null;
    }
  });
  const [error, setError] = useState<string | null>(null);
  const t0 = useRef(0);
  const token = useRef('');
  const frame = useRef(0);
  const latency = useRef(0);

  useEffect(() => {
    api.chronoInfo(deviceId()).then(setInfo).catch((e) => setError((e as ApiError).message));
    // Manette : on mesure le trajet réseau du téléphone, déduit côté serveur à l'arrêt, et on amène le bouton à l'écran
    if (screen) {
      syncClock().then((s) => (latency.current = s.latencyMs)).catch(() => {});
      setTimeout(() => document.querySelector('.chrono__game')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 300);
    }
  }, [screen]);

  const tick = useCallback(() => {
    setDisplay(performance.now() - t0.current);
    frame.current = requestAnimationFrame(tick);
  }, []);
  useEffect(() => () => cancelAnimationFrame(frame.current), []);

  async function start() {
    setError(null);
    setLast(null);
    setPhase('starting');
    try {
      const s = await api.chronoStart(deviceId(), screen ?? undefined);
      token.current = s.token;
      setInfo((i) => (i ? { ...i, remaining: s.remaining } : i));
      // Le chrono part après la réponse du serveur : le temps mesuré ne dépasse jamais le temps réel côté serveur
      t0.current = performance.now();
      setDisplay(0);
      setPhase('running');
      if (!screen) frame.current = requestAnimationFrame(tick);
    } catch (e) {
      setPhase('idle');
      setError((e as ApiError).message);
    }
  }

  async function stop() {
    const ms = Math.round(performance.now() - t0.current);
    cancelAnimationFrame(frame.current);
    setDisplay(ms);
    setPhase('stopping');
    try {
      const r = await api.chronoStop(token.current, ms, screen ? latency.current : undefined);
      setLast({ ms: r.elapsedMs, win: r.win });
      setDisplay(r.elapsedMs);
      setInfo((i) => (i ? { ...i, remaining: r.remaining } : i));
      if (r.prize) {
        setPrize(r.prize);
        try {
          localStorage.setItem(PRIZE_KEY, JSON.stringify(r.prize));
        } catch {
          /* navigation privée : le bon reste affiché tant que la page est ouverte */
        }
      }
    } catch (e) {
      setError((e as ApiError).message);
    } finally {
      setPhase('idle');
    }
  }

  const running = phase === 'running';
  const busy = phase === 'starting' || phase === 'stopping';
  const unlimited = info?.attemptsPerDay === 0;
  const out = info !== null && !unlimited && info.remaining <= 0 && !running;
  // Sur écran géant : quelqu'un d'autre joue (l'écran n'est pas libre et ce n'est pas notre partie)
  const screenTaken = !!screen && live.state?.status === 'RUNNING' && !running && !busy;

  // Barre d'espace / Entrée : démarrer et arrêter sans viser le bouton
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.code !== 'Space' && e.code !== 'Enter') return;
      if ((e.target as HTMLElement)?.tagName === 'INPUT') return;
      e.preventDefault();
      if (running) stop();
      else if (!busy && !out && !screenTaken && info?.enabled) start();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  return (
    <>
      <PageHero
        eyebrow={screen ? 'Écran géant' : 'Jeu'}
        title="Le défi 10 secondes"
        subtitle={
          screen
            ? 'Votre téléphone est la manette : regardez le chrono sur l’écran et arrêtez-le pile sur 10.00 s.'
            : `Arrêtez le chrono pile sur 10.00 s et gagnez : ${info?.prize ?? 'un cadeau'}.`
        }
        photo={img('1495474472287-4d71bcdd2085', 600)}
      />
      <section className="section container chrono">
        <div className="chrono__game">
          {screen && running ? (
            <div className="chrono__screen chrono__screen--look" aria-live="polite">
              <span className="chrono__look">👀</span>
              <span className="chrono__unit">Regardez l’écran !</span>
            </div>
          ) : (
            <div className={`chrono__screen ${last?.win ? 'chrono__screen--win' : ''}`} aria-live="polite">
              <span className="chrono__digits">{fmt(display)}</span>
              <span className="chrono__unit">secondes</span>
            </div>
          )}

          {info && !info.enabled ? (
            <p className="notice">Le défi est en pause pour le moment. Revenez bientôt !</p>
          ) : (
            <button
              className={`chrono__btn ${running ? 'chrono__btn--stop' : ''}`}
              onClick={running ? stop : start}
              disabled={busy || (out && !running) || !info || screenTaken}
            >
              {running ? 'Arrêter' : phase === 'starting' ? '…' : screenTaken ? 'Patientez' : 'Démarrer'}
            </button>
          )}

          {screen && !running && (
            <p className="chrono__live">
              {!live.connected ? 'Connexion à l’écran…' : screenTaken ? 'Un joueur est en cours sur l’écran, c’est bientôt à vous…' : 'Écran libre : à vous de jouer !'}
            </p>
          )}
          {info && info.enabled && unlimited && <p className="chrono__tries">Essais illimités</p>}
          {info && info.enabled && !unlimited && (
            <p className="chrono__tries">
              {Array.from({ length: info.attemptsPerDay }, (_, k) => (
                <i key={k} className={k < info.remaining ? 'on' : ''} />
              ))}
              {out ? 'Plus d’essai aujourd’hui : revenez demain !' : `${info.remaining} essai${info.remaining > 1 ? 's' : ''} restant${info.remaining > 1 ? 's' : ''} aujourd’hui`}
            </p>
          )}
          {last && !last.win && <p className="chrono__verdict">{fmt(last.ms)} s — {verdict(last.ms)}</p>}
          {last?.win && <p className="chrono__verdict chrono__verdict--win">10.00 s pile ! Bravo, vous avez gagné 🎉</p>}
          {error && <p className="form-error" role="alert">{error}</p>}
        </div>

        <div className="chrono__side">
          {prize ? (
            <PrizeTicket prize={prize} />
          ) : (
            <div className="chrono__rules">
              <h2>Les règles</h2>
              <ol>
                <li>Appuyez sur <b>Démarrer</b> : le chrono se lance{screen ? ' sur l’écran' : ''}.</li>
                <li>Appuyez sur <b>Arrêter</b> quand {screen ? 'l’écran affiche' : 'vous pensez être à'} 10 secondes{screen ? '' : ' (barre d’espace possible sur ordinateur)'}.</li>
                <li>Pile <b>10.00 s</b> ? Vous gagnez <b>{info?.prize ?? 'un cadeau'}</b>, à montrer en caisse dans nos coffee shops.</li>
              </ol>
              <p className="sub-fine">
                {info
                  ? `${unlimited ? 'Essais illimités' : `${info.attemptsPerDay} essais par jour et par téléphone`}. Bon valable ${info.validityDays} jours, une seule fois.`
                  : ''}
              </p>
            </div>
          )}
        </div>
      </section>
    </>
  );
}

/** Bon gagnant : QR code à faire scanner en caisse. */
function PrizeTicket({ prize }: { prize: ChronoPrize }) {
  const [qr, setQr] = useState<string | null>(null);
  useEffect(() => {
    QRCode.toDataURL(prize.qr, { width: 420, margin: 2, errorCorrectionLevel: 'M' }).then(setQr).catch(() => {});
  }, [prize.qr]);
  const until = new Date(prize.expiresAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', timeZone: 'Africa/Casablanca' });
  return (
    <div className="prize-ticket">
      <p className="k-eyebrow">Bon gagnant</p>
      <h2>{prize.label}</h2>
      <div className="prize-ticket__qr">{qr ? <img src={qr} alt={`QR code du bon ${prize.code}`} /> : <div className="qr-placeholder" />}</div>
      <p className="prize-ticket__code">{prize.code.slice(0, 3)} {prize.code.slice(3)}</p>
      <p className="sub-fine">Montrez ce bon en caisse avant le {until}. Valable une seule fois.</p>
    </div>
  );
}
