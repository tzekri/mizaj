import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, api, customerToken, type OtpStart, type Plan } from '../api';
import { BRAND, PHOTOS, img } from '../config';
import { PageHero } from '../components/Layout';
import { Whatsapp } from '../components/Icons';

const DEFAULT_PLAN: Plan = { price: 120, drinks: 30, days: 30 };
const ESPRESSO_PRICE = 10;

const STEPS = [
  { n: '01', title: 'Inscrivez-vous', text: 'Saisissez votre numéro : vous recevez un code de validation sur WhatsApp.' },
  { n: '02', title: 'Payez en caisse', text: 'Présentez votre carte QR au barista et réglez l’abonnement. Il est activé immédiatement.' },
  { n: '03', title: 'Scannez à chaque café', text: 'Le barista scanne votre QR et valide : un espresso est décompté de votre carte.' },
  { n: '04', title: 'Rechargez', text: 'Après 30 jours ou 30 espressos, rechargez en caisse pour une nouvelle période.' },
];

export default function Subscribe() {
  const [plan, setPlan] = useState<Plan>(DEFAULT_PLAN);
  const loggedIn = !!customerToken.get();

  useEffect(() => {
    api.plan().then(setPlan).catch(() => {});
  }, []);

  const unit = (plan.price / plan.drinks).toFixed(plan.price % plan.drinks ? 2 : 0).replace('.', ',');
  const saving = plan.drinks * ESPRESSO_PRICE - plan.price;

  return (
    <>
      <PageHero
        title="Abonnement Mizaj"
        subtitle={`${plan.drinks} espressos par mois pour ${plan.price} DH. Votre carte est dans votre téléphone.`}
        photo={img(PHOTOS.espressoPour, 1920)}
      />

      <section className="section container sub-intro">
        <div className="sub-offer">
          <p className="eyebrow">L’offre</p>
          <div className="sub-price">
            <strong>{plan.price}</strong>
            <span>DH<br />/ mois</span>
          </div>
          <ul>
            <li><b>{plan.drinks} espressos</b> à consommer dans nos coffee shops</li>
            <li>Valable <b>{plan.days} jours</b> après activation</li>
            <li>Soit <b>{unit} DH l’espresso</b> au lieu de {ESPRESSO_PRICE} DH</li>
            <li>Jusqu’à <b>{saving} DH d’économie</b> chaque mois</li>
          </ul>
          <p className="sub-fine">Les espressos non consommés à la fin de la période ne sont pas reportés.</p>
        </div>
        <div className="sub-form-wrap">
          {loggedIn ? (
            <div className="sub-done">
              <h2>Vous êtes déjà abonné</h2>
              <p>Votre carte QR vous attend.</p>
              <Link to="/ma-carte" className="btn btn--accent">Voir ma carte</Link>
            </div>
          ) : (
            <SignupForm />
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

function SignupForm() {
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

  async function sendCode(e?: FormEvent) {
    e?.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api.otpStart(phone, firstName);
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
      navigate('/ma-carte', { replace: true });
    } catch (err) {
      setError((err as ApiError).message);
      setBusy(false);
    }
  }

  if (!sent) {
    return (
      <form className="sub-form" onSubmit={sendCode}>
        <h2>Créer ma carte</h2>
        <p className="sub-form__lead">Déjà inscrit ? Saisissez votre numéro pour retrouver votre carte.</p>
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
        <button className="btn btn--accent btn--block" disabled={busy}>
          <Whatsapp size={18} /> {busy ? 'Envoi…' : 'Recevoir mon code sur WhatsApp'}
        </button>
        <p className="sub-legal">
          En continuant, vous acceptez de recevoir un message WhatsApp de {BRAND.name} Coffee contenant votre code.
        </p>
      </form>
    );
  }

  return (
    <form className="sub-form" onSubmit={verify}>
      <h2>Code de validation</h2>
      <p className="sub-form__lead">
        {sent.delivered ? 'Nous avons envoyé un code à 6 chiffres sur WhatsApp au ' : 'Code généré pour le '}
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
      <button type="button" className="link sub-resend" disabled={cooldown > 0 || busy} onClick={() => sendCode()}>
        {cooldown > 0 ? `Renvoyer le code dans ${cooldown} s` : 'Renvoyer le code'}
      </button>
    </form>
  );
}
