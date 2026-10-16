import { useEffect, useState } from 'react';
import { ApiError, api, type MenuCategory } from './api';
import { img } from './config';

// Menu public, chargé une fois par visite et partagé entre les pages (accueil, menu, catégories).
let cache: MenuCategory[] | null = null;
let pending: Promise<MenuCategory[]> | null = null;

export function useMenu() {
  const [menu, setMenu] = useState<MenuCategory[] | null>(cache);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (cache) return;
    let alive = true;
    (pending ??= api.menu())
      .then((m) => {
        cache = m;
        if (alive) setMenu(m);
      })
      .catch((e) => {
        pending = null;
        if (alive) setError((e as ApiError).message);
      });
    return () => {
      alive = false;
    };
  }, []);

  return { menu, error };
}

/** Après une modification dans l'administration : le site rechargera le menu à jour. */
export function invalidateMenu() {
  cache = null;
  pending = null;
}

/** Photo d'un produit ou d'une catégorie : identifiant Unsplash ou adresse https complète. */
export const menuPhoto = (photo: string, w = 1200) => (photo.startsWith('http') ? photo : img(photo, w));
