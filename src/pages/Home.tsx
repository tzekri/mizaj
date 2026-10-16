import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BRAND, PHOTOS, POPULAR, SHOPS, SLIDES, findItem, img } from '../config';
import { Header } from '../components/Layout';
import { Arrow, Clock, Pin } from '../components/Icons';

function Hero() {
  return (
    <section className="hero" style={{ backgroundImage: `url(${img(PHOTOS.heroBeans, 1920)})` }}>
      <Header hideLogo />
      <div className="hero__content">
        <h1 className="hero__brand">
          <img src="/brand/logo-light.png" alt={`Mizaje Coffee — ${BRAND.arabicTagline}`} />
        </h1>
        <p className="hero__tagline">{BRAND.tagline}</p>
        <p className="hero__fr">Votre café, selon votre humeur.</p>
        <div className="hero__ctas">
          <Link to="/menu" className="btn btn--light">Découvrir le menu</Link>
          <Link to="/franchise" className="btn btn--ghost">Devenir franchisé</Link>
        </div>
      </div>
      <div className="hero__steam" aria-hidden />
    </section>
  );
}

function BeanBand() {
  return (
    <section className="bean-band">
      <div className="bean-band__pattern" aria-hidden />
      <img src={img(PHOTOS.beansTamper, 1400)} alt="Grains de café torréfiés" className="bean-band__img" />
    </section>
  );
}

function Origin() {
  return (
    <section className="section container origin">
      <div className="origin__text">
        <p className="eyebrow">Arabica &amp; Robusta</p>
        <h2>Café torréfié avec passion</h2>
        <p>
          Savourez chaque gorgée de nos cafés soigneusement sélectionnés. Nos grains, sourcés en Éthiopie, au Brésil et
          en Colombie, sont torréfiés en petits lots à Casablanca pour révéler des arômes riches et authentiques.
        </p>
        <Link to="/menu" className="btn btn--accent">Voir nos boissons</Link>
      </div>
      <div className="origin__media">
        <img src={img(PHOTOS.roaster, 900)} alt="Torréfaction des grains Mizaje" />
        <div className="since-badge">
          <span>since</span>
          <strong>{BRAND.since}</strong>
        </div>
      </div>
    </section>
  );
}

function FranchiseCard() {
  return (
    <section className="container franchise-card-wrap">
      <div className="franchise-card">
        <h2>Devenir investisseur Mizaje Coffee</h2>
        <p>
          Rejoignez l’aventure Mizaje et investissez dans une marque de café en pleine expansion. Profitez d’une{' '}
          <b>opportunité unique</b> de développement avec notre réseau de coffee shops. Nous offrons un{' '}
          <b>retour sur investissement attractif</b> et un <b>accompagnement personnalisé</b> pour assurer votre réussite.
        </p>
        <Link to="/franchise" className="btn btn--accent">Devenir franchisé</Link>
      </div>
    </section>
  );
}

function Slider() {
  const [i, setI] = useState(0);
  const n = SLIDES.length;
  useEffect(() => {
    const t = setInterval(() => setI((x) => (x + 1) % n), 6000);
    return () => clearInterval(t);
  }, [i, n]);
  return (
    <section className="slider">
      {SLIDES.map((s, k) => (
        <div
          key={s.title}
          className={`slide ${k === i ? 'slide--active' : ''}`}
          style={{ backgroundImage: `url(${img(s.photo, 1920)})` }}
          aria-hidden={k !== i}
        >
          <div className="slide__content">
            <h2 className="script">{s.title}</h2>
            <p>{s.text}</p>
          </div>
        </div>
      ))}
      <button className="slider__nav slider__nav--prev" onClick={() => setI((i - 1 + n) % n)} aria-label="Précédent">
        <Arrow left />
      </button>
      <button className="slider__nav slider__nav--next" onClick={() => setI((i + 1) % n)} aria-label="Suivant">
        <Arrow />
      </button>
      <div className="slider__dots">
        {SLIDES.map((s, k) => (
          <button key={s.title} className={k === i ? 'active' : ''} onClick={() => setI(k)} aria-label={`Slide ${k + 1}`} />
        ))}
      </div>
    </section>
  );
}

