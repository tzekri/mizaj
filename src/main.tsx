import { StrictMode, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { Floating, Footer } from './components/Layout';
import Home from './pages/Home';
import Menu from './pages/Menu';
import MenuCategory from './pages/MenuCategory';
import Shops from './pages/Shops';
import Franchise from './pages/Franchise';
import Chrono from './pages/Chrono';
import ChronoScreen from './pages/ChronoScreen';
import { FRANCHISE_ENABLED } from './config';
import Subscribe from './pages/Subscribe';
import MyCard from './pages/MyCard';
import Barista from './pages/Barista';
import Poster from './pages/Poster';
import Juice from './pages/Juice';
import './styles.css';

/** Pied de page et boutons flottants, sauf sur l'écran géant du défi (TV en plein écran). */
function SiteChrome({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  return pathname.startsWith('/defi/ecran') ? null : <>{children}</>;
}

function ScrollTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <ScrollTop />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/menu" element={<Menu />} />
          <Route path="/menu/:slug" element={<MenuCategory />} />
          <Route path="/nos-cafes" element={<Shops />} />
          {FRANCHISE_ENABLED && <Route path="/franchise" element={<Franchise />} />}
          <Route path="/abonnement" element={<Subscribe />} />
          <Route path="/ma-carte" element={<MyCard />} />
          <Route path="/barista" element={<Barista />} />
          <Route path="/affiche" element={<Poster />} />
          <Route path="/jus" element={<Juice />} />
          <Route path="/defi" element={<Chrono />} />
          <Route path="/defi/ecran" element={<ChronoScreen />} />
          <Route path="*" element={<Home />} />
        </Routes>
      </main>
      <SiteChrome>
        <Footer />
        <Floating />
      </SiteChrome>
    </BrowserRouter>
  </StrictMode>,
);
