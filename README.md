# Kidima Coffee House

Site d'une marque de café (Casablanca), inspiré de la structure de buncoffee.ma, avec des **abonnements** :
le client choisit sa formule, reçoit sa carte QR dans son téléphone et la fait scanner à chaque café.

- **Frontend** (racine du dépôt) : React 18 + TypeScript + Vite + React Router.
- **Backend** ([`backend/`](backend)) : Spring Boot 3.3 / Java 21, PostgreSQL (H2 en local), JWT HS256.
- **Pages** : Accueil, Menu (bannières par catégorie + recherche) et `/menu/:slug`, Abonnement, Ma carte, Espace barista,
  Nos coffee shops (la partie « Devenez franchisé » est masquée pour le moment : `FRANCHISE_ENABLED` dans `src/config.ts`).
- **Contenu** : [`src/config.ts`](src/config.ts) — coordonnées, coffee shops, menu, slides. Les données sont **fictives**.
- **Couleurs** : variables CSS en haut de [`src/styles.css`](src/styles.css) (couleurs du logo : marine `--accent` `#10284a`, or `--accent-on-dark` `#d9973f`, fond crème `--page` `#faf5ec`).
- **Logo** : [`public/brand/`](public/brand) — `logo.png` (couleur, fond clair), `logo-light.png` (crème + or, fond sombre),
  `logo-mark*.png` (sans signature), `icon*.png`, favicons ; `storefront.jpg` (devanture).
- **Photos** : Unsplash (licence libre), chargées depuis images.unsplash.com.

## Abonnement

| Étape | Qui | Où |
|---|---|---|
| Scanne l'affiche (`/affiche`), choisit sa formule, saisit son numéro → carte QR créée immédiatement | Client | `/abonnement` → `/ma-carte` |
| Nouveau téléphone / carte perdue : le barista donne en caisse un code de récupération (6 chiffres, 10 min) que le client saisit | Barista + client | `/barista` → `/abonnement` « J'ai déjà une carte » |
| Envoie gratuitement sa carte (image) sur WhatsApp depuis « Ma carte » (partage du téléphone, sans Twilio) | Client | `/ma-carte` |
| Scanne le QR, encaisse la formule (modifiable au comptoir), clique « Activer » | Barista | `/barista` |
| À chaque boisson : scan du QR → « Valider 1 … » → −1 | Barista | `/barista` |
| La carte se met à jour seule (rafraîchie toutes les 5 s) | Client | `/ma-carte` |

Règles :
- Les espressos non consommés à l'échéance **ne sont pas reportés** ; un renouvellement anticipé (confirmé par le barista) clôt la période en cours.
- Deux validations à moins de 2 minutes demandent une confirmation (double scan) ; une validation peut être annulée pendant 10 minutes.
- Le QR contient un jeton aléatoire (pas le numéro). Le client peut le régénérer : l'ancien ne fonctionne plus.
- **Pas de code à l'inscription** (sans Twilio) : un numéro déjà inscrit ne s'ouvre jamais à distance, seulement avec le code de récupération donné en caisse (usage unique, 10 min, 5 essais). Inscriptions limitées à 40 / heure / IP (Wi-Fi partagé du coffee shop).
- Si Twilio est configuré : code envoyé à l'inscription (10 min, 5 essais, 60 s entre deux envois, 5 / heure / numéro, 15 / heure / IP).
- Formules en base, gérées dans Administration (prix, boissons, durée, visibilité). Au premier démarrage : Découverte (50 DH, 10 espressos, 15 j), Espresso (135 DH, 30 espressos, 30 j), Lait & Douceur (220 DH, 30 boissons au lait, 30 j).

Rôles : `BARISTA` (comptoir) et `ADMIN` (comptoir + statistiques, formules, abonnés, gestion du personnel).

## Compose ton jus

Page `/jus` (ouverte à tous) : **1 base** + jusqu'à **2 fruits** + jusqu'à **2 extras**, en **33 cl** (15 DH) ou **50 cl** (20 DH),
plus le supplément de chaque ingrédient (×1,4 en 50 cl). Aperçu du verre aux couleurs des fruits, prix en direct.

