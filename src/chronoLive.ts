import { useEffect, useState } from 'react';
import { api } from './api';

/** État d'un écran géant du défi, diffusé en direct par le serveur. */
export type ScreenState = {
  status: 'IDLE' | 'RUNNING' | 'RESULT';
  /** Heure serveur du départ (ms) */
  startedAt: number;
  elapsedMs?: number;
  win: boolean;
  prize?: string;
  /** Heure serveur du dernier changement (ms) */
  changedAt: number;
  serverNow: number;
};

/**
 * Cale l'horloge sur celle du serveur : plusieurs aller-retours, on garde le plus rapide.
 * offsetMs : à ajouter à Date.now() pour obtenir l'heure serveur ; latencyMs : trajet réseau (moitié de l'aller-retour).
 */
export async function syncClock(samples = 5): Promise<{ offsetMs: number; latencyMs: number }> {
  let best = { rtt: Infinity, offsetMs: 0 };
  for (let i = 0; i < samples; i++) {
    const t0 = performance.now();
    const { serverTime } = await api.chronoPing();
    const rtt = performance.now() - t0;
    if (rtt < best.rtt) best = { rtt, offsetMs: serverTime + rtt / 2 - Date.now() };
  }
  return { offsetMs: best.offsetMs, latencyMs: Math.round(best.rtt / 2) };
}

/** Abonnement en direct à un écran (reconnexion automatique du navigateur en cas de coupure). */
export function useScreen(screen: string | null) {
  const [state, setState] = useState<ScreenState | null>(null);
  const [connected, setConnected] = useState(false);
  useEffect(() => {
    if (!screen) return;
    const source = new EventSource(`/api/public/chrono/screens/${encodeURIComponent(screen)}/events`);
    source.addEventListener('state', (e) => {
      setConnected(true);
      setState(JSON.parse((e as MessageEvent).data) as ScreenState);
    });
    source.onerror = () => setConnected(false);
    return () => source.close();
  }, [screen]);
  return { state, connected };
}

/** Centièmes tronqués : 9 996 ms → « 9.99 », 10 004 ms → « 10.00 » (comme le serveur). */
export const fmtChrono = (ms: number) => (Math.floor(Math.max(0, ms) / 10) / 100).toFixed(2);

export function verdict(ms: number) {
  const diff = Math.abs(ms - 10_000) / 1000;
  if (diff < 0.05) return 'Si près ! À un cheveu…';
  if (diff < 0.2) return 'Très proche, encore un effort !';
  if (diff < 0.6) return 'Pas mal, concentrez-vous.';
  return ms < 10_000 ? 'Trop tôt !' : 'Trop tard !';
}
