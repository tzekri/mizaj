import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
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
  type Today,
} from '../api';
import { SHOPS } from '../config';
import { Header } from '../components/Layout';

export default function Barista() {
  const [session, setSession] = useState<StaffSession | null>(staffSession.get());
  const [tab, setTab] = useState<'counter' | 'admin'>('counter');

  const logout = useCallback(() => {
    staffSession.set(null);
    setSession(null);
  }, []);

  return (
    <>
      <section className="app-top">
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
                    <button className={tab === 'admin' ? 'active' : ''} onClick={() => setTab('admin')}>Administration</button>
                  </div>
                )}
                <button className="btn btn--outline btn--sm" onClick={logout}>Déconnexion</button>
              </div>
            </div>
            {tab === 'counter' ? <Counter onAuthError={logout} /> : <Admin onAuthError={logout} />}
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
  const [notice, setNotice] = useState<Notice>(null);
  const [busy, setBusy] = useState(false);
  const [today, setToday] = useState<Today | null>(null);
  const [phone, setPhone] = useState('');

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
        setCard(await api.scan(data));
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
      setCard(await api.lookup(phone));
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
      (c) => `Espresso validé ✓ — il reste ${c.drinksLeft} espresso(s).`,
      'RECENT',
    );
  }

  function activate() {
    if (!card) return;
    const { price, drinks } = card.plan;
    if (!confirm(`Confirmer : ${price} DH encaissés ? ${drinks} espressos seront activés.`)) return;
    run(
      (replace) => api.activate(card.customerId, replace),
      (c) => `Abonnement activé ✓ — ${c.drinksLeft} espressos jusqu’au ${fmtDate(c.expiresAt)}.`,
      'STILL_ACTIVE',
    );
  }

  function undo() {
    if (!card?.undoableRedemptionId) return;
    if (!confirm('Annuler le dernier espresso validé ? Il sera recrédité sur la carte.')) return;
    run(() => api.undo(card.undoableRedemptionId!), (c) => `Validation annulée — ${c.drinksLeft} espresso(s) restant(s).`);
  }

  function reset() {
    setCard(null);
    setNotice(null);
  }

  return (
    <div className="counter">
      <div className="counter__left">
        {today && (
          <div className="today">
            <div><strong>{today.redemptions}</strong><span>espressos aujourd’hui</span></div>
            <div><strong>{today.activations}</strong><span>abonnements activés</span></div>
          </div>
        )}
        {card ? (
          <button className="btn btn--accent btn--block" onClick={reset}>Scanner un autre client</button>
        ) : (
          <Scanner onResult={onScan} paused={busy} />
        )}
        <form className="lookup" onSubmit={lookup}>
          <input type="tel" inputMode="tel" placeholder="Ou rechercher par numéro : 06…" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <button className="btn btn--outline btn--sm" disabled={busy || !phone.trim()}>Rechercher</button>
        </form>
      </div>

      <div className="counter__right">
        {notice && <p className={`notice notice--${notice.kind}`} role="status">{notice.text}</p>}
        {card ? (
          <CustomerPanel card={card} busy={busy} onRedeem={redeem} onActivate={activate} onUndo={undo} />
        ) : (
          <div className="counter__empty">
            <h2>Scannez la carte du client</h2>
            <p>Le client ouvre « Ma carte » sur son téléphone et présente son QR code devant la caméra.</p>
          </div>
        )}
      </div>
    </div>
  );
}

function CustomerPanel({
  card,
  busy,
  onRedeem,
  onActivate,
  onUndo,
}: {
  card: StaffCard;
  busy: boolean;
  onRedeem: () => void;
  onActivate: () => void;
  onUndo: () => void;
}) {
  const active = card.status === 'ACTIVE';
  return (
    <div className={`customer customer--${card.status.toLowerCase()}`}>
      <div className="customer__head">
        <div>
          <h2>{card.firstName || 'Client sans prénom'}</h2>
          <p>{card.phone} · client depuis le {fmtDate(card.memberSince)}</p>
        </div>
        <span className={`badge badge--${card.status.toLowerCase()}`}>{STATUS_LABEL[card.status]}</span>
      </div>

      {active && (
        <div className="customer__count">
          <strong>{card.drinksLeft}</strong>
          <span>espresso(s) restant(s) sur {card.drinksTotal}<br />expire le {fmtDate(card.expiresAt)} ({daysLeft(card.expiresAt)} j)</span>
        </div>
      )}
      {card.status === 'EXPIRED' && <p className="customer__msg">Abonnement terminé le {fmtDate(card.expiresAt)}.</p>}
      {card.status === 'EMPTY' && <p className="customer__msg">Les {card.drinksTotal} espressos ont été consommés.</p>}
      {card.status === 'NONE' && <p className="customer__msg">Compte créé, abonnement jamais activé : encaissez {card.plan.price} DH puis activez.</p>}
      {card.lastRedemptionAt && <p className="customer__last">Dernier espresso : {fmtDateTime(card.lastRedemptionAt)}</p>}

      <div className="customer__actions">
        {active ? (
          <button className="btn btn--accent btn--xl" disabled={busy} onClick={onRedeem}>Valider 1 espresso</button>
        ) : (
          <button className="btn btn--accent btn--xl" disabled={busy} onClick={onActivate}>
            {card.status === 'NONE' ? 'Activer' : 'Renouveler'} — {card.plan.price} DH encaissés
          </button>
        )}
        {active && (
          <button className="btn btn--outline btn--sm" disabled={busy} onClick={onActivate}>Renouveler par anticipation</button>
        )}
        {card.undoableRedemptionId && (
          <button className="link" disabled={busy} onClick={onUndo}>Annuler le dernier espresso</button>
        )}
      </div>
    </div>
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
          <Kpi value={stats.redemptionsToday} label="espressos aujourd’hui" />
          <Kpi value={stats.redemptionsThisMonth} label="espressos ce mois-ci" />
          <Kpi value={stats.customers} label="comptes créés" />
        </div>
      )}

      <h2>Abonnés</h2>
      {subs.length === 0 ? (
        <p className="empty">Aucun abonnement activé pour l’instant.</p>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Client</th><th>Téléphone</th><th>État</th><th>Restant</th><th>Fin</th></tr>
            </thead>
            <tbody>
              {subs.map((s) => (
                <tr key={s.customerId}>
                  <td>{s.firstName || '—'}</td>
                  <td>{s.phone}</td>
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
