import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ApiError, api, customerToken, type OtpStart, type Plan } from '../api';
import { BRAND, PHOTOS, img } from '../config';
import { PageHero } from '../components/Layout';
import { Whatsapp } from '../components/Icons';

/** Prix de référence d'une boisson à l'unité, pour afficher l'économie. */
const UNIT_PRICE: Record<string, number> = { espresso: 10, 'boisson au lait': 20 };

const STEPS = [
  { n: '01', title: 'Choisissez', text: 'Sélectionnez votre formule et saisissez votre numéro : un code de validation arrive sur WhatsApp.' },
  { n: '02', title: 'Payez en caisse', text: 'Présentez votre carte QR au barista et réglez la formule. Elle est activée immédiatement.' },
  { n: '03', title: 'Scannez à chaque café', text: 'Le barista scanne votre QR et valide : une boisson est décomptée de votre carte.' },
  { n: '04', title: 'Rechargez', text: 'Quand la période se termine ou que vos boissons sont consommées, rechargez en caisse.' },
];

const fmtUnit = (p: Plan) => (p.price / p.drinks).toFixed(p.price % p.drinks ? 1 : 0).replace('.', ',');

export default function Subscribe() {
  const [plans, setPlans] = useState<Plan[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [params, setParams] = useSearchParams();
  const loggedIn = !!customerToken.get();

  useEffect(() => {
    api.plans().then(setPlans).catch((e) => setError((e as ApiError).message));
  }, []);

  const selectedCode = params.get('formule') ?? plans?.find((p) => p.code === 'espresso')?.code ?? plans?.[0]?.code;
  const selected = plans?.find((p) => p.code === selectedCode) ?? plans?.[0];
  const select = (code: string) => setParams({ formule: code }, { replace: true });

  return (
    <>
      <PageHero
        title="Abonnement Mizaj"
        subtitle="Votre café du quotidien à prix doux. Votre carte d’abonné est dans votre téléphone."
        photo={img(PHOTOS.espressoPour, 1920)}
      />

      <section className="section container sub-intro">
        <div className="sub-plans">
          <p className="eyebrow">1. Choisissez votre formule</p>
          {error && <p className="form-error">{error}</p>}
          {!plans && !error && <p className="empty">Chargement des formules…</p>}
          {plans?.map((p) => {
            const ref = UNIT_PRICE[p.unit];
            const saving = ref ? p.drinks * ref - p.price : 0;
            return (
              <button
                key={p.code}
                type="button"
                className={`plan-card ${selected?.code === p.code ? 'plan-card--on' : ''}`}
                onClick={() => select(p.code)}
                aria-pressed={selected?.code === p.code}
              >
                <span className="plan-card__radio" aria-hidden />
                <span className="plan-card__body">
                  <strong>{p.name}</strong>
                  <span>{p.drinks} {p.unitPlural} · {p.days} jours</span>
                  {p.description && <em>{p.description}</em>}
                </span>
                <span className="plan-card__price">
                  <b>{p.price} DH</b>
                  <small>{fmtUnit(p)} DH / {p.unit}</small>
                  {saving > 0 && <small className="plan-card__save">−{saving} DH</small>}
                </span>
              </button>
            );
          })}
          <p className="sub-fine">
            Le paiement se fait en caisse, à l’activation. Les boissons non consommées à la fin de la période ne sont pas reportées.
          </p>
        </div>
        <div className="sub-form-wrap">
          {loggedIn ? (
            <div className="sub-done">
              <h2>Vous avez déjà votre carte</h2>
              <p>Ouvrez-la pour la présenter au barista.</p>
              <Link to="/ma-carte" className="btn btn--accent">Voir ma carte</Link>
            </div>
          ) : (
            <SignupForm plan={selected} />
          )}
        </div>
      </section>

      <section className="section steps-wrap">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Comment ça marche</p>
            <h2>Votre carte, sans carte</h2>
          </div>
          <div className="steps">
            {STEPS.map((s) => (
              <div key={s.n} className="step">
                <span>{s.n}</span>
                <h3>{s.title}</h3>
                <p>{s.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

function SignupForm({ plan }: { plan?: Plan }) {
  const navigate = useNavigate();
  const [phone, setPhone] = useState('');
  const [firstName, setFirstName] = useState('');
  const [sent, setSent] = useState<OtpStart | null>(null);
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);
  const codeInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  async function sendCode(e?: FormEvent, channel?: 'whatsapp' | 'sms') {
    e?.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api.otpStart(phone, firstName, plan?.code, channel);
      setSent(res);
      setCode('');
      setCooldown(res.resendInSeconds);
      setTimeout(() => codeInput.current?.focus(), 50);
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setBusy(false);
    }
  }

  async function verify(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api.otpVerify(phone, code);
      customerToken.set(res.token);
      navigate('/ma-carte?bienvenue=1', { replace: true });
    } catch (err) {
      setError((err as ApiError).message);
      setBusy(false);
    }
  }

  if (!sent) {
    return (
      <form className="sub-form" onSubmit={sendCode}>
        <p className="eyebrow">2. Créez votre carte</p>
        {plan && (
          <div className="sub-recap">
            <span>{plan.name}</span>
            <b>{plan.price} DH</b>
          </div>
        )}
        <label>
          Numéro WhatsApp
          <input
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            placeholder="06 12 34 56 78"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
          />
        </label>
        <label>
          Prénom <span className="optional">(affiché au barista)</span>
          <input autoComplete="given-name" maxLength={60} value={firstName} onChange={(e) => setFirstName(e.target.value)} />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="btn btn--accent btn--block" disabled={busy || !plan}>
          <Whatsapp size={18} /> {busy ? 'Envoi…' : 'Recevoir mon code'}
        </button>
        <p className="sub-legal">
          Code envoyé sur WhatsApp (ou par SMS). Déjà inscrit ? Saisissez votre numéro pour retrouver votre carte. En continuant,
          vous acceptez de recevoir un message de {BRAND.name} Coffee contenant votre code.
        </p>
      </form>
    );
  }

  return (
    <form className="sub-form" onSubmit={verify}>
      <h2>Code de validation</h2>
      <p className="sub-form__lead">
        {!sent.delivered
          ? 'Code généré pour le '
          : sent.channel === 'sms'
            ? 'Nous avons envoyé un code à 6 chiffres par SMS au '
            : 'Nous avons envoyé un code à 6 chiffres sur WhatsApp au '}
        <b>{sent.phone}</b>.{' '}
        <button type="button" className="link" onClick={() => { setSent(null); setError(null); }}>Modifier</button>
      </p>
      {sent.devCode && (
        <p className="dev-code">
          Mode démo (WhatsApp pas encore branché) : votre code est <b>{sent.devCode}</b>
        </p>
      )}
      <input
        ref={codeInput}
        className="otp-input"
        inputMode="numeric"
        autoComplete="one-time-code"
        pattern="\d{6}"
        maxLength={6}
        placeholder="••••••"
        value={code}
        onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
        aria-label="Code à 6 chiffres"
        required
      />
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="btn btn--accent btn--block" disabled={busy || code.length !== 6}>
        {busy ? 'Vérification…' : sent.newAccount ? 'Valider et créer ma carte' : 'Valider et ouvrir ma carte'}
      </button>
      <div className="sub-resend">
        <button type="button" className="link" disabled={cooldown > 0 || busy} onClick={() => sendCode(undefined, sent.channel)}>
          {cooldown > 0 ? `Renvoyer le code dans ${cooldown} s` : 'Renvoyer le code'}
        </button>
        {sent.smsAvailable && sent.channel === 'whatsapp' && (
          <button type="button" className="link" disabled={cooldown > 0 || busy} onClick={() => sendCode(undefined, 'sms')}>
            Pas de WhatsApp ? Recevoir le code par SMS
          </button>
        )}
      </div>
    </form>
  );
}
