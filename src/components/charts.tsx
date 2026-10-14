// Graphiques SVG légers du tableau de bord (sans bibliothèque) : colonnes, colonnes empilées, barres, carte de chaleur.
// Règles : marques fines (≤ 24 px, bout arrondi 4 px), grille en filet, une seule échelle, info-bulle au survol,
// textes en encre (jamais dans la couleur de la série), légende dès 2 séries.

import { useEffect, useRef, useState, type ReactNode } from 'react';

export const SERIES = ['var(--series-1)', 'var(--series-2)', 'var(--series-3)'];

const fmt = new Intl.NumberFormat('fr-FR');
export const num = (v: number) => fmt.format(v);

/** Largeur réelle du conteneur (le SVG se dessine à la bonne taille, pas étiré). */
function useWidth<T extends HTMLElement>(): [React.RefObject<T>, number] {
  const ref = useRef<T>(null);
  const [w, setW] = useState(600);
  useEffect(() => {
    if (!ref.current) return;
    const ro = new ResizeObserver(([e]) => setW(Math.max(260, Math.floor(e.contentRect.width))));
    ro.observe(ref.current);
    return () => ro.disconnect();
  }, []);
  return [ref, w];
}

/** Graduations « propres » (0, 5, 10… / 0, 200, 400…). */
function ticks(max: number, count = 4) {
  if (max <= 0) return { top: 1, steps: [0, 1] };
  const raw = max / count;
  const mag = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * mag).find((s) => s >= raw) ?? raw;
  const top = Math.ceil(max / step) * step;
  const steps: number[] = [];
  for (let v = 0; v <= top + 1e-9; v += step) steps.push(Math.round(v * 100) / 100);
  return { top, steps };
}

type Tip = { x: number; y: number; content: ReactNode } | null;

function Tooltip({ tip }: { tip: Tip }) {
  if (!tip) return null;
  return (
    <div className="viz-tip" style={{ left: tip.x, top: tip.y }} role="status">
      {tip.content}
    </div>
  );
}

/** Chemin d'une colonne : bout supérieur arrondi (4 px), base carrée sur la ligne de base. */
function colPath(x: number, y: number, w: number, h: number) {
  if (h <= 0) return '';
  const r = Math.min(4, w / 2, h);
  return `M${x},${y + h}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${y + h}Z`;
}

const PAD = { top: 12, right: 8, bottom: 26, left: 40 };

// --- Colonnes (une ou plusieurs séries empilées) ---

export type ColumnDatum = { label: string; title: string; values: number[] };