function Craft() {
  return (
    <section className="craft">
      <img src="/brand/icon.png" alt="" className="craft__icon" />
      <p>
        Chaque tasse est le fruit d’un <em>savoir-faire minutieux</em>. Nos cafés d’exception éveillent vos sens et
        apportent chaleur et plaisir à chaque moment. Découvrez l’harmonie parfaite entre tradition et goût : nos cafés{' '}
        <em>Mizaje</em> Arabica et Robusta, cultivés avec soin, offrent une expérience aromatique incomparable.
      </p>
    </section>
  );
}

function Aroma() {
  return (
    <section className="aroma">
      <div className="aroma__img" style={{ backgroundImage: `url(${img(PHOTOS.smokeCup, 900)})` }} />
      <div className="aroma__text">
        <h2>
          <span>Divin</span>
          <strong>Arôme</strong>
          <span>dans chaque tasse</span>
        </h2>
        <p>Savourez l’équilibre parfait entre douceur et caractère. Chaque gorgée révèle un mélange raffiné, pensé pour éveiller vos sens.</p>
      </div>
      <div className="aroma__img" style={{ backgroundImage: `url(${img('1789073200448-bd416341470f', 900)})` }} />
    </section>
  );
}

function Products() {
  return (
    <section className="section container">
      <div className="section-head">
        <p className="eyebrow">Les incontournables</p>
        <h2>Produits populaires</h2>
        <p>Découvrez nos cafés soigneusement sélectionnés, où équilibre et richesse des arômes se mêlent pour un plaisir unique à chaque tasse.</p>
      </div>
      <div className="products">
        {POPULAR.map(({ slug, name, tag }) => {
          const p = findItem(slug, name)!;
          return (
            <Link key={name} to={`/menu/${slug}`} className="product">
              <div className="product__img">
                <img src={img(p.photo, 600)} alt={p.name} loading="lazy" />
                {tag && <span className="product__tag">{tag}</span>}
              </div>
              <h3>{p.name}</h3>
              <p>{p.desc}</p>
              <div className="product__price">{p.price} DH</div>
            </Link>
          );
        })}
      </div>
      <div className="center">
        <Link to="/menu" className="btn btn--outline">Voir tout le menu</Link>
      </div>
    </section>
  );
}

function SubscriptionTeaser() {
  return (
    <section className="sub-teaser">
      <div className="container sub-teaser__inner">
        <div className="sub-teaser__text">
          <p className="eyebrow">Nouveau · Abonnement</p>
          <h2>
            Votre café du quotidien
            <br />
            dès <strong>45 DH</strong>
          </h2>
          <p>
            Trois formules : Découverte, Espresso (30 espressos pour 120 DH, soit 4 DH l’espresso) et Lait &amp; Douceur.
            Inscrivez-vous en 30 secondes avec votre numéro : votre carte QR est dans votre téléphone, le barista la scanne à
            chaque café.
          </p>
          <Link to="/abonnement" className="btn btn--light">Je m’abonne</Link>
        </div>
        <div className="sub-teaser__phone" aria-hidden>
          <div className="mini-card">
            <img className="mini-card__brand" src="/brand/logo-mark-light.png" alt="" />
            <div className="mini-card__qr" />
            <strong>27 / 30</strong>
            <span>espressos restants</span>
          </div>
        </div>
      </div>
    </section>
  );
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
          {SHOPS.map((s) => (
            <article key={s.name} className="shop">
              <img src={img(s.photo, 700)} alt={`Coffee shop Mizaje ${s.name}`} loading="lazy" />
              <div className="shop__body">
                <h3>{s.name}</h3>
                <p><Pin size={15} /> {s.address}, {s.city}</p>
                <p><Clock size={15} /> {s.hours}</p>
              </div>
            </article>
          ))}
        </div>
        <div className="hero__ctas">
          <Link to="/menu" className="btn btn--light">Découvrir le menu</Link>
          <Link to="/franchise" className="btn btn--ghost">Devenir franchisé</Link>
        </div>
      </div>
    </section>
  );
}

export default function Home() {
  return (
    <>
      <Hero />
      <BeanBand />
      <Origin />
      <FranchiseCard />
      <Slider />
      <Craft />
      <Aroma />
      <Products />
      <SubscriptionTeaser />
      <ShopsStrip />
    </>
  );
}
