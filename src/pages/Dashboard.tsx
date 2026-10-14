import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError, api, fmtDate, type DashboardData, type DashboardKpis } from '../api';
import { Bars, Columns, DataTable, Heatmap, Legend, Meter, SERIES, num, type ColumnDatum } from '../components/charts';

const TZ = 'Africa/Casablanca';
/** Date du jour à Casablanca, au format AAAA-MM-JJ. */
const todayIso = () => new Intl.DateTimeFormat('en-CA', { timeZone: TZ }).format(new Date());
const shift = (iso: string, days: number) => {
  const d = new Date(iso + 'T12:00:00Z');
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
};

type Preset = { id: string; label: string; range: () => [string, string] };
const PRESETS: Preset[] = [
  { id: '7', label: '7 jours', range: () => [shift(todayIso(), -6), todayIso()] },
  { id: '30', label: '30 jours', range: () => [shift(todayIso(), -29), todayIso()] },
  { id: '90', label: '90 jours', range: () => [shift(todayIso(), -89), todayIso()] },
  { id: 'month', label: 'Ce mois', range: () => [todayIso().slice(0, 8) + '01', todayIso()] },
  {
    id: 'last-month',
    label: 'Mois dernier',
    range: () => {
      const firstThis = todayIso().slice(0, 8) + '01';
      const lastPrev = shift(firstThis, -1);
      return [lastPrev.slice(0, 8) + '01', lastPrev];
    },
  },
  { id: '12m', label: '12 mois', range: () => [shift(todayIso(), -364), todayIso()] },
];

const DH = (v: number) => `${num(v)} DH`;
const shortDay = (iso: string) => new Date(iso + 'T12:00:00Z').toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', timeZone: 'UTC' });
const longDay = (iso: string) => new Date(iso + 'T12:00:00Z').toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' });
const monthLabel = (ym: string) => new Date(ym + '-15T12:00:00Z').toLocaleDateString('fr-FR', { month: 'short', year: '2-digit', timeZone: 'UTC' });

