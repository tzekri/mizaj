import { useEffect, useState, type ReactNode } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { BRAND, FRANCHISE_ENABLED, SHOPS } from '../config';
import { Instagram, Search, Tiktok, Whatsapp } from './Icons';

/** Logo officiel, couleurs d'origine (fond crème) ; « full » : logo complet. */
export function Logo({ small = false, full = false, hidden = false }: { small?: boolean; full?: boolean; hidden?: boolean }) {
  return (
    <Link
      to="/"
      className={`logo ${small ? 'logo--small' : ''} ${hidden ? 'logo--hidden' : ''}`}
      aria-label="Kidima Coffee House — accueil"
      tabIndex={hidden ? -1 : undefined}
    >
      <img src={full ? '/brand/logo.png' : '/brand/logo-mark.png'} alt={`Kidima Coffee House — ${BRAND.slogan}`} />
    </Link>
  );
}

const NAV = [
  { to: '/', label: 'Accueil' },
  { to: '/menu', label: 'Menu' },
  { to: '/abonnement', label: 'Abonnement' },
  { to: '/jus', label: 'Compose ton jus' },
  { to: '/defi', label: 'Défi 10 s' },
  { to: '/nos-cafes', label: 'Nos coffee shops' },
  ...(FRANCHISE_ENABLED ? [{ to: '/franchise', label: 'Devenez franchisé' }] : []),
];

/** hideLogo : l'accueil affiche déjà le grand logo dans son héros. */
export function Header({ small = false, hideLogo = false }: { small?: boolean; hideLogo?: boolean }) {
  const [open, setOpen] = useState(false);
  const { pathname } = useLocation();
  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  return (
    <header className="header">
      <div className="header__top">
        <div className="header__social">
          <span>Suivez-nous</span>
          <span className="dash" />
          <a href={BRAND.instagram} target="_blank" rel="noreferrer" className="round-icon" aria-label="Instagram">
            <Instagram size={13} />
          </a>
          <a href={BRAND.tiktok} target="_blank" rel="noreferrer" className="round-icon" aria-label="TikTok">
            <Tiktok size={13} />
          </a>
        </div>
        <Logo small={small} hidden={hideLogo} />
        <div className="header__right">
          <Link to="/menu" className="round-icon" aria-label="Rechercher dans le menu">
            <Search size={13} />
          </Link>
          <button className="burger" onClick={() => setOpen((o) => !o)} aria-label="Menu" aria-expanded={open}>
            <span /><span /><span />
          </button>
        </div>
      </div>
      <nav className={`nav ${open ? 'nav--open' : ''}`}>
        {NAV.map((n) => (
          <NavLink key={n.to} to={n.to} end className={({ isActive }) => (isActive ? 'active' : '')}>
            {n.label}
          </NavLink>
        ))}
      </nav>
    </header>
  );
}

export function Footer() {
  return (
    <footer className="footer">
      <div className="container footer__grid">
        <div>
          <Logo full />
          <p className="footer__about">
            Café de spécialité torréfié à Casablanca — <span className="footer__slogan" dir="rtl" lang="ar">{BRAND.slogan}</span>
          </p>
          <div className="footer__social">
            <a href={BRAND.instagram} target="_blank" rel="noreferrer" className="round-icon" aria-label="Instagram"><Instagram size={14} /></a>
            <a href={BRAND.tiktok} target="_blank" rel="noreferrer" className="round-icon" aria-label="TikTok"><Tiktok size={14} /></a>
          </div>
        </div>
        <div>
          <h4>Nos coffee shops</h4>
          <ul className="footer__shops">
            {SHOPS.map((s) => (
              <li key={s.name}>
                <strong>{s.name}</strong>
                <span>{s.address}</span>
                <span>{s.city}</span>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h4>Contact</h4>
          <ul className="footer__contact">
            <li><a href={`tel:${BRAND.phone.replace(/\s/g, '')}`}>{BRAND.phone}</a></li>
            <li>{BRAND.email}</li>
          </ul>
          {FRANCHISE_ENABLED && <Link to="/franchise" className="btn btn--accent btn--sm">Devenir investisseur</Link>}
          <p className="footer__staff"><Link to="/barista">Espace barista</Link></p>
        </div>
      </div>
      <div className="footer__bottom">
        KIDIMA COFFEE HOUSE © Tous droits réservés — {new Date().getFullYear()}
      </div>
    </footer>
  );
}

export function Floating() {
  const [show, setShow] = useState(false);
  const { pathname } = useLocation();
  // Écrans applicatifs (carte QR, comptoir) : rien ne doit masquer le contenu.
  const appScreen = ['/ma-carte', '/barista', '/affiche'].includes(pathname);
  useEffect(() => {
    const onScroll = () => setShow(window.scrollY > 500);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);
  return (
    <>
      {!appScreen && (
        <a className="fab-wa" href={`https://wa.me/${BRAND.whatsapp}`} target="_blank" rel="noreferrer" aria-label="WhatsApp">
          <Whatsapp />
        </a>
      )}
      <button className={`go-top ${show ? 'go-top--show' : ''}`} onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
        GO<br />TOP
      </button>
    </>
  );
}

/** En-tête de page : bandeau bleu nuit (comme le fond du logo), titre à gauche, photo ronde cerclée de cuivre à droite. */
export function PageHero({ title, subtitle, photo, eyebrow }: { title: string; subtitle?: ReactNode; photo: string; eyebrow?: string }) {
  return (
    <section className="k-pagehero on-dark">
      <Header small />
      <div className="container k-pagehero__inner">
        <div className="k-pagehero__text">
          {eyebrow && <p className="k-eyebrow">{eyebrow}</p>}
          <h1>{title}</h1>
          {subtitle && <p className="k-pagehero__lead">{subtitle}</p>}
        </div>
        <div className="k-pagehero__photo" aria-hidden>
          <span className="k-hero__ring" />
          {/* Photo affichée en petit : on demande une image de 600 px au lieu de la grande version */}
          <img src={photo.replace(/([?&])w=\d+/, '$1w=600')} alt="" />
        </div>
      </div>
    </section>
  );
}
