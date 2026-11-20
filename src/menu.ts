import { useEffect, useState } from 'react';
import { ApiError, api, type MenuCategory, type MenuItem } from './api';
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

/** Les deux prix d'un produit avec leurs noms (« S » / « L »), ou un seul. */
export function prices(item: Pick<MenuItem, 'price' | 'price2' | 'priceLabels'>): { price: number; label?: string }[] {
  if (item.price2 == null) return [{ price: item.price }];
  const [a, b] = (item.priceLabels ?? '').split('|');
  return [{ price: item.price, label: a || undefined }, { price: item.price2, label: b || undefined }];
}

/** Prix en une ligne : « 12 / 16 DH ». */
export const priceText = (item: Pick<MenuItem, 'price' | 'price2' | 'priceLabels'>) =>
  `${prices(item).map((p) => p.price).join(' / ')} DH`;

/** Photo d'un produit ou d'une catégorie : identifiant Unsplash ou adresse https complète. */
export const menuPhoto = (photo: string, w = 1200) => (photo.startsWith('http') ? photo : img(photo, w));
