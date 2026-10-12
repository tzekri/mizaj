# Mizaj Coffee — مزاج

Site vitrine d'une marque de café (Casablanca), inspiré de la structure de buncoffee.ma.

- **Stack** : React 18 + TypeScript + Vite + React Router (aucun backend)
- **Pages** : Accueil, Menu (bannières par catégorie + recherche, inspiré de buncoffee.ma/bun-menu), une page par catégorie `/menu/:slug` (fiches avec photo, prix, intensité, contenance), Nos coffee shops, Devenez franchisé (formulaire → e-mail)
- **Contenu** : tout est dans [`src/config.ts`](src/config.ts) — coordonnées, coffee shops, produits, menu, slides. Les données sont **fictives**.
- **Couleurs** : variables CSS en haut de [`src/styles.css`](src/styles.css) (`--accent` = grenat `#7b1e2b`, `--accent-on-dark` pour les textes sur fond sombre).
- **Photos** : Unsplash (licence libre), chargées depuis images.unsplash.com.

## Lancer

```bash
npm install
npm run dev      # http://localhost:5175
npm run build    # sortie dans dist/
```
