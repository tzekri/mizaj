// Contenu du site — coordonnées fictives, à remplacer par les vraies.

export const BRAND = {
  name: 'Mizaj',
  arabic: 'مزاج',
  tagline: 'Enjoy Your Mizaj',
  arabicTagline: 'قهوة على مزاجك',
  since: 2024,
  phone: '+212 6 00 00 00 00',
  whatsapp: '212600000000',
  email: 'contact@mizajcoffee.ma',
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

export type MenuItem = {
  name: string;
  desc: string;
  price: number;
  photo: string;
  intensity?: number; // 1 à 5
  volume?: string;
};

export type MenuCategory = {
  slug: string;
  light: string; // mot fin du titre (ex. « Hot »)
  bold: string; // mot gras du titre (ex. « Coffee »)
  arabic: string;
  tone: 'cream' | 'accent' | 'dark';
  photo: string;
  items: MenuItem[];
};

export const MENU: MenuCategory[] = [
  {
    slug: 'coffee',
    light: 'Espresso',
    bold: 'Coffee',
    arabic: 'إسبريسو',
    tone: 'cream',
    photo: PHOTOS.extraction,
    items: [
      { name: 'Espresso', desc: 'Notre assemblage signature, corsé et intense, crema noisette.', price: 10, intensity: 5, volume: '30 ml', photo: '1612183515105-8c737ba7585d' },
      { name: 'Ristretto', desc: 'Extraction courte, concentrée en arômes, finale chocolatée.', price: 12, intensity: 5, volume: '20 ml', photo: PHOTOS.espressoPour },
      { name: 'Américano', desc: 'Espresso allongé d’eau chaude, doux et aromatique.', price: 14, intensity: 3, volume: '200 ml', photo: '1630338606772-392095666116' },
      { name: 'Macchiato', desc: 'Espresso tâché d’une touche de mousse de lait.', price: 13, intensity: 4, volume: '60 ml', photo: '1543336179-44da3606afa4' },
      { name: 'Cortado', desc: 'Moitié espresso, moitié lait chaud. L’équilibre parfait.', price: 16, intensity: 4, volume: '120 ml', photo: '1741261524028-cd200446df20' },
      { name: 'Café turc', desc: 'Préparé au cezve, mouture extra-fine, parfum de cardamome.', price: 20, intensity: 5, volume: '80 ml', photo: '1664398346444-f6feafaa88b6' },
    ],
  },
  {
    slug: 'hot-coffee',
    light: 'Hot',
    bold: 'Coffee',
    arabic: 'قهوة ساخنة',
    tone: 'accent',
    photo: '1763208387697-fe0a913bfed9',
    items: [
      { name: 'Cappuccino', desc: 'Café mousseux avec une fine mousse de lait. Un grand classique équilibré.', price: 20, intensity: 3, volume: '180 ml', photo: '1558220829-4694a46bb01f' },
      { name: 'Café Latte', desc: 'Espresso et lait onctueux, latte art réalisé par nos baristas.', price: 22, intensity: 2, volume: '250 ml', photo: '1763208387697-fe0a913bfed9' },
      { name: 'Flat White', desc: 'Double ristretto et micro-mousse soyeuse, plus de café, moins de lait.', price: 22, intensity: 4, volume: '160 ml', photo: '1620052087057-bfd8235f5874' },
      { name: 'Spanish Latte', desc: 'Latte légèrement sucré au lait concentré, crémeux et réconfortant.', price: 28, intensity: 2, volume: '250 ml', photo: PHOTOS.latteDark },
      { name: 'Caramel Latte', desc: 'Latte chaud au caramel beurre salé maison.', price: 26, intensity: 2, volume: '250 ml', photo: '1760630691241-7c8aa2e680ce' },
      { name: 'Mocaccino', desc: 'Mélange irrésistible de café et de chocolat noir. Riche et gourmand.', price: 26, intensity: 3, volume: '250 ml', photo: '1618576230663-9714aecfb99a' },
      { name: 'Nouss-Nouss', desc: 'Le classique marocain : moitié café, moitié lait.', price: 12, intensity: 3, volume: '150 ml', photo: '1497636577773-f1231844b336' },
    ],
  },
  {
    slug: 'ice-coffee',
    light: 'Ice',
    bold: 'Coffee',
    arabic: 'قهوة مثلجة',
    tone: 'dark',
    photo: PHOTOS.icedPour,
    items: [
      { name: 'Iced Latte', desc: 'Espresso, lait frais et glaçons. Simple et rafraîchissant.', price: 24, intensity: 2, volume: '350 ml', photo: '1764298490445-6add12301888' },
      { name: 'Iced Spanish Latte', desc: 'Notre Spanish latte servi sur glace, doux et crémeux.', price: 30, intensity: 2, volume: '350 ml', photo: '1527156231393-7023794f363c' },
      { name: 'Iced Caramel Macchiato', desc: 'Lait vanillé, espresso et filet de caramel sur glace.', price: 30, intensity: 2, volume: '350 ml', photo: '1527678357412-ef45dfbd9ecc' },
      { name: 'Iced Americano', desc: 'Double espresso allongé d’eau fraîche sur glaçons.', price: 20, intensity: 4, volume: '350 ml', photo: '1680327284937-1e1cb66c3ac7' },
      { name: 'Cold Brew', desc: 'Infusé à froid pendant 18 heures. Notes de cacao, faible acidité.', price: 25, intensity: 4, volume: '330 ml', photo: '1531835207745-506a1bc035d8' },
      { name: 'Frappé Caramel', desc: 'Café glacé mixé, caramel et crème fouettée.', price: 32, intensity: 2, volume: '400 ml', photo: '1579888071069-c107a6f79d82' },
    ],
  },
  {
    slug: 'milkshake',
    light: 'Fresh',
    bold: 'Milkshake',
    arabic: 'ميلك شيك',
    tone: 'cream',
    photo: '1577805947697-89e18249d767',
    items: [
      { name: 'Milkshake Oreo', desc: 'Glace vanille, biscuits Oreo et chantilly.', price: 32, volume: '400 ml', photo: '1572490122747-3968b75cc699' },
      { name: 'Milkshake Chocolat', desc: 'Chocolat noir fondant, glace et éclats de cacao.', price: 30, volume: '400 ml', photo: '1577805947697-89e18249d767' },
      { name: 'Milkshake Fraise', desc: 'Fraises fraîches mixées avec glace vanille.', price: 30, volume: '400 ml', photo: '1579954115545-a95591f28bfc' },
      { name: 'Milkshake Vanille', desc: 'Vanille de Madagascar, lait entier, crème fouettée.', price: 28, volume: '400 ml', photo: '1568901839119-631418a3910d' },
      { name: 'Milkshake Brownie', desc: 'Brownie maison, sauce chocolat et chantilly.', price: 35, volume: '400 ml', photo: '1619158403521-ed9795026d47' },
    ],
  },
  {
    slug: 'cocktail',
    light: 'Fresh',
    bold: 'Cocktail',
    arabic: 'كوكتيل',
    tone: 'accent',
    photo: '1654074517750-f854f7c27d62',
    items: [
      { name: 'Virgin Mojito', desc: 'Menthe fraîche, citron vert, sucre de canne et eau pétillante.', price: 28, volume: '400 ml', photo: '1634496064950-02f043806b09' },
      { name: 'Red Berries', desc: 'Fruits rouges, citron et romarin.', price: 30, volume: '400 ml', photo: '1595981267035-7b04ca84a82d' },
      { name: 'Purple Mizaj', desc: 'Thé butterfly pea, citron et sirop de lavande. Change de couleur !', price: 32, volume: '400 ml', photo: '1595977514600-72cbc8376c38' },
      { name: 'Citronnade Menthe', desc: 'Citrons pressés, menthe et une pointe de fleur d’oranger.', price: 22, volume: '400 ml', photo: '1575596510825-f748919a2bf7' },
      { name: 'Jus d’orange pressé', desc: 'Oranges du Souss pressées minute.', price: 20, volume: '330 ml', photo: '1613478223719-2ab802602423' },
    ],
  },
  {
    slug: 'tea',
    light: 'Thé &',
    bold: 'Infusions',
    arabic: 'أتاي',
    tone: 'dark',
    photo: '1591299089616-c9604047b1a6',
    items: [
      { name: 'Atay à la menthe', desc: 'Le thé marocain : gunpowder, menthe fraîche, servi à la théière.', price: 15, volume: 'Théière', photo: '1591299089616-c9604047b1a6' },
      { name: 'Atay Royal', desc: 'Menthe, chiba et verveine, servi avec le service traditionnel.', price: 25, volume: 'Théière', photo: '1518439532222-5dcc881e74b8' },
      { name: 'Thé noir', desc: 'Ceylan sélectionné, infusé à la minute.', price: 15, volume: '300 ml', photo: '1546641555-20676239fb5d' },
      { name: 'Chai Latte', desc: 'Thé noir épicé (cannelle, gingembre, cardamome) et lait mousseux.', price: 24, volume: '300 ml', photo: '1661499102718-aebb4886a0bc' },
      { name: 'Matcha Latte', desc: 'Matcha japonais fouetté et lait onctueux.', price: 30, volume: '250 ml', photo: '1515823064-d6e0c04616a7' },
      { name: 'Infusion Bien-être', desc: 'Verveine, camomille ou tilleul, au choix.', price: 15, volume: '300 ml', photo: '1558160074-4d7d8bdf4256' },
    ],
  },
  {
    slug: 'ice-tea',
    light: 'Ice',
    bold: 'Tea',
    arabic: 'شاي مثلج',
    tone: 'cream',
    photo: '1556679343-c7306c1976bc',
    items: [
      { name: 'Ice Tea Citron', desc: 'Thé noir infusé à froid, citron vert et menthe.', price: 22, volume: '400 ml', photo: '1556679343-c7306c1976bc' },
      { name: 'Ice Tea Pêche', desc: 'Thé glacé à la pêche blanche, peu sucré.', price: 24, volume: '400 ml', photo: '1616434963473-030c8f1ce663' },
      { name: 'Ice Tea Fruits Rouges', desc: 'Thé noir, framboise et cassis.', price: 24, volume: '400 ml', photo: '1601390395693-364c0e22031a' },
      { name: 'Hibiscus Glacé', desc: 'Bissap infusé, gingembre et citron. Acidulé et désaltérant.', price: 22, volume: '400 ml', photo: '1654922704274-cd34f165c5e7' },
      { name: 'Iced Matcha', desc: 'Matcha cérémonial sur lait froid et glaçons.', price: 32, volume: '350 ml', photo: '1717603545560-28f9da83ecc3' },
    ],
  },
  {
    slug: 'cafe-en-grain',
    light: 'Café en',
    bold: 'Grain',
    arabic: 'بن',
    tone: 'accent',
    photo: PHOTOS.heroBeans,
    items: [
      { name: 'Mizaj Signature', desc: 'Grains 250 g — Arabica 80 % / Robusta 20 %. Chocolat, noisette.', price: 75, intensity: 4, volume: '250 g', photo: '1562051036-e0eea191d42f' },
      { name: 'Arabica Éthiopie', desc: 'Grains 250 g — 100 % Arabica Yirgacheffe. Floral, agrumes.', price: 95, intensity: 2, volume: '250 g', photo: '1657037721632-4f0b6d07b45a' },
      { name: 'Mizaj Intense', desc: 'Moulu 250 g — torréfaction foncée, idéal pour l’espresso.', price: 65, intensity: 5, volume: '250 g', photo: '1606791405792-1004f1718d0c' },
      { name: 'Décaféiné', desc: 'Grains 250 g — décaféiné à l’eau, tout le goût sans la caféine.', price: 80, intensity: 3, volume: '250 g', photo: '1605711529603-d3a7d001fa5e' },
    ],
  },
  {
    slug: 'viennoiseries',
    light: 'Nos',
    bold: 'Viennoiseries',
    arabic: 'معجنات',
    tone: 'dark',
    photo: '1555507036-ab1f4038808a',
    items: [
      { name: 'Croissant pur beurre', desc: 'Feuilletage maison, cuit chaque matin.', price: 10, photo: '1555507036-ab1f4038808a' },
      { name: 'Pain au chocolat', desc: 'Deux barres de chocolat noir dans une pâte feuilletée.', price: 12, photo: '1564354273277-c6d4b8532100' },
      { name: 'Cinnamon Roll', desc: 'Brioche roulée à la cannelle, glaçage cream cheese.', price: 22, photo: '1645995575875-ea6511c9d127' },
      { name: 'Muffin', desc: 'Myrtilles ou chocolat, moelleux à cœur.', price: 18, photo: '1637861004744-0f846ef2df40' },
      { name: 'Cookie', desc: 'Pépites de chocolat noir et fleur de sel.', price: 15, photo: '1499636136210-6f4ee915583e' },
      { name: 'Cheesecake', desc: 'New York cheesecake, coulis de fruits rouges.', price: 35, photo: '1524351199678-941a58a3df50' },
    ],
  },
];

export const findItem = (slug: string, name: string) =>
  MENU.find((c) => c.slug === slug)?.items.find((i) => i.name === name);

// Produits mis en avant sur l'accueil (références au menu).
export const POPULAR = [
  { slug: 'cafe-en-grain', name: 'Mizaj Signature', tag: 'Best-seller' },
  { slug: 'hot-coffee', name: 'Spanish Latte' },
  { slug: 'ice-coffee', name: 'Cold Brew', tag: 'Nouveau' },
  { slug: 'tea', name: 'Atay à la menthe' },
];

export const SLIDES = [
  {
    title: 'Time to Drink Coffee',
    text: 'Chaque mardi, votre espresso est à seulement 5 dh ! Venez découvrir nos origines du moment, discuter avec nos baristas et vivre une expérience unique à chaque visite.',
    photo: PHOTOS.espressoPour,
  },
  {
    title: 'Taste and Aroma',
    text: 'Un Cold Brew pour les chaudes journées d’été ou un filtre parfait à partir de grains fraîchement torréfiés : nos cafés artisanaux sont toujours à l’honneur.',
    photo: PHOTOS.chemex,
  },
  {
    title: 'Fresh Every Morning',
    text: 'Nos grains sont torréfiés chaque semaine à Casablanca, en petits lots, pour garantir une fraîcheur et une constance irréprochables dans votre tasse.',
    photo: PHOTOS.roasterVintage,
  },
];
