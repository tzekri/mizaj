import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, type JuiceIngredient } from '../api';
import { BRAND, FRANCHISE_ENABLED, PHOTOS, SHOPS, img } from '../config';
import { menuPhoto, useMenu } from '../menu';
import { Header } from '../components/Layout';
import { Arrow, Clock, Pin } from '../components/Icons';
import { IngredientThumb, JuiceGlass } from '../components/juice';

/** Héros bleu nuit, comme le fond du logo : accroche à gauche, médaillon Kidima à droite. */
function Hero() {
  return (
    <section className="k-hero">
      <Header hideLogo />
      <div className="container k-hero__inner">
        <div className="k-hero__text">
          <p className="k-eyebrow">Coffee house · Casablanca</p>
          <h1>
            Le café,
            <br />
            <em>à la marocaine.</em>
          </h1>
          <p className="k-hero__lead">
            Espresso, nouss-nouss, atay et jus pressés minute, servis dans nos trois coffee shops. Votre carte d’abonné
            tient dans votre téléphone.
          </p>
          <div className="k-hero__ctas">
            <Link to="/menu" className="btn btn--copper">Voir le menu</Link>
            <SecondCta />
          </div>
          <ul className="k-hero__facts">
            <li><strong>{SHOPS.length}</strong> coffee shops</li>
            <li><strong>4 DH</strong> l’espresso abonné</li>
            <li><strong>100 %</strong> pressé minute</li>
          </ul>
        </div>
        <div className="k-hero__badge">
          <span className="k-hero__ring" aria-hidden />
          <img src="/brand/logo.png" alt={`${BRAND.name} Coffee House — ${BRAND.arabicTagline}`} />
        </div>
      </div>
    </section>
  );
}

/** Second bouton du héros : la franchise, ou l'abonnement tant qu'elle est masquée. */
function SecondCta() {
  return FRANCHISE_ENABLED ? (
    <Link to="/franchise" className="btn btn--ghost">Devenir franchisé</Link>
  ) : (
    <Link to="/abonnement" className="btn btn--ghost">Je m’abonne dès 45 DH</Link>
  );
}

const WAYS = [
  {
    n: '01',
    to: '/menu',
    title: 'La carte',
    text: 'Cafés de spécialité, boissons glacées, thés et viennoiseries maison.',
    cta: 'Voir le menu',
    photo: '1763208387697-fe0a913bfed9',
  },
  {
    n: '02',
    to: '/abonnement',
    title: 'L’abonnement',
    text: 'Des espressos ou des boissons au lait à prix doux, avec une carte QR dans votre téléphone.',
    cta: 'Choisir ma formule',
    photo: PHOTOS.espressoPour,
  },
  {
    n: '03',
    to: '/jus',
    title: 'Compose ton jus',
    text: 'Une base, deux fruits, une touche : votre jus est pressé à la minute, à votre goût.',
    cta: 'Composer mon jus',
    photo: '1613478223719-2ab802602423',
  },
];

