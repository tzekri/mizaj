# Mizaj Coffee — مزاج

Site d'une marque de café (Casablanca), inspiré de la structure de buncoffee.ma, avec des **abonnements** :
le client choisit sa formule, reçoit sa carte QR dans son téléphone et la fait scanner à chaque café.

- **Frontend** (racine du dépôt) : React 18 + TypeScript + Vite + React Router.
- **Backend** ([`backend/`](backend)) : Spring Boot 3.3 / Java 21, PostgreSQL (H2 en local), JWT HS256.
- **Pages** : Accueil, Menu (bannières par catégorie + recherche) et `/menu/:slug`, Abonnement, Ma carte, Espace barista,
  Nos coffee shops, Devenez franchisé.
- **Contenu** : [`src/config.ts`](src/config.ts) — coordonnées, coffee shops, menu, slides. Les données sont **fictives**.
- **Couleurs** : variables CSS en haut de [`src/styles.css`](src/styles.css) (`--accent` = grenat `#7b1e2b`).
- **Photos** : Unsplash (licence libre), chargées depuis images.unsplash.com.

## Abonnement

| Étape | Qui | Où |
|---|---|---|
| Scanne l'affiche (`/affiche`), choisit sa formule, saisit son numéro, valide le code WhatsApp → compte + carte QR | Client | `/abonnement` → `/ma-carte` |
| Envoie gratuitement sa carte (image) sur WhatsApp depuis « Ma carte » (partage du téléphone, sans Twilio) | Client | `/ma-carte` |
| Scanne le QR, encaisse la formule (modifiable au comptoir), clique « Activer » | Barista | `/barista` |
| À chaque boisson : scan du QR → « Valider 1 … » → −1 | Barista | `/barista` |
| La carte se met à jour seule (rafraîchie toutes les 5 s) | Client | `/ma-carte` |

Règles :
- Les espressos non consommés à l'échéance **ne sont pas reportés** ; un renouvellement anticipé (confirmé par le barista) clôt la période en cours.
- Deux validations à moins de 2 minutes demandent une confirmation (double scan) ; une validation peut être annulée pendant 10 minutes.
- Le QR contient un jeton aléatoire (pas le numéro). Le client peut le régénérer : l'ancien ne fonctionne plus.
- Code WhatsApp : 10 min de validité, 5 essais, 60 s entre deux envois, 5 envois / heure / numéro, 15 / heure / IP.
- Formules en base, gérées dans Administration (prix, boissons, durée, visibilité). Au premier démarrage : Découverte (45 DH, 10 espressos, 15 j), Espresso (120 DH, 30 espressos, 30 j), Lait & Douceur (220 DH, 30 boissons au lait, 30 j).

Rôles : `BARISTA` (comptoir) et `ADMIN` (comptoir + statistiques, formules, abonnés, gestion du personnel).

### WhatsApp (Twilio)

Tant que `TWILIO_*` n'est pas renseigné, le code est **affiché à l'écran** (`OTP_DEV_MODE=true`) : pratique pour la démo,
**à désactiver** en production. Pour l'activer :

1. Compte Twilio → Messaging → WhatsApp : sandbox pour tester, puis un expéditeur WhatsApp Business validé.
2. Créer un modèle de message de type *Authentication* (Content Template Builder) avec la variable `{{1}}` = code,
   le faire approuver par WhatsApp → récupérer son **Content SID** (`HX…`).
3. Renseigner sur Render : `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_FROM` (ex. `whatsapp:+212…`),
   `TWILIO_CONTENT_SID`, puis `OTP_DEV_MODE=false`.

Sans `TWILIO_CONTENT_SID`, un message texte libre est envoyé : accepté uniquement par la sandbox.

## Lancer en local

```bash
npm install
npm run dev                       # site : http://localhost:5175 (proxy /api → 8095)
```

```bash
cd backend
./mvnw -DskipTests package
java -jar target/mizaj-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local   # API : http://localhost:8095 (H2 en mémoire)
```

Comptes de démonstration : `admin@mizajcoffee.ma` / `mizaj1234`, `barista@mizajcoffee.ma` / `barista1234`.
La caméra du scanner exige HTTPS (ou localhost).

## Déploiement (Render)

`render.yaml` crée la base PostgreSQL `mizaj-db` et le service Docker `mizaj` (https://mizaj.onrender.com) (le [`Dockerfile`](Dockerfile)
construit le site et l'embarque dans le jar Spring Boot). Render demande `ADMIN_EMAIL` / `ADMIN_PASSWORD` à la création.
La base est en plan payant `basic-256mb` (pas d'expiration) : Render n'autorise qu'une base gratuite par compte, déjà utilisée par Nobty.