export function Columns({
  data,
  series,
  height = 220,
  format = num,
  unit,
}: {
  data: ColumnDatum[];
  /** Nom de chaque série (≥ 2 : empilées + légende). */
  series: string[];
  height?: number;
  format?: (v: number) => string;
  unit?: string;
}) {
  const [ref, width] = useWidth<HTMLDivElement>();
  const [tip, setTip] = useState<Tip>(null);
  const [hover, setHover] = useState<number | null>(null);
  const totals = data.map((d) => d.values.reduce((a, b) => a + b, 0));
  const { top, steps } = ticks(Math.max(0, ...totals));
  const plotW = width - PAD.left - PAD.right;
  const plotH = height - PAD.top - PAD.bottom;
  const band = plotW / Math.max(1, data.length);
  const barW = Math.max(2, Math.min(24, band * 0.62));
  const y = (v: number) => PAD.top + plotH - (v / top) * plotH;
  // Étiquettes d'axe X espacées pour ne jamais se chevaucher
  const every = Math.max(1, Math.ceil(data.length / Math.max(1, Math.floor(plotW / 56))));
  const color = (i: number) => (series.length === 1 ? 'var(--viz-single)' : SERIES[i % SERIES.length]);

  return (
    <div className="viz" ref={ref}>
      {series.length > 1 && <Legend items={series.map((s, i) => ({ label: s, color: color(i) }))} />}
      <svg width={width} height={height} role="img" aria-label={`${series.join(', ')} — graphique en colonnes`}>
        {steps.map((s) => (
          <g key={s}>
            <line x1={PAD.left} x2={width - PAD.right} y1={y(s)} y2={y(s)} className={s === 0 ? 'viz-base' : 'viz-grid'} />
            <text x={PAD.left - 6} y={y(s)} className="viz-tick" textAnchor="end" dominantBaseline="middle">
              {format(s)}
            </text>
          </g>
        ))}
        {data.map((d, i) => {
          const x = PAD.left + i * band + (band - barW) / 2;
          let acc = 0;
          const segs = d.values.map((v, si) => {
            const y0 = y(acc + v);
            const h = y(acc) - y0;
            acc += v;
            // 2 px de séparation (couleur de fond) entre segments empilés
            const gap = si > 0 && h > 2 ? 2 : 0;
            const isTop = si === d.values.length - 1 || d.values.slice(si + 1).every((n) => n === 0);
            return h > 0 ? (
              isTop ? (
                <path key={si} d={colPath(x, y0, barW, h - gap)} fill={color(si)} />
              ) : (
                <rect key={si} x={x} y={y0} width={barW} height={Math.max(0, h - gap)} fill={color(si)} />
              )
            ) : null;
          });
          return (
            <g key={i} opacity={hover === null || hover === i ? 1 : 0.55}>
              {segs}
              {i % every === 0 && (
                <text x={x + barW / 2} y={height - 8} className="viz-tick" textAnchor="middle">
                  {d.label}
                </text>
              )}
              {/* Zone de survol : toute la largeur de la bande */}
              <rect
                x={PAD.left + i * band}
                y={PAD.top}
                width={band}
                height={plotH}
                fill="transparent"
                tabIndex={0}
                aria-label={`${d.title} : ${format(totals[i])}${unit ? ' ' + unit : ''}`}
                onPointerMove={(e) => {
                  setHover(i);
                  const box = ref.current!.getBoundingClientRect();
                  setTip({
                    x: Math.min(e.clientX - box.left + 12, width - 170),
                    y: e.clientY - box.top - 10,
                    content: <TipBody title={d.title} rows={series.map((s, si) => ({ label: s, value: format(d.values[si]), color: color(si) }))} total={series.length > 1 ? format(totals[i]) : undefined} unit={unit} />,
                  });
                }}
                onFocus={() => {
                  setHover(i);
                  setTip({ x: Math.min(x, width - 170), y: PAD.top, content: <TipBody title={d.title} rows={series.map((s, si) => ({ label: s, value: format(d.values[si]), color: color(si) }))} unit={unit} /> });
                }}
                onPointerLeave={() => { setHover(null); setTip(null); }}
                onBlur={() => { setHover(null); setTip(null); }}
              />
            </g>
          );
        })}
      </svg>
      <Tooltip tip={tip} />
    </div>
  );
}

function TipBody({ title, rows, total, unit }: { title: string; rows: { label: string; value: string; color: string }[]; total?: string; unit?: string }) {
  return (
    <>
      <div className="viz-tip__title">{title}</div>
      {rows.map((r) => (
        <div key={r.label} className="viz-tip__row">
          <i style={{ background: r.color }} />
          <b>{r.value}{unit ? ` ${unit}` : ''}</b>
          <span>{r.label}</span>
        </div>
      ))}
      {total && (
        <div className="viz-tip__row viz-tip__total">
          <b>{total}{unit ? ` ${unit}` : ''}</b>
          <span>total</span>
        </div>
      )}
    </>
  );
}

export function Legend({ items }: { items: { label: string; color: string }[] }) {
  return (
    <ul className="viz-legend">
      {items.map((it) => (
        <li key={it.label}>
          <i style={{ background: it.color }} />
          {it.label}
        </li>
      ))}
    </ul>
  );
}

// --- Barres horizontales (classement) ---