export default function Dashboard({ onAuthError }: { onAuthError: () => void }) {
  const [preset, setPreset] = useState('30');
  const [range, setRange] = useState<[string, string]>(PRESETS[1].range());
  const [shop, setShop] = useState('');
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await api.dashboard(range[0], range[1], shop || undefined));
    } catch (e) {
      const err = e as ApiError;
      if (err.status === 401) onAuthError();
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [range, shop, onAuthError]);
  useEffect(() => {
    load();
  }, [load]);

  function choose(p: Preset) {
    setPreset(p.id);
    setRange(p.range());
  }

  async function download(kind: 'consommations' | 'abonnements') {
    try {
      await api.downloadCsv(kind, range[0], range[1], shop || undefined);
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  return (
    <div className="dash viz-root">
      {/* Filtres : une seule rangée, au-dessus de tout ce qu'ils filtrent */}
      <div className="dash-filters">
        <div className="seg seg--wrap" role="group" aria-label="Période">
          {PRESETS.map((p) => (
            <button key={p.id} className={preset === p.id ? 'active' : ''} onClick={() => choose(p)}>{p.label}</button>
          ))}
        </div>
        <label className="dash-date">
          Du
          <input type="date" value={range[0]} max={range[1]} onChange={(e) => { setPreset('custom'); setRange([e.target.value, range[1]]); }} />
        </label>
        <label className="dash-date">
          au
          <input type="date" value={range[1]} min={range[0]} max={todayIso()} onChange={(e) => { setPreset('custom'); setRange([range[0], e.target.value]); }} />
        </label>
        <label className="dash-date">
          Coffee shop
          <select value={shop} onChange={(e) => setShop(e.target.value)}>
            <option value="">Tous</option>
            {data?.shops.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <div className="dash-export">
          <button className="btn btn--outline btn--sm" onClick={() => download('consommations')}>Exporter les consommations (CSV)</button>
          <button className="btn btn--outline btn--sm" onClick={() => download('abonnements')}>Exporter les abonnements (CSV)</button>
        </div>
      </div>

      {error && <p className="notice notice--error">{error}</p>}
      {!data ? (
        <p className="empty">Chargement du tableau de bord…</p>
      ) : (
        // Pendant le rechargement, on garde l'affichage précédent en transparence (pas de saut)
        <div className={loading ? 'dash-body dash-body--loading' : 'dash-body'}>
          <p className="dash-period">
            Du {fmtDate(data.from + 'T12:00:00Z')} au {fmtDate(data.to + 'T12:00:00Z')}
            {data.shop ? ` · ${data.shop}` : ' · tous les coffee shops'} — comparé aux {daysBetween(data.from, data.to)} jours précédents
          </p>
          <KpiGrid k={data.kpis} p={data.previous} />
          <DashCharts data={data} />
        </div>
      )}
    </div>
  );
}

const daysBetween = (a: string, b: string) => Math.round((Date.parse(b) - Date.parse(a)) / 86_400_000) + 1;

function Delta({ now, before, money }: { now: number; before: number; money?: boolean }) {
  if (before === 0 && now === 0) return <span className="kpi__delta">= période précédente</span>;
  if (before === 0) return <span className="kpi__delta kpi__delta--up">▲ nouveau</span>;
  const pct = Math.round(((now - before) / before) * 100);
  const cls = pct > 0 ? 'kpi__delta--up' : pct < 0 ? 'kpi__delta--down' : '';
  return (
    <span className={`kpi__delta ${cls}`} title={`Période précédente : ${money ? DH(before) : num(before)}`}>
      {pct > 0 ? '▲' : pct < 0 ? '▼' : '='} {pct > 0 ? '+' : ''}{pct} % <span>vs période préc.</span>
    </span>
  );
}

function KpiGrid({ k, p }: { k: DashboardKpis; p: DashboardKpis }) {
  return (
    <div className="dash-kpis">
      <div className="kpi kpi--hero">
        <span>Chiffre d’affaires abonnements</span>
        <strong>{DH(k.revenue)}</strong>
        <Delta now={k.revenue} before={p.revenue} money />
      </div>
      <div className="kpi">
        <span>Boissons servies</span>
        <strong>{num(k.redemptions)}</strong>
        <Delta now={k.redemptions} before={p.redemptions} />
        <small>{num(k.redemptionsPerDay)} par jour en moyenne</small>
      </div>
      <div className="kpi">
        <span>Abonnements activés</span>
        <strong>{num(k.activations)}</strong>
        <Delta now={k.activations} before={p.activations} />
        <small>{num(k.firstActivations)} nouveaux · {num(k.renewals)} renouvellements</small>
      </div>
      <div className="kpi">
        <span>Abonnés actifs aujourd’hui</span>
        <strong>{num(k.activeNow)}</strong>
        <small>abonnements en cours de validité</small>
      </div>
      <div className="kpi">
        <span>Nouveaux comptes</span>
        <strong>{num(k.newCustomers)}</strong>
        <Delta now={k.newCustomers} before={p.newCustomers} />
      </div>
      <div className="kpi">
        <span>Taux de consommation</span>
        <strong>{k.consumptionRate == null ? '—' : `${num(k.consumptionRate)} %`}</strong>
        <small>boissons bues / achetées, abonnements terminés sur la période</small>
      </div>
      <div className="kpi">
        <span>Taux de renouvellement</span>
        <strong>{k.renewalRate == null ? '—' : `${num(k.renewalRate)} %`}</strong>
        <small>abonnements terminés suivis d’une recharge</small>
      </div>
    </div>
  );
}

function DashCharts({ data }: { data: DashboardData }) {
  const planNames = data.plans.map((p) => p.name);
  const planColor = (code: string) => SERIES[Math.max(0, data.plans.findIndex((p) => p.code === code)) % SERIES.length];

  // Au-delà de 62 jours, les colonnes quotidiennes deviennent illisibles : regroupement par semaine.
  const weekly = data.daily.length > 62;
  const buckets = useMemo(() => {
    if (!weekly) return data.daily.map((d) => ({ start: d.date, end: d.date, redemptions: d.redemptions, activations: d.activations, revenue: d.revenue }));
    const out: { start: string; end: string; redemptions: number; activations: number; revenue: number }[] = [];
    data.daily.forEach((d, i) => {
      if (i % 7 === 0) out.push({ start: d.date, end: d.date, redemptions: 0, activations: 0, revenue: 0 });
      const b = out[out.length - 1];
      b.end = d.date;
      b.redemptions += d.redemptions;
      b.activations += d.activations;
      b.revenue += d.revenue;
    });
    return out;
  }, [data.daily, weekly]);
  const title = (b: { start: string; end: string }) => (b.start === b.end ? longDay(b.start) : `Semaine du ${shortDay(b.start)} au ${shortDay(b.end)}`);

  const drinks: ColumnDatum[] = buckets.map((b) => ({ label: shortDay(b.start), title: title(b), values: [b.redemptions] }));
  const activations: ColumnDatum[] = buckets.map((b) => ({ label: shortDay(b.start), title: title(b), values: [b.activations] }));
  const months: ColumnDatum[] = data.monthly.map((m) => ({
    label: monthLabel(m.month),
    title: new Date(m.month + '-15T12:00:00Z').toLocaleDateString('fr-FR', { month: 'long', year: 'numeric', timeZone: 'UTC' }),
    values: data.plans.map((p) => m.revenueByPlan[p.code] ?? 0),
  }));
  const peak = useMemo(() => {
    let best = { d: 0, h: 0, v: 0 };
    data.heatmap.forEach((row, d) => row.forEach((v, h) => { if (v > best.v) best = { d, h, v }; }));
    return best;
  }, [data.heatmap]);
  const DAYS_LONG = ['lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi', 'dimanche'];

  return (
    <div className="dash-grid">
      <section className="dash-card dash-card--wide">
        <header>
          <h3>Boissons servies {weekly ? 'par semaine' : 'par jour'}</h3>
          <p>{num(data.kpis.redemptions)} au total sur la période</p>
        </header>
        <Columns data={drinks} series={['Boissons servies']} unit="boisson(s)" />
        <DataTable head={[weekly ? 'Semaine' : 'Jour', 'Boissons servies']} rows={buckets.map((b) => [title(b), b.redemptions])} />
      </section>

      <section className="dash-card">
        <header>
          <h3>Abonnements activés {weekly ? 'par semaine' : 'par jour'}</h3>
          <p>{num(data.kpis.activations)} activations · {DH(data.kpis.revenue)}</p>
        </header>
        <Columns data={activations} series={['Abonnements activés']} height={200} unit="activation(s)" />
        <DataTable head={[weekly ? 'Semaine' : 'Jour', 'Activations', 'Encaissé (DH)']} rows={buckets.map((b) => [title(b), b.activations, b.revenue])} />
      </section>

      <section className="dash-card">
        <header>
          <h3>Chiffre d’affaires par mois, par formule</h3>
          <p>6 derniers mois</p>
        </header>
        <Columns data={months} series={planNames} height={200} format={(v) => num(v)} unit="DH" />
        <DataTable
          head={['Mois', ...planNames, 'Total (DH)']}
          rows={data.monthly.map((m) => [monthLabel(m.month), ...data.plans.map((p) => m.revenueByPlan[p.code] ?? 0), m.revenue])}
        />
      </section>

      <section className="dash-card">
        <header>
          <h3>Par coffee shop</h3>
          <p>Boissons servies sur la période (tous les coffee shops, pour comparer)</p>
        </header>
        {data.shopStats.length === 0 ? <p className="empty">Aucune activité sur la période.</p> : (
          <>
            <Bars data={data.shopStats.map((s) => ({ label: s.shop, value: s.redemptions }))} unit="boisson(s)" />
            <div className="table-wrap dash-table">
              <table className="table">
                <thead><tr><th>Coffee shop</th><th className="num">Boissons</th><th className="num">Activations</th><th className="num">Encaissé</th></tr></thead>
                <tbody>
                  {data.shopStats.map((s) => (
                    <tr key={s.shop}><td>{s.shop}</td><td className="num">{num(s.redemptions)}</td><td className="num">{num(s.activations)}</td><td className="num">{DH(s.revenue)}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      <section className="dash-card">
        <header>
          <h3>Par formule</h3>
          <p>Abonnés actifs, ventes et consommation</p>
        </header>
        <Legend items={data.plans.map((p) => ({ label: p.name, color: planColor(p.code) }))} />
        <div className="table-wrap dash-table">
          <table className="table">
            <thead><tr><th>Formule</th><th className="num">Actifs</th><th className="num">Activations</th><th className="num">Encaissé</th><th className="num">Boissons</th><th>Consommation</th></tr></thead>
            <tbody>
              {data.plans.map((p) => (
                <tr key={p.code}>
                  <td><i className="key" style={{ background: planColor(p.code) }} />{p.name} <small>({p.price} DH)</small></td>
                  <td className="num">{num(p.activeNow)}</td>
                  <td className="num">{num(p.activations)}</td>
                  <td className="num">{DH(p.revenue)}</td>
                  <td className="num">{num(p.redemptions)}</td>
                  <td><Meter value={p.consumptionRate ?? null} label={`Consommation ${p.name}`} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="dash-note">Consommation : boissons bues / achetées, pour les abonnements arrivés à échéance sur la période.</p>
      </section>

      <section className="dash-card dash-card--wide">
        <header>
          <h3>Heures de pointe</h3>
          <p>
            {peak.v > 0
              ? `Pic : le ${DAYS_LONG[peak.d]} entre ${peak.h}h et ${peak.h + 1}h (${peak.v} boissons sur la période)`
              : 'Aucune boisson servie sur la période'}
          </p>
        </header>
        <Heatmap grid={data.heatmap} />
        <DataTable
          head={['Jour', ...Array.from({ length: 16 }, (_, i) => `${7 + i}h`)]}
          rows={DAYS_LONG.map((d, i) => [d, ...data.heatmap[i].slice(7, 23)])}
        />
      </section>

      <section className="dash-card">
        <header>
          <h3>À relancer</h3>
          <p>Abonnements qui se terminent dans 5 jours ou presque épuisés : proposez le renouvellement</p>
        </header>
        {data.followUp.length === 0 ? <p className="empty">Personne à relancer pour le moment.</p> : (
          <div className="table-wrap dash-table">
            <table className="table">
              <thead><tr><th>Client</th><th>Téléphone</th><th>Formule</th><th className="num">Restant</th><th>Fin</th><th>Motif</th></tr></thead>
              <tbody>
                {data.followUp.map((f) => (
                  <tr key={f.customerId}>
                    <td>{f.firstName || '—'}</td>
                    <td>{f.phone}</td>
                    <td>{f.planName || '—'}</td>
                    <td className="num">{f.drinksLeft} / {f.drinksTotal}</td>
                    <td>{fmtDate(f.expiresAt)}</td>
                    <td><span className={`badge ${f.reason === 'EXPIRING' ? 'badge--none' : 'badge--empty'}`}>{f.reason === 'EXPIRING' ? '⏳ Fin proche' : '☕ Presque épuisé'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="dash-card">
        <header>
          <h3>Clients les plus fidèles</h3>
          <p>Top 10 des boissons servies sur la période</p>
        </header>
        {data.topCustomers.length === 0 ? <p className="empty">Aucune boisson servie sur la période.</p> : (
          <div className="table-wrap dash-table">
            <table className="table">
              <thead><tr><th>#</th><th>Client</th><th>Téléphone</th><th>Formule</th><th className="num">Boissons</th></tr></thead>
              <tbody>
                {data.topCustomers.map((c, i) => (
                  <tr key={c.customerId}><td>{i + 1}</td><td>{c.firstName || '—'}</td><td>{c.phone}</td><td>{c.planName || '—'}</td><td className="num">{num(c.redemptions)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