- « Obtenir mon code » → code de 6 caractères + QR (`MIZAJE-JUS:…`), valable 48 h, **à usage unique**.
- Comptoir : le même scanner lit les cartes et les jus (ou saisie du code) → recette, format, prix → « Servi ».
- Abonnés : jusqu'à 10 **favoris**, recommandés en un geste depuis « Ma carte » (au prix actuel).
- Administration → « Jus composés » : ingrédients (supplément, couleur, disponibilité, ajout), jus servis et mix les plus servis sur 30 jours.

## Menu & prix (administration)

Espace barista → compte admin → onglet **« Menu & prix »** :
- **Prix des jus composés** : mode **prix fixe** par format (par défaut : 15 DH en 33 cl, quels que soient les fruits) ou **prix selon les ingrédients** (prix du format + suppléments × coefficient, calcul conservé).
- **Menu du coffee shop** : prix modifiable directement dans le tableau, produits (nom, description, contenance, intensité,
  photo Unsplash ou https, pastille, « Produits populaires » de l’accueil), disponibilité, ordre (↑ ↓), catégories.

Le menu est en base (`menu_categories`, `menu_items`) : au premier démarrage il est chargé depuis
`backend/src/main/resources/seed/menu.json`, puis seuls les changements faits dans l’administration comptent.
API : `GET /api/public/menu`, `/api/admin/menu/**`, `PUT /api/admin/juice/formats/{S33|S50}`.

### Option : code de validation par Twilio Verify

Par défaut, l'inscription se fait **sans code**. Si Twilio est configuré, un code est envoyé par **Twilio Verify** : Twilio génère, envoie et vérifie le code (anti-fraude, limites d'envoi),
**WhatsApp d'abord, SMS en secours**. Le client peut aussi demander le SMS (« Pas de WhatsApp ? »).
Sans configuration Twilio : inscription directe, récupération de carte en caisse.

Mise en place :

1. Compte Twilio → **Verify → Services → Create** : nom « Kidima Coffee House », canaux **SMS** et **WhatsApp** → copier le SID `VA…`.
2. Sur Render (service `mizaj` → Environment) : `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_VERIFY_SID`.
   Le code est alors demandé automatiquement à l'inscription. **Le SMS fonctionne immédiatement**
   (vérifier dans Verify → Geo permissions que le Maroc est autorisé).
3. WhatsApp (optionnel, moins cher que le SMS) : Verify exige **votre propre expéditeur WhatsApp**
   (« Bring Your Own WhatsApp Sender ») : compte WhatsApp Business + numéro dans Twilio (Messaging → Senders → WhatsApp),
   rattaché à un Messaging Service, lui-même sélectionné dans l'onglet *WhatsApp* du service Verify.
   Tant que ce n'est pas fait, Twilio refuse le canal WhatsApp et le site bascule automatiquement sur le SMS.

Ordre des canaux : `TWILIO_VERIFY_CHANNELS` (défaut `whatsapp,sms` ; `sms` seul pour n'utiliser que le SMS).
Alternative sans Verify : `TWILIO_WHATSAPP_FROM` + `TWILIO_CONTENT_SID` (API Messages, modèle WhatsApp à faire approuver).

## Lancer en local

```bash
npm install
npm run dev                       # site : http://localhost:5175 (proxy /api → 8095)
```

```bash
cd backend
./mvnw -DskipTests package
java -jar target/mizaje-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local   # API : http://localhost:8095 (H2 en mémoire)
```

Comptes de démonstration : `admin@kidima.ma` / `mizaj1234`, `barista@kidima.ma` / `barista1234`.
La caméra du scanner exige HTTPS (ou localhost).

## Déploiement (Render)

`render.yaml` crée la base PostgreSQL `mizaj-db` et le service Docker `mizaj` (https://mizaj.onrender.com) (le [`Dockerfile`](Dockerfile)
construit le site et l'embarque dans le jar Spring Boot). Render demande `ADMIN_EMAIL` / `ADMIN_PASSWORD` à la création.
La base est en plan payant `basic-256mb` (pas d'expiration) : Render n'autorise qu'une base gratuite par compte, déjà utilisée par Nobty.
