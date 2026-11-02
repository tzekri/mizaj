import { useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';
import { api, type Ad, type ChronoInfo } from '../api';
import { fmtChrono, syncClock, useScreen, verdict } from '../chronoLive';
import AdSlide from '../components/AdSlide';
import LedDisplay, { ledText } from '../components/LedDisplay';

const SCREEN_KEY = 'kidima.screen';
/** Durée d'affichage d'un résultat avant de revenir à l'invitation à jouer. */
const RESULT_MS = 9_000;
/** Écran au repos : le jeu reste affiché GAME_MS, puis une pub partenaire AD_MS, chacune son tour. */
const GAME_MS = 20_000;
const AD_MS = 10_000;
/** Les pubs sont rechargées toutes les 5 minutes (nouveau partenaire, fin de diffusion). */
const ADS_REFRESH_MS = 5 * 60_000;

/** Coffee shop de cette TV (?boutique=…) : choisit ses pubs et range ses affichages. */
const shopOf = () => new URLSearchParams(window.location.search).get('boutique')?.trim() || null;

/** Identifiant de cet écran, gardé dans le navigateur de la TV (le QR code reste le même d'un jour à l'autre). */
function screenId(): string {
  const fromUrl = new URLSearchParams(window.location.search).get('id');
  if (fromUrl && /^[A-Za-z0-9]{6,16}$/.test(fromUrl)) return fromUrl;
  try {
    let id = localStorage.getItem(SCREEN_KEY);
    if (!id) {
      const alphabet = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
      id = Array.from(crypto.getRandomValues(new Uint8Array(8)), (b) => alphabet[b % alphabet.length]).join('');
      localStorage.setItem(SCREEN_KEY, id);
    }
    return id;
  } catch {
    return 'KIDIMATV1';
  }
}

/** Écran géant du défi « 10 secondes » (TV du coffee shop) : le chrono en grand, les clients jouent avec leur téléphone. */
export default function ChronoScreen() {
  const [screen] = useState(screenId);
  const { state, connected } = useScreen(screen);
  const [info, setInfo] = useState<ChronoInfo | null>(null);
  const [qr, setQr] = useState<string | null>(null);
  const [display, setDisplay] = useState(0);
  const [showResult, setShowResult] = useState(false);
  const [shop] = useState(shopOf);
  const [ads, setAds] = useState<Ad[]>([]);
  const [ad, setAd] = useState<Ad | null>(null);
  const nextAd = useRef(0);
  const offset = useRef(0);
  const frame = useRef(0);
  const joinUrl = `${window.location.origin}/defi?ecran=${screen}`;

  useEffect(() => {
    const loadInfo = () => api.chronoInfo('screen').then(setInfo).catch(() => {});
    loadInfo();
    const sync = () => syncClock().then((s) => (offset.current = s.offsetMs)).catch(() => {});
    sync();
    const t = setInterval(() => {
      sync();
      loadInfo();
    }, 60_000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    const load = () =>
      api.ads(shop).then((list) =>
        // Même liste qu'avant : on garde l'objet pour ne pas relancer la rotation
        setAds((old) => (JSON.stringify(old) === JSON.stringify(list) ? old : list)),
      ).catch(() => {});
    load();
    const t = setInterval(load, ADS_REFRESH_MS);
    return () => clearInterval(t);
  }, [shop]);

  useEffect(() => {
    QRCode.toDataURL(joinUrl, { width: 520, margin: 1, errorCorrectionLevel: 'M' }).then(setQr).catch(() => {});
  }, [joinUrl]);

  // Chrono en direct, calé sur l'heure du serveur : ce que voit le joueur est ce que mesure le serveur
  useEffect(() => {
    cancelAnimationFrame(frame.current);
    if (state?.status === 'RUNNING') {
      const tick = () => {
        setDisplay(Date.now() + offset.current - state.startedAt);
        frame.current = requestAnimationFrame(tick);
      };
      tick();
    } else if (state?.status === 'RESULT' && state.elapsedMs != null) {
      // Résultat affiché quelques secondes après la fin de la partie (pas un vieux résultat à l'ouverture de la TV)
      const left = RESULT_MS - (state.serverNow - state.changedAt);
      setDisplay(state.elapsedMs);
      setShowResult(left > 0);
      if (left <= 0) return;
      const t = setTimeout(() => setShowResult(false), left);
      return () => clearTimeout(t);
    } else {
      setDisplay(0);
    }
    return () => cancelAnimationFrame(frame.current);
  }, [state?.status, state?.startedAt, state?.elapsedMs]);

  const running = state?.status === 'RUNNING';
  const result = state?.status === 'RESULT' && showResult;
  const win = result && state?.win;
  const idle = !running && !result;

  // Rotation des pubs, seulement quand personne ne joue : une partie qui démarre reprend l'écran tout de suite
  useEffect(() => {
    if (!idle || ads.length === 0) {
      setAd(null);
      return;
    }
    let timer = 0;
    const showGame = () => {
      setAd(null);
      timer = window.setTimeout(showAd, GAME_MS);
    };
    const showAd = () => {
      const next = ads[nextAd.current++ % ads.length];
      setAd(next);
      api.adShown(next.id, shop, screen).catch(() => {});
      timer = window.setTimeout(showGame, AD_MS);
    };
    showGame();
    return () => clearTimeout(timer);
  }, [idle, ads, shop, screen]);

  return (
    <div className={`tv ${win ? 'tv--win' : ''}`}>
      <img className="tv__logo" src="/brand/logo.png" alt="Kidima Coffee House" />
      {ad ? (
        <div className="tv__ad" key={ad.id}>
          <AdSlide ad={ad} />
        </div>
      ) : (
        <div className="tv__main">
          <p className="tv__title">Le défi 10 secondes</p>
          {/* Au repos : l'objectif « 10.00 » en contour ; pendant la partie et le résultat : le chrono plein */}
          <div className="tv__led">
            <LedDisplay text={running || result ? ledText(display) : '10.00'} dim={!running && !result} blink={!!win} label={`${fmtChrono(display)} secondes`} />
          </div>
          <p className="tv__status">
            {running
              ? 'Arrêtez pile sur 10.00 !'
              : win
                ? `GAGNÉ ! ${state?.prize ?? ''} 🎉`
                : result && state?.elapsedMs != null
                  ? <>{verdict(state.elapsedMs)}<br />À qui le tour ?</>
                  : info && !info.enabled
                    ? 'Le défi est en pause'
                    : `Arrêtez le chrono pile sur 10.00 et gagnez ${info?.prize?.toLowerCase() ?? 'un cadeau'}`}
          </p>
        </div>
      )}
      <aside className="tv__join">
        <div className="tv__qr">{qr && <img src={qr} alt="QR code pour jouer" />}</div>
        <p className="tv__join-title">{ad ? 'Défi 10 secondes : scannez pour jouer' : 'Scannez pour jouer'}</p>
        <p className="tv__join-sub">
          Votre téléphone devient la manette
          {info && info.attemptsPerDay > 0 ? ` · ${info.attemptsPerDay} essai${info.attemptsPerDay > 1 ? 's' : ''} par jour` : ''}
        </p>
        {!connected && <p className="tv__offline">Connexion au serveur…</p>}
      </aside>
      <button className="tv__fullscreen" onClick={() => document.documentElement.requestFullscreen?.().catch(() => {})} aria-label="Plein écran">⛶</button>
      {win && <div className="tv__confetti" aria-hidden>{Array.from({ length: 40 }, (_, i) => <i key={i} style={{ left: `${(i * 37) % 100}%`, animationDelay: `${(i % 10) * 0.18}s` }} />)}</div>}
    </div>
  );
}
