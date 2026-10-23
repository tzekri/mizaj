import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import QrScanner from 'qr-scanner';
import {
  ApiError,
  STATUS_LABEL,
  api,
  daysLeft,
  fmtDate,
  fmtDateTime,
  staffSession,
  type StaffCard,
  type StaffMember,
  type StaffRole,
  type StaffSession,
  type Stats,
  type Subscriber,
  type JuiceIngredient,
  type JuiceOrder,
  type JuiceStats,
  type Plan,
  type PlanInput,
  type Today,
} from '../api';

import { SHOPS } from '../config';
import Dashboard from './Dashboard';
import MenuAdmin from './MenuAdmin';
import { IngredientThumb, JuiceGlass } from '../components/juice';

/** Codes de jus ; « MIZAJ-JUS: » : codes générés des premières versions (ancien nom « Mizaj »). */
const JUICE_QR_PREFIXES = ['MIZAJE-JUS:', 'MIZAJ-JUS:'];
import { Header } from '../components/Layout';

export default function Barista() {
  const [session, setSession] = useState<StaffSession | null>(staffSession.get());
  const [tab, setTab] = useState<'counter' | 'dashboard' | 'admin' | 'menu'>('counter');

  const logout = useCallback(() => {
    staffSession.set(null);
    setSession(null);
  }, []);

  return (
    <>
      <section className="app-top on-dark">
        <Header small />
      </section>
      <section className="section container barista">
        {!session ? (
          <StaffLogin onLogin={(s) => { staffSession.set(s); setSession(s); }} />
        ) : (
          <>
            <div className="barista__bar">
              <div>
                <p className="eyebrow">Espace barista</p>
                <h1>{session.name}{session.shop ? ` · ${session.shop}` : ''}</h1>
              </div>
              <div className="barista__bar-actions">
                {session.role === 'ADMIN' && (
                  <div className="seg">
                    <button className={tab === 'counter' ? 'active' : ''} onClick={() => setTab('counter')}>Comptoir</button>
                    <button className={tab === 'dashboard' ? 'active' : ''} onClick={() => setTab('dashboard')}>Tableau de bord</button>
                    <button className={tab === 'menu' ? 'active' : ''} onClick={() => setTab('menu')}>Menu & prix</button>
                    <button className={tab === 'admin' ? 'active' : ''} onClick={() => setTab('admin')}>Administration</button>
                  </div>
                )}
                <button className="btn btn--outline btn--sm" onClick={logout}>Déconnexion</button>
              </div>
            </div>
            {tab === 'counter' && <Counter onAuthError={logout} />}
            {tab === 'dashboard' && <Dashboard onAuthError={logout} />}
            {tab === 'menu' && <MenuAdmin />}
            {tab === 'admin' && <Admin onAuthError={logout} />}
          </>
        )}
      </section>
    </>
  );
}

function StaffLogin({ onLogin }: { onLogin: (s: StaffSession) => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      onLogin(await api.staffLogin(email, password));
    } catch (err) {
      setError((err as ApiError).message);
      setBusy(false);
    }
  }

  return (
    <form className="sub-form staff-login" onSubmit={submit}>
      <h2>Espace barista</h2>
      <p className="sub-form__lead">Connectez-vous pour scanner les cartes des abonnés.</p>
      <label>E-mail<input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
      <label>
        Mot de passe
        <input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      </label>
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="btn btn--accent btn--block" disabled={busy}>{busy ? 'Connexion…' : 'Se connecter'}</button>
    </form>
  );
}

// --- Comptoir ---

type Notice = { kind: 'ok' | 'error'; text: string } | null;

