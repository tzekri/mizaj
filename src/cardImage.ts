// Image de la carte d'abonné (PNG) générée dans le navigateur, pour l'envoyer sur WhatsApp ou l'enregistrer.

import QRCode from 'qrcode';

const W = 1080;
const H = 1350;

export async function renderCardImage(opts: { qrToken: string; firstName?: string; planName: string }): Promise<Blob> {
  await document.fonts?.ready;
  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d')!;

  // Fond crème du logo, liseré aux couleurs du drapeau en bas
  const bg = ctx.createLinearGradient(0, 0, 0, H);
  bg.addColorStop(0, '#fbf7ef');
  bg.addColorStop(1, '#efe5d2');
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, W, H);
  ctx.fillStyle = '#c1272d';
  ctx.fillRect(0, H - 18, W / 2, 18);
  ctx.fillStyle = '#1d6b35';
  ctx.fillRect(W / 2, H - 18, W / 2, 18);

  // Logo officiel (version claire) en tête de carte
  const logo = new Image();
  logo.src = '/brand/logo-mark.png';
  await logo.decode();
  const lw = 560;
  const lh = (logo.naturalHeight / logo.naturalWidth) * lw;
  ctx.drawImage(logo, (W - lw) / 2, 48, lw, lh);
  ctx.textAlign = 'center';
  ctx.font = '600 32px Poppins, sans-serif';
  ctx.fillStyle = '#9a6a1c';
  ctx.fillText('CARTE ABONNÉ', W / 2, 48 + lh + 52);

  // QR sur fond blanc arrondi
  const box = 680;
  const x = (W - box) / 2;
  const y = 48 + lh + 84;
  ctx.fillStyle = '#fff';
  ctx.shadowColor = 'rgba(18, 39, 90, 0.16)';
  ctx.shadowBlur = 40;
  ctx.shadowOffsetY = 12;
  roundRect(ctx, x, y, box, box, 48);
  ctx.fill();
  ctx.shadowColor = 'transparent';
  const qr = new Image();
  qr.src = await QRCode.toDataURL(`MIZAJE:${opts.qrToken}`, { width: box - 80, margin: 2, errorCorrectionLevel: 'M', color: { dark: '#14100d', light: '#ffffff' } });
  await qr.decode();
  ctx.drawImage(qr, x + 40, y + 40, box - 80, box - 80);

  ctx.fillStyle = '#12275a';
  ctx.font = '700 64px Poppins, sans-serif';
  ctx.fillText(opts.firstName || 'Abonné Kidima', W / 2, y + box + 92);
  ctx.font = '500 38px Poppins, sans-serif';
  ctx.globalAlpha = 0.85;
  ctx.fillText(`Formule ${opts.planName} · à présenter en caisse`, W / 2, y + box + 152);
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
  const file = new File([blob], 'carte-kidima.png', { type: 'image/png' });
  if (navigator.canShare?.({ files: [file] })) {
    try {
      await navigator.share({ files: [file], text, title: 'Ma carte Kidima' });
      return 'shared';
    } catch (e) {
      if ((e as DOMException).name === 'AbortError') return 'cancelled';
    }
  }
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'carte-kidima.png';
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
  return 'downloaded';
}
