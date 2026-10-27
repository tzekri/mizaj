/**
 * Afficheur à LED « 7 segments », comme un chronomètre de compétition : chiffres rouges sur panneau noir,
 * segments éteints visibles en fond.
 */

// Segments : a (haut), b (haut droit), c (bas droit), d (bas), e (bas gauche), f (haut gauche), g (milieu)
const DIGITS: Record<string, string> = {
  '0': 'abcdef',
  '1': 'bc',
  '2': 'abged',
  '3': 'abgcd',
  '4': 'fgbc',
  '5': 'afgcd',
  '6': 'afgedc',
  '7': 'abc',
  '8': 'abcdefg',
  '9': 'abcdfg',
  '-': 'g',
  ' ': '',
};

const H = (y: number) => `10,${y} 15,${y - 5} 45,${y - 5} 50,${y} 45,${y + 5} 15,${y + 5}`;
const V = (x: number, y1: number, y2: number) => `${x},${y1} ${x + 5},${y1 + 5} ${x + 5},${y2 - 5} ${x},${y2} ${x - 5},${y2 - 5} ${x - 5},${y1 + 5}`;
const SEGMENTS: [string, string][] = [
  ['a', H(6)],
  ['b', V(54, 8, 48)],
  ['c', V(54, 52, 92)],
  ['d', H(94)],
  ['e', V(6, 52, 92)],
  ['f', V(6, 8, 48)],
  ['g', H(50)],
];

function Digit({ char }: { char: string }) {
  const on = DIGITS[char] ?? '';
  return (
    <svg className="led__digit" viewBox="-2 0 64 100" aria-hidden>
      {SEGMENTS.map(([name, points]) => (
        <polygon key={name} points={points} className={on.includes(name) ? 'led__seg led__seg--on' : 'led__seg'} />
      ))}
    </svg>
  );
}

function Dot() {
  return (
    <svg className="led__dot" viewBox="0 0 16 100" aria-hidden>
      <circle cx="8" cy="92" r="6" className="led__seg led__seg--on" />
    </svg>
  );
}

/** Temps en millisecondes affiché « SS.CC » (centièmes tronqués, comme le serveur) : 9 870 ms → « 09.87 ». */
export function ledText(ms: number) {
  return (Math.floor(Math.max(0, Math.min(ms, 99_999)) / 10) / 100).toFixed(2).padStart(5, '0');
}

/**
 * dim : affichage au repos (segments allumés à mi-intensité) ; blink : clignotement (victoire).
 * text : chiffres et point à afficher, ex. « 10.00 ».
 */
export default function LedDisplay({ text, dim = false, blink = false, label }: { text: string; dim?: boolean; blink?: boolean; label?: string }) {
  return (
    <div className={`led ${dim ? 'led--dim' : ''} ${blink ? 'led--blink' : ''}`} role="timer" aria-label={label ?? `${text} secondes`}>
      <div className="led__digits">
        {[...text].map((c, i) => (c === '.' ? <Dot key={i} /> : <Digit key={i} char={c} />))}
      </div>
    </div>
  );
}