function Ways() {
  return (
    <section className="section container">
      <div className="section-head">
        <p className="eyebrow">Chez {BRAND.name}</p>
        <h2>Trois façons de se faire plaisir</h2>
      </div>
      <div className="ways">
        {WAYS.map((w) => (
          <Link key={w.n} to={w.to} className="way">
            <div className="way__img">
              <img src={img(w.photo, 700)} alt="" loading="lazy" />
              <span className="way__n">{w.n}</span>
            </div>
            <div className="way__body">
              <h3>{w.title}</h3>
              <p>{w.text}</p>
              <span className="way__cta">{w.cta} <Arrow size={18} /></span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

function FranchiseCard() {
  return (
    <section className="container franchise-card-wrap">
      <div className="franchise-card">
        <h2>Devenir investisseur {BRAND.name} Coffee House</h2>
        <p>
          Rejoignez l’aventure {BRAND.name} et investissez dans une marque de café en pleine expansion. Profitez d’une{' '}
          <b>opportunité unique</b> de développement avec notre réseau de coffee shops, tout en bénéficiant d’un{' '}
          <b>retour sur investissement attractif</b> et un <b>accompagnement personnalisé</b> pour assurer votre réussite.
        </p>
        <Link to="/franchise" className="btn btn--accent">Devenir franchisé</Link>
      </div>
    </section>
  );
}

function Products() {
  const { menu } = useMenu();
  const featured = (menu ?? []).flatMap((c) => c.items.filter((i) => i.featured).map((item) => ({ item, slug: c.slug }))).slice(0, 8);
  if (featured.length === 0) return null;
  return (
    <section className="section popular">
      <div className="container">
        <div className="popular__head">
          <div>
            <p className="eyebrow">Les incontournables</p>
            <h2>Nos clients les adorent</h2>
          </div>
          <Link to="/menu" className="btn btn--outline">Tout le menu</Link>
        </div>
        <div className="products">
          {featured.map(({ item: p, slug }) => (
            <Link key={p.id} to={`/menu/${slug}`} className="product">
              <div className="product__img">
                <img src={menuPhoto(p.photo, 600)} alt={p.name} loading="lazy" />
                {p.tag && <span className="product__tag">{p.tag}</span>}
                <span className="product__price">{p.price} DH</span>
              </div>
              <h3>{p.name}</h3>
              {p.description && <p>{p.description}</p>}
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}

function SubscriptionTeaser() {
  return (
    <section className="sub-teaser">
      <div className="container sub-teaser__inner">
        <div className="sub-teaser__text">
          <p className="eyebrow">Abonnement</p>
          <h2>
            Votre café du quotidien
            <br />
            dès <strong>45 DH</strong>
          </h2>
          <ol className="k-steps">
            <li><b>Choisissez</b> votre formule : Découverte, Espresso ou Lait &amp; Douceur.</li>
            <li><b>Inscrivez-vous</b> avec votre numéro : votre carte QR est dans votre téléphone.</li>
            <li><b>Présentez-la</b> en caisse : le barista la scanne à chaque café.</li>
          </ol>
          <Link to="/abonnement" className="btn btn--copper">Je m’abonne</Link>
        </div>
        <div className="sub-teaser__phone" aria-hidden>
          <div className="mini-card">
            <img className="mini-card__brand" src="/brand/logo-mark.png" alt="" />
            <div className="mini-card__qr" />
            <strong>27 / 30</strong>
            <span>espressos restants</span>
          </div>
        </div>
      </div>
    </section>
  );
}

/** Aperçu du composeur : les vrais ingrédients du moment et un verre d'exemple. */
function JuiceTeaser() {
  const [items, setItems] = useState<JuiceIngredient[]>([]);
  useEffect(() => {
    api.juiceCatalog().then((c) => setItems(c.ingredients)).catch(() => {});
  }, []);
  const sample = useMemo(() => {
    const pick = (code: string) => items.find((i) => i.code === code);
    const chosen = [pick('orange'), pick('mangue'), pick('menthe')].filter(Boolean) as JuiceIngredient[];
    return chosen.length ? chosen : items.slice(0, 1);
  }, [items]);
  if (items.length === 0) return null;
  return (
    <section className="juice-teaser">
      <div className="container juice-teaser__inner">
        <div className="juice-teaser__glass" aria-hidden>
          <JuiceGlass items={sample} size="S50" />
        </div>
        <div>
          <p className="eyebrow">Nouveau</p>
          <h2>Compose ton jus</h2>
          <p>Choisissez une base, jusqu’à deux fruits et une touche {BRAND.name}. Obtenez votre code, on presse à la minute.</p>
          <div className="juice-teaser__thumbs">
            {items.filter((i) => i.category !== 'EXTRA').slice(0, 9).map((i) => (
              <span key={i.code} title={i.name}><IngredientThumb item={i} size={46} /></span>
            ))}
          </div>
          <Link to="/jus" className="btn btn--accent">Composer mon jus</Link>
        </div>
      </div>
    </section>
  );
}

/** « 7h00 – 01h00 » : ouvert maintenant, à l'heure de Casablanca ? (horaires après minuit gérés) */
function isOpenNow(hours: string, now = new Date()) {
  const m = hours.match(/(\d{1,2})h(\d{2})\D+(\d{1,2})h(\d{2})/);
  if (!m) return null;
  const parts = new Intl.DateTimeFormat('fr-FR', { timeZone: 'Africa/Casablanca', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(now);
  const t = Number(parts.find((p) => p.type === 'hour')!.value) * 60 + Number(parts.find((p) => p.type === 'minute')!.value);
  const open = Number(m[1]) * 60 + Number(m[2]);
  const close = Number(m[3]) * 60 + Number(m[4]);
  return close > open ? t >= open && t < close : t >= open || t < close;
}

function ShopsStrip() {
  return (
    <section className="shops-strip">
      <div className="container">
        <div className="section-head section-head--light">
          <p className="eyebrow">Venez nous voir</p>
          <h2>Nos coffee shops</h2>
        </div>
        <div className="shops">
          {SHOPS.map((s) => {
            const open = isOpenNow(s.hours);
            return (
              <article key={s.name} className="shop">
                <div className="shop__img">
                  <img src={img(s.photo, 700)} alt={`Coffee shop ${BRAND.name} ${s.name}`} loading="lazy" />
                  {open !== null && <span className={`shop__status ${open ? 'shop__status--open' : ''}`}>{open ? 'Ouvert' : 'Fermé'}</span>}
                </div>
                <div className="shop__body">
                  <h3>{s.name}</h3>
                  <p><Pin size={15} /> {s.address}, {s.city}</p>
                  <p><Clock size={15} /> {s.hours}</p>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}

export default function Home() {
  return (
    <>
      <Hero />
      <Ways />
      <Products />
      <SubscriptionTeaser />
      <JuiceTeaser />
      {FRANCHISE_ENABLED && <FranchiseCard />}
      <ShopsStrip />
    </>
  );
}