export function Bars({ data, format = num, unit }: { data: { label: string; value: number }[]; format?: (v: number) => string; unit?: string }) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <ul className="viz-bars">
      {data.map((d) => (
        <li key={d.label} title={`${d.label} : ${format(d.value)}${unit ? ' ' + unit : ''}`}>
          <span className="viz-bars__label">{d.label}</span>
          <span className="viz-bars__track">
            <span className="viz-bars__fill" style={{ width: `${(d.value / max) * 100}%` }} />
          </span>
          <b className="viz-bars__value">{format(d.value)}</b>
        </li>
      ))}
    </ul>
  );
}

// --- Jauge (taux 0-100 %) ---

export function Meter({ value, label }: { value: number | null | undefined; label: string }) {
  if (value == null) return <span className="muted-text">—</span>;
  return (
    <span className="viz-meter" role="meter" aria-valuenow={value} aria-valuemin={0} aria-valuemax={100} aria-label={label}>
      <span className="viz-meter__track">
        <span className="viz-meter__fill" style={{ width: `${Math.min(100, value)}%` }} />
      </span>
      <b>{num(value)} %</b>
    </span>
  );
}

// --- Carte de chaleur jours × heures (séquentiel : une teinte, du clair au foncé) ---

const RAMP = ['#cde2fb', '#9ec5f4', '#6da7ec', '#3987e5', '#256abf', '#184f95', '#0d366b'];
const DAYS = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
const DAYS_LONG = ['lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi', 'dimanche'];

export function Heatmap({ grid, fromHour = 7, toHour = 22 }: { grid: number[][]; fromHour?: number; toHour?: number }) {
  const [tip, setTip] = useState<{ d: number; h: number } | null>(null);
  const hours = Array.from({ length: toHour - fromHour + 1 }, (_, i) => fromHour + i);
  const max = Math.max(1, ...grid.flat());
  const cell = (v: number) => (v === 0 ? 'var(--viz-empty)' : RAMP[Math.min(RAMP.length - 1, Math.floor((v / max) * (RAMP.length - 0.001)))]);
  return (
    <div className="viz-heat">
      <div className="viz-heat__grid" style={{ gridTemplateColumns: `36px repeat(${hours.length}, minmax(14px, 1fr))` }}>
        <span />
        {hours.map((h) => (
          <span key={h} className="viz-tick viz-heat__hour">{h % 2 === 1 ? '' : `${h}h`}</span>
        ))}
        {DAYS.map((day, d) => (
          <div key={day} className="viz-heat__row">
            <span className="viz-tick">{day}</span>
            {hours.map((h) => (
              <button
                key={h}
                type="button"
                className={`viz-heat__cell ${tip && tip.d === d && tip.h === h ? 'on' : ''}`}
                style={{ background: cell(grid[d][h]) }}
                aria-label={`${DAYS_LONG[d]} ${h}h : ${grid[d][h]} boisson(s)`}
                onPointerEnter={() => setTip({ d, h })}
                onFocus={() => setTip({ d, h })}
                onPointerLeave={() => setTip(null)}
                onBlur={() => setTip(null)}
              />
            ))}
          </div>
        ))}
      </div>
      <div className="viz-heat__foot">
        <span className="viz-heat__readout">
          {tip ? (
            <>
              <b>{grid[tip.d][tip.h]}</b> boisson(s) le {DAYS_LONG[tip.d]} entre {tip.h}h et {tip.h + 1}h
            </>
          ) : (
            'Survolez une case pour voir le détail'
          )}
        </span>
        <span className="viz-heat__scale">
          moins {RAMP.map((c) => <i key={c} style={{ background: c }} />)} plus
        </span>
      </div>
    </div>
  );
}

/** Tableau de données équivalent à un graphique (accessibilité, lecture exacte). */
export function DataTable({ head, rows }: { head: string[]; rows: (string | number)[][] }) {
  return (
    <details className="viz-data">
      <summary>Voir les données</summary>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>{head.map((h) => <th key={h}>{h}</th>)}</tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>{r.map((c, j) => <td key={j} className={typeof c === 'number' ? 'num' : undefined}>{typeof c === 'number' ? num(c) : c}</td>)}</tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  );
}
