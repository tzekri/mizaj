import { useEffect, useState, type ReactNode } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { BRAND, FRANCHISE_ENABLED, SHOPS } from '../config';
import { Instagram, Search, Tiktok, Whatsapp } from './Icons';

/** Logo officiel (version claire pour fonds sombres) : emblème + « MAROKOFFY » ; « full » : logo complet (ماروكوفي, Coffee House). */
export function Logo({ small = false, full = false, hidden = false }: { small?: boolean; full?: boolean; hidden?: boolean }) {
  return (
    <Link
      to="/"
      className={`logo ${small ? 'logo--small' : ''} ${hidden ? 'logo--hidden' : ''}`}
      aria-label="Marokoffy Coffee House — accueil"
      tabIndex={hidden ? -1 : undefined}
    >
      <img src={full ? '/brand/logo-light.png' : '/brand/logo-mark-light.png'} alt={`Marokoffy Coffee House — ${BRAND.arabicTagline}`} />
    </Link>
  );
}

const NAV = [
  { to: '/', label: 'Accueil' },
  { to: '/menu', label: 'Menu' },
  { to: '/abonnement', label: 'Abonnement' },
  { to: '/jus', label: 'Compose ton jus' },
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
            Café de spécialité torréfié à Casablanca — <span dir="rtl" lang="ar">{BRAND.arabicTagline}</span>
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
            <li>{BRAND.phone}</li>
            <li>{BRAND.email}</li>
          </ul>
          {FRANCHISE_ENABLED && <Link to="/franchise" className="btn btn--accent btn--sm">Devenir investisseur</Link>}
          <p className="footer__staff"><Link to="/barista">Espace barista</Link></p>
        </div>
      </div>
      <div className="footer__bottom">
        MAROKOFFY COFFEE HOUSE © Tous droits réservés — {new Date().getFullYear()}
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

export function PageHero({ title, subtitle, photo }: { title: string; subtitle?: ReactNode; photo: string }) {
  return (
    <section className="page-hero" style={{ backgroundImage: `url(${photo})` }}>
      <Header small />
      <div className="page-hero__content">
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
    </section>
  );
}
