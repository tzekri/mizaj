import { useState, type FormEvent } from 'react';
import { BRAND, PHOTOS, img } from '../config';
import { PageHero } from '../components/Layout';

const STEPS = [
  { n: '01', title: 'Candidature', text: 'Remplissez le formulaire, notre équipe vous recontacte sous 48 h.' },
  { n: '02', title: 'Étude du projet', text: 'Analyse de l’emplacement, du budget et du potentiel de votre ville.' },
  { n: '03', title: 'Formation', text: 'Formation barista et gestion pour vous et votre équipe, dans nos coffee shops.' },
  { n: '04', title: 'Ouverture', text: 'Aménagement clé en main, approvisionnement en café et lancement accompagné.' },
];

const PERKS = [
  { k: '120 000 DH', v: 'Droit d’entrée indicatif' },
  { k: '18–24 mois', v: 'Retour sur investissement visé' },
  { k: '60–120 m²', v: 'Surface recommandée' },
  { k: '100 %', v: 'Café torréfié par Mizaje' },
];

export default function Franchise() {
  const [sent, setSent] = useState(false);

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const d = new FormData(e.currentTarget);
    const body = [...d.entries()].map(([k, v]) => `${k} : ${v}`).join('\n');
    window.location.href = `mailto:${BRAND.email}?subject=${encodeURIComponent('Candidature franchise Mizaje')}&body=${encodeURIComponent(body)}`;
    setSent(true);
  }

  return (
    <>
      <PageHero
        title="Devenez franchisé"
        subtitle="Ouvrez votre coffee shop Mizaje et rejoignez une marque marocaine en pleine croissance."
        photo="/brand/storefront.jpg"
      />
      <section className="section container">
        <div className="perks">
          {PERKS.map((p) => (
            <div key={p.v} className="perk">
              <strong>{p.k}</strong>
              <span>{p.v}</span>
            </div>
          ))}
        </div>
      </section>
      <section className="section steps-wrap">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Comment ça marche</p>
            <h2>4 étapes vers votre coffee shop</h2>
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
      <section className="section container franchise-form-wrap">
        <div className="section-head">
          <p className="eyebrow">Candidature</p>
          <h2>Parlons de votre projet</h2>
        </div>
        {sent ? (
          <div className="franchise-card">
            <h2>Merci !</h2>
            <p>Votre messagerie s’est ouverte avec votre candidature. Envoyez l’e-mail et nous revenons vers vous sous 48 h.</p>
          </div>
        ) : (
          <form className="form" onSubmit={submit}>
            <label>Nom complet<input name="Nom" required /></label>
            <label>Téléphone<input name="Téléphone" type="tel" required /></label>
            <label>E-mail<input name="E-mail" type="email" required /></label>
            <label>Ville souhaitée<input name="Ville" required /></label>
            <label>
              Budget disponible
              <select name="Budget" defaultValue="">
                <option value="" disabled>Choisir…</option>
                <option>Moins de 500 000 DH</option>
                <option>500 000 – 1 000 000 DH</option>
                <option>Plus de 1 000 000 DH</option>
              </select>
            </label>
            <label>
              Local disponible ?
              <select name="Local" defaultValue="">
                <option value="" disabled>Choisir…</option>
                <option>Oui</option>
                <option>Non, en recherche</option>
              </select>
            </label>
            <label className="form__full">Message<textarea name="Message" rows={4} /></label>
            <button className="btn btn--accent form__full" type="submit">Envoyer ma candidature</button>
          </form>
        )}
      </section>
    </>
  );
}
