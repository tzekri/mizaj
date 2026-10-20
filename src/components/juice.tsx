// Composeur de jus : aperçu du verre (couches aux couleurs des fruits) et ticket à présenter en caisse.

import { useEffect, useId, useState } from 'react';
import QRCode from 'qrcode';
import { fmtDateTime, type JuiceIngredient, type JuiceOrder } from '../api';

/**
 * Verre stylisé : la base occupe la moitié du jus, les fruits se partagent le reste ;
 * les extras apparaissent en garniture (feuilles, zeste…) au-dessus.
 */
export function JuiceGlass({ items, size = 'S33', label }: { items: JuiceIngredient[]; size?: 'S33' | 'S50'; label?: string }) {
  const base = items.find((i) => i.category === 'BASE');
  const fruits = items.filter((i) => i.category === 'FRUIT');
  const extras = items.filter((i) => i.category === 'EXTRA');
  const top = size === 'S50' ? 46 : 70; // niveau du jus (plus haut dans le grand format)
  const bottom = 268;
  const height = bottom - top;
  const layers: { color: string; h: number }[] = [];
  if (base) {
    const share = fruits.length ? 0.5 : 1;
    layers.push({ color: base.color, h: height * share });
    fruits.forEach((f) => layers.push({ color: f.color, h: (height * 0.5) / fruits.length }));
  }
  let y = bottom;
  const id = `glass-${useId().replace(/:/g, '')}`;
  return (
    <svg className="juice-glass" viewBox="0 0 220 300" role="img" aria-label={label ? `Aperçu : ${label}` : 'Verre vide : choisissez une base'}>
      <defs>
        <clipPath id={id}>
          <path d="M40 24 L180 24 L164 272 Q162 286 148 286 L72 286 Q58 286 56 272 Z" />
        </clipPath>
        <linearGradient id={`${id}-shine`} x1="0" x2="1">
          <stop offset="0" stopColor="#fff" stopOpacity="0.35" />
          <stop offset="0.25" stopColor="#fff" stopOpacity="0" />
          <stop offset="0.8" stopColor="#fff" stopOpacity="0" />
          <stop offset="1" stopColor="#fff" stopOpacity="0.18" />
        </linearGradient>
      </defs>
      <g clipPath={`url(#${id})`}>
        <rect x="0" y="0" width="220" height="300" fill="#f7f2ea" />
        {layers.map((l, i) => {
          y -= l.h;
          return <rect key={i} className="juice-glass__layer" x="0" y={y} width="220" height={l.h + 1} fill={l.color} />;
        })}
        {layers.length > 0 && <ellipse cx="110" cy={top} rx="80" ry="6" fill="#fff" opacity="0.35" />}
        <rect x="0" y="0" width="220" height="300" fill={`url(#${id}-shine)`} />
      </g>
      <path d="M40 24 L180 24 L164 272 Q162 286 148 286 L72 286 Q58 286 56 272 Z" fill="none" stroke="#c9bfb1" strokeWidth="3" />
      {/* Paille */}
      <path d="M128 8 L150 4 L136 150" fill="none" stroke="#081330" strokeWidth="6" strokeLinecap="round" opacity="0.85" />
      {/* Garnitures (extras) */}
      {extras.map((e, i) => (
        <g key={e.code} transform={`translate(${62 + i * 34} ${top - 14})`}>
          <path d="M0 10 Q10 -6 22 4 Q12 18 0 10 Z" fill={e.color} stroke="#fff" strokeWidth="1.5" />
        </g>
      ))}
    </svg>
  );
}

/** Vignette ronde d'un ingrédient : sa photo, ou une pastille de sa couleur à défaut. */
export function IngredientThumb({ item, size = 40 }: { item: JuiceIngredient; size?: number }) {
  const [broken, setBroken] = useState(false);
  const style = { width: size, height: size, background: item.color };
  return item.image && !broken ? (
    <img className="thumb" src={item.image} alt="" style={style} loading="lazy" onError={() => setBroken(true)} />
  ) : (
    <i className="thumb" style={style} aria-hidden />
  );
}

const STATUS: Record<JuiceOrder['status'], string> = {
  PENDING: 'À présenter en caisse',
  SERVED: 'Servi',
  EXPIRED: 'Code expiré',
};

/** Ticket du jus : code lisible + QR, à montrer au barista. */
export function JuiceTicket({ order }: { order: JuiceOrder }) {
  const [qr, setQr] = useState('');
  useEffect(() => {
    QRCode.toDataURL(order.qr, { width: 360, margin: 2, errorCorrectionLevel: 'M', color: { dark: '#0a1628', light: '#ffffff' } })
      .then(setQr)
      .catch(() => setQr(''));
  }, [order.qr]);
  return (
    <div className={`juice-ticket juice-ticket--${order.status.toLowerCase()}`}>
      <div className="juice-ticket__head">
        <strong>{order.recipe.name || 'Mon jus Marokoffy'}</strong>
        <span className={`badge ${order.status === 'PENDING' ? 'badge--active' : 'badge--empty'}`}>{STATUS[order.status]}</span>
      </div>
      <div className="juice-ticket__body">
        {qr && <img className="juice-ticket__qr" src={qr} alt={`QR du jus ${order.code}`} />}
        <div>
          <span className="juice-ticket__label">Code jus</span>
          <span className="juice-ticket__code">{order.code.slice(0, 3)} {order.code.slice(3)}</span>
          <span className="juice-ticket__thumbs" aria-hidden>
            {order.recipe.ingredients.map((i) => <IngredientThumb key={i.code} item={i} size={30} />)}
          </span>
          <p>{order.recipe.label}</p>
          <p>
            {order.recipe.sizeLabel} · <b>{order.recipe.price} DH</b> à régler en caisse
          </p>
          {order.status === 'PENDING' && <small>Valable jusqu’au {fmtDateTime(order.expiresAt)}</small>}
        </div>
      </div>
    </div>
  );
}