function Counter({ onAuthError }: { onAuthError: () => void }) {
  const [card, setCard] = useState<StaffCard | null>(null);
  const [juice, setJuice] = useState<JuiceOrder | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [busy, setBusy] = useState(false);
  const [today, setToday] = useState<Today | null>(null);
  const [phone, setPhone] = useState('');
  const [plans, setPlans] = useState<Plan[]>([]);
  const [planCode, setPlanCode] = useState('');

  useEffect(() => {
    api.plans().then(setPlans).catch(() => {});
  }, []);
  // Nouvelle carte affichée : on propose la formule choisie par le client.
  useEffect(() => {
    if (card) setPlanCode(card.preferredPlan.code);
  }, [card?.customerId, card?.preferredPlan.code]);

  const fail = useCallback(
    (e: unknown) => {
      const err = e as ApiError;
      if (err.status === 401) onAuthError();
      setNotice({ kind: 'error', text: err.message });
    },
    [onAuthError],
  );

  const refreshToday = useCallback(() => {
    api.today().then(setToday).catch(() => {});
  }, []);
  useEffect(refreshToday, [refreshToday]);

  const onScan = useCallback(
    async (data: string) => {
      setBusy(true);
      setNotice(null);
      try {
        // Le même scanner lit les cartes d'abonné et les codes de jus composés
        if (JUICE_QR_PREFIXES.some((p) => data.toUpperCase().startsWith(p))) {
          setCard(null);
          setJuice(await api.juiceScan(data));
        } else {
          setJuice(null);
          setCard(await api.scan(data));
        }
      } catch (e) {
        fail(e);
      } finally {
        setBusy(false);
      }
    },
    [fail],
  );

  async function lookup(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setNotice(null);
    try {
      // 6 caractères : code de jus ; sinon numéro de téléphone
      const compact = phone.replace(/[\s-]/g, '');
      if (compact.length === 6) {
        setCard(null);
        setJuice(await api.juiceScan(compact));
      } else {
        setJuice(null);
        setCard(await api.lookup(phone));
      }
      setPhone('');
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  }

  /**
   * Exécute une action sur la carte. Si le serveur demande une confirmation (code `confirmCode`),
   * le barista la donne et l'action est rejouée avec `confirmed`.
   */
  async function run(
    action: (confirmed: boolean) => Promise<StaffCard>,
    success: (c: StaffCard) => string,
    confirmCode?: string,
  ) {
    setBusy(true);
    setNotice(null);
    try {
      let next: StaffCard;
      try {
        next = await action(false);
      } catch (e) {
        const err = e as ApiError;
        if (!confirmCode || err.code !== confirmCode || !confirm(err.message)) throw e;
        next = await action(true);
      }
      setCard(next);
      setNotice({ kind: 'ok', text: success(next) });
      refreshToday();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }

  function redeem() {
    if (!card) return;
    run(
      (force) => api.redeem(card.customerId, force),
      (c) => `Validation enregistrée ✓ — reste ${c.drinksLeft} / ${c.drinksTotal} (${c.plan.unitPlural}).`,
      'RECENT',
    );
  }

  function activate() {
    if (!card) return;
    const plan = plans.find((p) => p.code === planCode) ?? card.preferredPlan;
    if (!confirm(`Formule ${plan.name} : ${plan.price} DH encaissés ?\nLe client disposera de ${plan.drinks} ${plan.unitPlural} pendant ${plan.days} jours.`)) return;
    run(
      (replace) => api.activate(card.customerId, plan.code, replace),
      (c) => `Abonnement ${c.plan.name} activé ✓ — ${c.drinksLeft} ${c.plan.unitPlural} jusqu’au ${fmtDate(c.expiresAt)}.`,
      'STILL_ACTIVE',
    );
  }

  function undo() {
    if (!card?.undoableRedemptionId) return;
    if (!confirm('Annuler la dernière validation ? La boisson sera recréditée sur la carte.')) return;
    run(() => api.undo(card.undoableRedemptionId!), (c) => `Validation annulée — reste ${c.drinksLeft} / ${c.drinksTotal} (${c.plan.unitPlural}).`);
  }

  async function serveJuice() {
    if (!juice) return;
    setBusy(true);
    setNotice(null);
    try {
      const served = await api.juiceServe(juice.code);
      setJuice(served);
      setNotice({ kind: 'ok', text: `Jus servi ✓ — ${served.recipe.price} DH encaissés.` });
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  }

  function reset() {
    setCard(null);
    setJuice(null);
    setNotice(null);
  }

  return (
    <div className="counter">
      <div className="counter__left">
        {today && (
          <div className="today">
            <div><strong>{today.redemptions}</strong><span>boissons validées aujourd’hui</span></div>
            <div><strong>{today.activations}</strong><span>abonnements activés</span></div>
          </div>
        )}
        {card || juice ? (
          <button className="btn btn--accent btn--block" onClick={reset}>Scanner un autre client</button>
        ) : (
          <Scanner onResult={onScan} paused={busy} />
        )}
        <form className="lookup" onSubmit={lookup}>
          <input type="tel" inputMode="tel" placeholder="Numéro (06…) ou code jus" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <button className="btn btn--outline btn--sm" disabled={busy || !phone.trim()}>Rechercher</button>
        </form>
      </div>

      <div className="counter__right">
        {notice && <p className={`notice notice--${notice.kind}`} role="status">{notice.text}</p>}
        {juice ? (
          <JuicePanel order={juice} busy={busy} onServe={serveJuice} />
        ) : card ? (
          <CustomerPanel
            card={card}
            busy={busy}
            plans={plans}
            planCode={planCode}
            onPlanChange={setPlanCode}
            onRedeem={redeem}
            onActivate={activate}
            onUndo={undo}
          />
        ) : (
          <div className="counter__empty">
            <h2>Scannez la carte du client</h2>
            <p>Le client présente son QR code « Ma carte » (abonnement) ou le code de son jus composé devant la caméra.</p>
          </div>
        )}
      </div>
    </div>
  );
}

function CustomerPanel({
  card,
  busy,
  plans,
  planCode,
  onPlanChange,
  onRedeem,
  onActivate,
  onUndo,
}: {
  card: StaffCard;
  busy: boolean;
  plans: Plan[];
  planCode: string;
  onPlanChange: (code: string) => void;
  onRedeem: () => void;
  onActivate: () => void;
  onUndo: () => void;
}) {
  const active = card.status === 'ACTIVE';
  const chosen = plans.find((p) => p.code === planCode) ?? card.preferredPlan;
  const { unit, unitPlural } = card.plan;
  return (
    <div className={`customer customer--${card.status.toLowerCase()}`}>
      <div className="customer__head">
        <div>
          <h2>{card.firstName || 'Client sans prénom'}</h2>
          <p>{card.phone} · client depuis le {fmtDate(card.memberSince)}</p>
          <p className="customer__plan">Formule {card.plan.name}</p>
        </div>
        <span className={`badge badge--${card.status.toLowerCase()}`}>{STATUS_LABEL[card.status]}</span>
      </div>

      {active && (
        <div className="customer__count">
          <strong>{card.drinksLeft}</strong>
          <span>sur {card.drinksTotal} {unitPlural}<br />expire le {fmtDate(card.expiresAt)} ({daysLeft(card.expiresAt)} j)</span>
        </div>
      )}
      {card.status === 'EXPIRED' && <p className="customer__msg">Abonnement terminé le {fmtDate(card.expiresAt)}.</p>}
      {card.status === 'EMPTY' && <p className="customer__msg">Toutes les boissons de la formule ({card.drinksTotal} {unitPlural}) ont été servies.</p>}
      {card.status === 'NONE' && <p className="customer__msg">Compte créé, abonnement jamais activé : encaissez la formule choisie puis activez.</p>}
      {card.lastRedemptionAt && <p className="customer__last">Dernière validation : {fmtDateTime(card.lastRedemptionAt)}</p>}

      {plans.length > 1 && (
        <label className="plan-switch">
          Formule à {active ? 'renouveler' : 'activer'}
          <select value={planCode} onChange={(e) => onPlanChange(e.target.value)} disabled={busy}>
            {plans.map((p) => (
              <option key={p.code} value={p.code}>
                {p.name} — {p.price} DH · {p.drinks} {p.unitPlural} · {p.days} j
                {p.code === card.preferredPlan.code ? ' (choix du client)' : ''}
              </option>
            ))}
          </select>
        </label>
      )}

      <div className="customer__actions">
        {active ? (
          <button className="btn btn--accent btn--xl" disabled={busy} onClick={onRedeem}>Valider 1 {unit}</button>
        ) : (
          <button className="btn btn--accent btn--xl" disabled={busy} onClick={onActivate}>
            {card.status === 'NONE' ? 'Activer' : 'Renouveler'} {chosen.name} — {chosen.price} DH encaissés
          </button>
        )}
        {active && (
          <button className="btn btn--outline btn--sm" disabled={busy} onClick={onActivate}>
            Renouveler par anticipation ({chosen.name}, {chosen.price} DH)
          </button>
        )}
        <RecoveryCode customerId={card.customerId} />
        {card.undoableRedemptionId && (
          <button className="link" disabled={busy} onClick={onUndo}>Annuler la dernière validation</button>
        )}
      </div>
    </div>
  );
}

/** Jus composé à préparer : recette, format et prix, puis « Servi ». */
function JuicePanel({ order, busy, onServe }: { order: JuiceOrder; busy: boolean; onServe: () => void }) {
  const r = order.recipe;
  return (
    <div className={`customer juice-panel customer--${order.status === 'PENDING' ? 'active' : 'expired'}`}>
      <div className="customer__head">
        <div>
          <h2>{r.name || 'Jus composé'}</h2>
          <p>Code {order.code.slice(0, 3)} {order.code.slice(3)} · créé le {fmtDateTime(order.createdAt)}</p>
        </div>
        <span className={`badge ${order.status === 'PENDING' ? 'badge--active' : 'badge--empty'}`}>
          {order.status === 'PENDING' ? 'À préparer' : order.status === 'SERVED' ? 'Déjà servi' : 'Code expiré'}
        </span>
      </div>
      <div className="juice-panel__recipe">
        <JuiceGlass items={r.ingredients} size={r.size} label={r.label} />
        <div>
          <p className="juice-panel__size">{r.sizeLabel}</p>
          <ul>
            {r.ingredients.map((i) => (
              <li key={i.code}>
                <IngredientThumb item={i} size={36} />
                <span>{i.name}</span>
                <small>{i.category === 'BASE' ? 'base' : i.category === 'FRUIT' ? 'fruit' : 'extra'}</small>
              </li>
            ))}
          </ul>
          <p className="juice-panel__price">{r.price} DH</p>
        </div>
      </div>
      {order.status === 'SERVED' && <p className="customer__msg">Servi le {fmtDateTime(order.servedAt)}{order.shop ? ` · ${order.shop}` : ''}.</p>}
      {order.status === 'EXPIRED' && <p className="customer__msg">Ce code a expiré : le client peut recréer son jus en un geste.</p>}
      {order.status === 'PENDING' && (
        <div className="customer__actions">
          <button className="btn btn--accent btn--xl" disabled={busy} onClick={onServe}>Servi — {r.price} DH encaissés</button>
        </div>
      )}
    </div>
  );
}

/** Code à lire au client (présent en caisse) pour qu'il ouvre sa carte sur un nouveau téléphone. */
function RecoveryCode({ customerId }: { customerId: number }) {
  const [code, setCode] = useState<{ code: string; expiresAt: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    setCode(null);
    setError(null);
  }, [customerId]);

  async function generate() {
    setError(null);
    try {
      setCode(await api.recoveryCode(customerId));
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  if (code) {
    return (
      <div className="recovery">
        <span>Code de récupération à donner au client :</span>
        <strong>{code.code.replace(/(\d{3})(\d{3})/, '$1 $2')}</strong>
        <small>
          Valable jusqu’à {new Date(code.expiresAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', timeZone: 'Africa/Casablanca' })}.
          Le client le saisit sur la page Abonnement → « J’ai déjà une carte ». Vérifiez qu’il s’agit bien du titulaire.
        </small>
      </div>
    );
  }
  return (
    <>
      <button className="link" onClick={generate}>Nouveau téléphone ? Donner un code de récupération</button>
      {error && <p className="form-error">{error}</p>}
    </>
  );
}

/** Lecture du QR code par la caméra (arrière de préférence). Nécessite HTTPS ou localhost. */
function Scanner({ onResult, paused }: { onResult: (data: string) => void; paused: boolean }) {
  const video = useRef<HTMLVideoElement>(null);
  const scanner = useRef<QrScanner | null>(null);
  const [error, setError] = useState<string | null>(null);
  const handler = useRef(onResult);
  handler.current = onResult;

  useEffect(() => {
    if (!video.current) return;
    let last = '';
    const s = new QrScanner(
      video.current,
      (result) => {
        if (result.data === last) return;
        last = result.data;
        s.stop();
        handler.current(result.data);
      },
      { preferredCamera: 'environment', highlightScanRegion: true, highlightCodeOutline: true, maxScansPerSecond: 8 },
    );
    scanner.current = s;
    s.start().catch(() =>
      setError('Caméra indisponible. Autorisez l’accès à la caméra (le site doit être ouvert en https), ou recherchez le client par numéro.'),
    );
    return () => {
      s.stop();
      s.destroy();
      scanner.current = null;
    };
  }, []);

  useEffect(() => {
    if (!paused && scanner.current && !error) scanner.current.start().catch(() => {});
  }, [paused, error]);

  return (
    <div className="scanner">
      <video ref={video} muted playsInline />
      {error && <p className="scanner__error">{error}</p>}
    </div>
  );
}

// --- Administration ---

function Admin({ onAuthError }: { onAuthError: () => void }) {
  const [stats, setStats] = useState<Stats | null>(null);
  const [subs, setSubs] = useState<Subscriber[]>([]);
  const [staff, setStaff] = useState<StaffMember[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [st, su, sf] = await Promise.all([api.stats(), api.subscribers(), api.staff()]);
      setStats(st);
      setSubs(su);
      setStaff(sf);
    } catch (e) {
      const err = e as ApiError;
      if (err.status === 401) onAuthError();
      setError(err.message);
    }
  }, [onAuthError]);
  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="admin">
      {error && <p className="notice notice--error">{error}</p>}
      {stats && (
        <div className="kpis">
          <Kpi value={stats.activeSubscriptions} label="abonnements actifs" />
          <Kpi value={`${stats.revenueThisMonth} DH`} label="encaissés ce mois-ci" />
          <Kpi value={stats.redemptionsToday} label="boissons validées aujourd’hui" />
          <Kpi value={stats.redemptionsThisMonth} label="boissons validées ce mois-ci" />
          <Kpi value={stats.customers} label="comptes créés" />
        </div>
      )}

      <p className="admin__links">
        <Link to="/affiche" className="btn btn--outline btn--sm">Affiche QR « Abonnement » à imprimer</Link>
      </p>

      <PlansEditor />

      <JuiceAdmin />

      <h2>Abonnés</h2>
      {subs.length === 0 ? (
        <p className="empty">Aucun abonnement activé pour l’instant.</p>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Client</th><th>Téléphone</th><th>Formule</th><th>État</th><th>Restant</th><th>Fin</th></tr>
            </thead>
            <tbody>
              {subs.map((s) => (
                <tr key={s.customerId}>
                  <td>{s.firstName || '—'}</td>
                  <td>{s.phone}</td>
                  <td>{s.planName || '—'}</td>
                  <td><span className={`badge badge--${s.status.toLowerCase()}`}>{STATUS_LABEL[s.status]}</span></td>
                  <td>{s.drinksLeft}</td>
                  <td>{fmtDate(s.expiresAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2>Personnel</h2>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Nom</th><th>E-mail</th><th>Rôle</th><th>Coffee shop</th><th /></tr>
          </thead>
          <tbody>
            {staff.map((m) => (
              <tr key={m.id} className={m.active ? '' : 'muted'}>
                <td>{m.fullName}</td>
                <td>{m.email}</td>
                <td>{m.role === 'ADMIN' ? 'Administrateur' : 'Barista'}</td>
                <td>{m.shop || 'Tous'}</td>
                <td>
                  <button
                    className="link"
                    onClick={() => api.setStaffActive(m.id, !m.active).then(load).catch((e) => setError((e as ApiError).message))}
                  >
                    {m.active ? 'Désactiver' : 'Réactiver'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <NewStaff onCreated={load} />
    </div>
  );
}

const EMPTY_PLAN: PlanInput = {
  name: '',
  description: '',
  price: 100,
  drinks: 20,
  days: 30,
  unit: 'espresso',
  unitPlural: 'espressos',
  active: true,
};

/** Formules d'abonnement : modification (prix, boissons, durée, activation) et création. */
function PlansEditor() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [editing, setEditing] = useState<string | null>(null); // code, ou 'new'
  const [form, setForm] = useState<PlanInput>(EMPTY_PLAN);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api.adminPlans().then(setPlans).catch((e) => setError((e as ApiError).message));
  }, []);
  useEffect(load, [load]);

  function edit(p?: Plan) {
    setError(null);
    setEditing(p ? p.code : 'new');
    setForm(p ? { ...p, description: p.description ?? '' } : EMPTY_PLAN);
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      if (editing === 'new') await api.createPlan(form);
      else if (editing) await api.updatePlan(editing, form);
      setEditing(null);
      load();
    } catch (err) {
      setError((err as ApiError).message);
    }
  }

  const num = (k: 'price' | 'drinks' | 'days') => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [k]: Number(e.target.value) });

  return (
    <>
      <h2>Formules d’abonnement</h2>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Formule</th><th>Prix</th><th>Boissons</th><th>Durée</th><th>État</th><th /></tr>
          </thead>
          <tbody>
            {plans.map((p) => (
              <tr key={p.code} className={p.active ? '' : 'muted'}>
                <td><b>{p.name}</b>{p.description ? <><br /><small>{p.description}</small></> : null}</td>
                <td>{p.price} DH</td>
                <td>{p.drinks} {p.unitPlural}</td>
                <td>{p.days} jours</td>
                <td>{p.active ? 'Proposée' : 'Masquée'}</td>
                <td><button className="link" onClick={() => edit(p)}>Modifier</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editing ? (
        <form className="form new-staff" onSubmit={save}>
          <h3 className="form__full">{editing === 'new' ? 'Nouvelle formule' : `Modifier « ${form.name} »`}</h3>
          <label>Nom<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required maxLength={60} /></label>
          <label>
            Description
            <input value={form.description ?? ''} onChange={(e) => setForm({ ...form, description: e.target.value })} maxLength={160} />
          </label>
          <label>Prix (DH)<input type="number" min={1} value={form.price} onChange={num('price')} required /></label>
          <label>Nombre de boissons<input type="number" min={1} value={form.drinks} onChange={num('drinks')} required /></label>
          <label>Durée (jours)<input type="number" min={1} max={366} value={form.days} onChange={num('days')} required /></label>
          <label>
            Boisson (singulier / pluriel)
            <span className="pair">
              <input value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })} required maxLength={40} />
              <input value={form.unitPlural} onChange={(e) => setForm({ ...form, unitPlural: e.target.value })} required maxLength={40} />
            </span>
          </label>
          <label className="check form__full">
            <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
            Proposée aux clients sur la page Abonnement
          </label>
          <p className="sub-fine form__full">
            Les abonnements déjà activés gardent leurs conditions ; les changements s’appliquent aux prochaines activations.
          </p>
          {error && <p className="form-error form__full">{error}</p>}
          <div className="form__full pair-actions">
            <button className="btn btn--accent">Enregistrer</button>
            <button type="button" className="btn btn--outline" onClick={() => setEditing(null)}>Annuler</button>
          </div>
        </form>
      ) : (
        <p><button className="btn btn--outline btn--sm" onClick={() => edit()}>+ Nouvelle formule</button></p>
      )}
    </>
  );
}

const CATEGORY_LABEL: Record<JuiceIngredient['category'], string> = { BASE: 'Base', FRUIT: 'Fruit', EXTRA: 'Extra' };
const EMPTY_INGREDIENT: Omit<JuiceIngredient, 'code'> = { name: '', category: 'FRUIT', price: 5, color: '#f2a03d', image: '', available: true };
const isoDay = (d: Date) => new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Casablanca' }).format(d);

/** Composeur de jus : ingrédients (prix, couleur, disponibilité) et jus servis sur 30 jours. */
function JuiceAdmin() {
  const [items, setItems] = useState<JuiceIngredient[]>([]);
  const [stats, setStats] = useState<JuiceStats | null>(null);
  const [editing, setEditing] = useState<string | null>(null); // code, ou 'new'
  const [form, setForm] = useState<Omit<JuiceIngredient, 'code'>>(EMPTY_INGREDIENT);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api.juiceIngredients().then(setItems).catch((e) => setError((e as ApiError).message));
    const to = new Date();
    const from = new Date(to.getTime() - 29 * 86_400_000);
    api.juiceStats(isoDay(from), isoDay(to)).then(setStats).catch(() => {});
  }, []);
  useEffect(load, [load]);

  function edit(i?: JuiceIngredient) {
    setError(null);
    setEditing(i ? i.code : 'new');
    setForm(i ? { name: i.name, category: i.category, price: i.price, color: i.color, image: i.image ?? '', available: i.available } : EMPTY_INGREDIENT);
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.saveJuiceIngredient(form, editing === 'new' ? undefined : editing ?? undefined);
      setEditing(null);
      load();
    } catch (err) {
      setError((err as ApiError).message);
    }
  }

  async function toggle(i: JuiceIngredient) {
    try {
      await api.saveJuiceIngredient({ name: i.name, category: i.category, price: i.price, color: i.color, image: i.image ?? '', available: !i.available }, i.code);
      load();
    } catch (err) {
      setError((err as ApiError).message);
    }
  }

  return (
    <>
      <h2>Jus composés</h2>
      {stats && (
        <div className="kpis kpis--3">
          <Kpi value={stats.served} label="jus servis (30 jours)" />
          <Kpi value={`${stats.revenue} DH`} label="encaissés (30 jours)" />
          <Kpi value={stats.created} label="codes créés (30 jours)" />
        </div>
      )}
      {stats && stats.topMixes.length > 0 && (
        <div className="top-mixes">
          <h3>Mix les plus servis</h3>
          <ol>
            {stats.topMixes.map((m) => (
              <li key={m.label + m.sizeLabel}><b>{m.label}</b> <span>{m.sizeLabel} · {m.count} fois</span></li>
            ))}
          </ol>
        </div>
      )}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Ingrédient</th><th>Type</th><th className="num">Supplément (33 cl)</th><th>Disponible</th><th /></tr>
          </thead>
          <tbody>
            {items.map((i) => (
              <tr key={i.code} className={i.available ? '' : 'muted'}>
                <td className="ing-cell"><IngredientThumb item={i} size={30} />{i.name}</td>
                <td>{CATEGORY_LABEL[i.category]}</td>
                <td className="num">{i.price ? `+${i.price} DH` : 'inclus'}</td>
                <td><button className="link" onClick={() => toggle(i)}>{i.available ? 'Oui — masquer' : 'Non — proposer'}</button></td>
                <td><button className="link" onClick={() => edit(i)}>Modifier</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="dash-note">Mode de prix (fixe ou selon les ingrédients) et prix des formats : onglet « Menu & prix ». Les suppléments ci-dessus ne comptent qu’en mode « selon les ingrédients ».</p>
      {editing ? (
        <form className="form new-staff" onSubmit={save}>
          <h3 className="form__full">{editing === 'new' ? 'Nouvel ingrédient' : `Modifier « ${form.name} »`}</h3>
          <label>Nom<input value={form.name} maxLength={40} required onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
          <label>
            Type
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value as JuiceIngredient['category'] })}>
              <option value="BASE">Base</option>
              <option value="FRUIT">Fruit</option>
              <option value="EXTRA">Extra</option>
            </select>
          </label>
          <label>Supplément 33 cl (DH)<input type="number" min={0} max={200} value={form.price} onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} /></label>
          <label>
            Couleur dans le verre
            <input type="color" value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} />
          </label>
          <label className="form__full">
            Photo (adresse https, ex. Unsplash) — laisser vide pour une pastille de couleur
            <span className="pair">
              <input type="url" value={form.image ?? ''} placeholder="https://images.unsplash.com/photo-…" onChange={(e) => setForm({ ...form, image: e.target.value })} />
              <IngredientThumb item={{ code: 'preview', ...form }} size={44} />
            </span>
          </label>
          <label className="check form__full">
            <input type="checkbox" checked={form.available} onChange={(e) => setForm({ ...form, available: e.target.checked })} />
            Proposé dans le composeur
          </label>
          {error && <p className="form-error form__full">{error}</p>}
          <div className="form__full pair-actions">
            <button className="btn btn--accent">Enregistrer</button>
            <button type="button" className="btn btn--outline" onClick={() => setEditing(null)}>Annuler</button>
          </div>
        </form>
      ) : (
        <p><button className="btn btn--outline btn--sm" onClick={() => edit()}>+ Nouvel ingrédient</button></p>
      )}
    </>
  );
}

function Kpi({ value, label }: { value: number | string; label: string }) {
  return (
    <div className="kpi">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function NewStaff({ onCreated }: { onCreated: () => void }) {
  const [form, setForm] = useState({ fullName: '', email: '', password: '', role: 'BARISTA' as StaffRole, shop: SHOPS[0].name });
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setDone(null);
    try {
      const m = await api.createStaff({ ...form, shop: form.shop || undefined });
      setDone(`Compte créé pour ${m.fullName}.`);
      setForm({ ...form, fullName: '', email: '', password: '' });
      onCreated();
    } catch (err) {
      setError((err as ApiError).message);
    }
  }

  return (
    <form className="form new-staff" onSubmit={submit}>
      <h3 className="form__full">Ajouter un membre du personnel</h3>
      <label>Nom<input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required /></label>
      <label>E-mail<input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label>
      <label>
        Mot de passe (8 caractères min.)
        <input type="text" minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
      </label>
      <label>
        Rôle
        <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as StaffRole })}>
          <option value="BARISTA">Barista</option>
          <option value="ADMIN">Administrateur</option>
        </select>
      </label>
      <label>
        Coffee shop
        <select value={form.shop} onChange={(e) => setForm({ ...form, shop: e.target.value })}>
          {SHOPS.map((s) => <option key={s.name} value={s.name}>{s.name}</option>)}
          <option value="">Tous</option>
        </select>
      </label>
      {error && <p className="form-error form__full">{error}</p>}
      {done && <p className="notice notice--ok form__full">{done}</p>}
      <button className="btn btn--accent form__full">Créer le compte</button>
    </form>
  );
}
