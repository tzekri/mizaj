import { StrictMode, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { Floating, Footer } from './components/Layout';
import Home from './pages/Home';
import Menu from './pages/Menu';
import MenuCategory from './pages/MenuCategory';
import Shops from './pages/Shops';
import Franchise from './pages/Franchise';
import Subscribe from './pages/Subscribe';
import MyCard from './pages/MyCard';
import Barista from './pages/Barista';
import Poster from './pages/Poster';
import './styles.css';

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
          <Route path="/franchise" element={<Franchise />} />
          <Route path="/abonnement" element={<Subscribe />} />
          <Route path="/ma-carte" element={<MyCard />} />
          <Route path="/barista" element={<Barista />} />
          <Route path="/affiche" element={<Poster />} />
          <Route path="*" element={<Home />} />
        </Routes>
      </main>
      <Footer />
      <Floating />
    </BrowserRouter>
  </StrictMode>,
);
