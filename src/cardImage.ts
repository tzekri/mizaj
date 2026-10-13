// Image de la carte d'abonné (PNG) générée dans le navigateur, pour l'envoyer sur WhatsApp ou l'enregistrer.

import QRCode from 'qrcode';
import { BRAND } from './config';

const W = 1080;
const H = 1350;

export async function renderCardImage(opts: { qrToken: string; firstName?: string; planName: string }): Promise<Blob> {
  await document.fonts?.ready;
  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d')!;

  const bg = ctx.createLinearGradient(0, 0, W, H);
  bg.addColorStop(0, '#8f2433');
  bg.addColorStop(0.45, '#7b1e2b');
  bg.addColorStop(1, '#4a1019');
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, W, H);

  ctx.fillStyle = '#fff';
  ctx.textAlign = 'center';
  ctx.font = '700 150px "Aref Ruqaa", serif';
  ctx.fillText(BRAND.arabic, W / 2, 190);
  ctx.font = '600 34px Poppins, sans-serif';
  ctx.globalAlpha = 0.8;
  ctx.fillText('MIZAJ COFFEE · CARTE ABONNÉ', W / 2, 300);
  ctx.globalAlpha = 1;

  // QR sur fond blanc arrondi
  const box = 700;
  const x = (W - box) / 2;
  const y = 360;
  ctx.fillStyle = '#fff';
  roundRect(ctx, x, y, box, box, 48);
  ctx.fill();
  const qr = new Image();
  qr.src = await QRCode.toDataURL(`MIZAJ:${opts.qrToken}`, { width: box - 80, margin: 2, errorCorrectionLevel: 'M', color: { dark: '#14100d', light: '#ffffff' } });
  await qr.decode();
  ctx.drawImage(qr, x + 40, y + 40, box - 80, box - 80);

  ctx.fillStyle = '#fff';
  ctx.font = '700 64px Poppins, sans-serif';
  ctx.fillText(opts.firstName || 'Abonné Mizaj', W / 2, 1165);
  ctx.font = '500 40px Poppins, sans-serif';
  ctx.globalAlpha = 0.85;
  ctx.fillText(`Formule ${opts.planName} · à présenter en caisse`, W / 2, 1235);
  ctx.globalAlpha = 1;

  return new Promise((resolve, reject) => canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('canvas'))), 'image/png'));
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

/**
 * Partage la carte : feuille de partage du téléphone (WhatsApp, enregistrer dans Photos…).
 * Sans partage de fichiers (ordinateur, vieux navigateurs) : téléchargement de l'image.
 * @returns 'shared' | 'downloaded' | 'cancelled'
 */
export async function shareCard(blob: Blob, text: string): Promise<'shared' | 'downloaded' | 'cancelled'> {
  const file = new File([blob], 'carte-mizaj.png', { type: 'image/png' });
  if (navigator.canShare?.({ files: [file] })) {
    try {
      await navigator.share({ files: [file], text, title: 'Ma carte Mizaj Coffee' });
      return 'shared';
    } catch (e) {
      if ((e as DOMException).name === 'AbortError') return 'cancelled';
    }
  }
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'carte-mizaj.png';
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
  return 'downloaded';
}
