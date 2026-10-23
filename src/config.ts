// Contenu du site — coordonnées fictives, à remplacer par les vraies.

/** Partie franchise (page, liens, encart d'accueil) : masquée pour le moment, passer à true pour la réafficher. */
export const FRANCHISE_ENABLED = false;

export const BRAND = {
  name: 'Kidima',
  tagline: 'Le café à la marocaine',
  arabicTagline: 'قهوتك على مزاجك',
  since: 2024,
  phone: '+212 6 00 00 00 00',
  whatsapp: '212600000000',
  email: 'contact@kidima.ma',
  instagram: 'https://www.instagram.com/',
  tiktok: 'https://www.tiktok.com/',
};

// Photos Unsplash (licence libre), référencées par identifiant.
export const img = (id: string, w = 1200) =>
  `https://images.unsplash.com/photo-${id}?auto=format&fit=crop&w=${w}&q=80`;

export const PHOTOS = {
  heroBeans: '1789700736833-9ae7f8ee71cd',
  beansTamper: '1789314187992-63ad3e090d3c',
  roaster: '1777383505374-3c8d2afcac4d',
  smokeCup: '1764361276489-79b17d9a8782',
  latteDark: '1770494347735-4a6b50086124',
  espressoPour: '1786431851384-b3ebeffe7946',
  chemex: '1761393877623-4412d7060ba4',
  extraction: '1782672473485-106941f087d9',
  machine: '1637029567716-6c6775c341e5',
  roasterVintage: '1511537190424-bbbab87ac5eb',
  barDark: '1709548145082-04d0cde481d4',
  barGreen: '1728761390316-935ffeb3fbcc',
  barMinimal: '1745878134951-65bde84348f7',
  icedPour: '1592663527359-cf6642f54cff',
};

export const SHOPS = [
  { name: 'Maârif', address: '42, Rue Abou Al Waqt', city: 'Maârif, Casablanca', hours: '7h00 – 23h00', photo: PHOTOS.barDark },
  { name: 'Ain Diab', address: '15, Boulevard de la Corniche', city: 'Ain Diab, Casablanca', hours: '7h00 – 01h00', photo: PHOTOS.barGreen },
  { name: 'Bourgogne', address: '88, Rue Ibn Toufail', city: 'Bourgogne, Casablanca', hours: '6h30 – 22h00', photo: PHOTOS.barMinimal },
];

// Le menu (catégories, produits, prix) est géré dans l'administration : voir src/menu.ts et /api/public/menu.
