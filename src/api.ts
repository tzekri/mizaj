// Client de l'API Spring Boot (même domaine en production, proxy Vite en développement).

export type Status = 'NONE' | 'ACTIVE' | 'EMPTY' | 'EXPIRED';
export type Plan = {
  code: string;
  name: string;
  description?: string;
  price: number;
  drinks: number;
  days: number;
  unit: string;
  unitPlural: string;
  active: boolean;
};
export type PlanInput = Omit<Plan, 'code'>;

export type Card = {
  firstName?: string;
  phone: string;
  qrToken: string;
  status: Status;
  drinksLeft: number;
  drinksTotal: number;
  activatedAt?: string;
  expiresAt?: string;
  plan: Plan;
  history: { at: string; shop?: string }[];
};

export type OtpStart = {
  phone: string;
  newAccount: boolean;
  expiresInSeconds: number;
  resendInSeconds: number;
  channel: 'whatsapp' | 'sms';
  smsAvailable: boolean;
};

export type StaffRole = 'BARISTA' | 'ADMIN';
export type StaffSession = { token: string; name: string; role: StaffRole; shop?: string };

export type StaffCard = {
  customerId: number;
  firstName?: string;
  phone: string;
  status: Status;
  drinksLeft: number;
  drinksTotal: number;
  activatedAt?: string;
  expiresAt?: string;
  lastRedemptionAt?: string;
  undoableRedemptionId?: number;
  memberSince: string;
  plan: Plan;
  preferredPlan: Plan;
};

export type Today = { shop?: string; redemptions: number; activations: number };
export type Stats = {
  customers: number;
  activeSubscriptions: number;
  revenueThisMonth: number;
  activationsThisMonth: number;
  redemptionsToday: number;
  redemptionsThisMonth: number;
};
export type Subscriber = {
  customerId: number;
  firstName?: string;
  phone: string;
  planName?: string;
  status: Status;
  drinksLeft: number;
  expiresAt: string;
};
export type StaffMember = { id: number; fullName: string; email: string; role: StaffRole; shop?: string; active: boolean };

export type DashboardKpis = {
  revenue: number;
  activations: number;
  firstActivations: number;
  renewals: number;
  redemptions: number;
  newCustomers: number;
  activeNow: number;
  /** Absent (null) quand aucun abonnement n'est arrivé à échéance sur la période. */
  consumptionRate?: number | null;
  renewalRate?: number | null;
  redemptionsPerDay: number;
};

export type DashboardData = {
  from: string;
  to: string;
  shop?: string;
  shops: string[];
  kpis: DashboardKpis;
  previous: DashboardKpis;
  daily: { date: string; redemptions: number; activations: number; revenue: number }[];
  monthly: { month: string; revenue: number; revenueByPlan: Record<string, number> }[];
  plans: { code: string; name: string; price: number; activeNow: number; activations: number; revenue: number; redemptions: number; consumptionRate?: number | null }[];
  shopStats: { shop: string; redemptions: number; activations: number; revenue: number }[];
  heatmap: number[][];
  followUp: { customerId: number; firstName?: string; phone: string; planName?: string; drinksLeft: number; drinksTotal: number; expiresAt: string; reason: 'EXPIRING' | 'LOW' }[];
  topCustomers: { customerId: number; firstName?: string; phone: string; planName?: string; redemptions: number }[];
};

// --- Composeur de jus ---

export type JuiceCategory = 'BASE' | 'FRUIT' | 'EXTRA';
export type JuiceSize = 'S33' | 'S50';
export type JuiceIngredient = {
  code: string;
  name: string;
  category: JuiceCategory;
  price: number;
  color: string;
  /** Photo (URL) ; absente → pastille de couleur. */
  image?: string;
  available: boolean;
};
export type JuiceCatalog = {
  ingredients: JuiceIngredient[];
  sizes: { code: JuiceSize; label: string; basePrice: number; factor: number }[];
  maxFruits: number;
  maxExtras: number;
};
export type JuiceRecipe = {
  name?: string;
  size: JuiceSize;
  sizeLabel: string;
  ingredients: JuiceIngredient[];
  label: string;
  price: number;
  available: boolean;
};
export type JuiceOrder = {
  code: string;
  qr: string;
  recipe: JuiceRecipe;
  createdAt: string;
  expiresAt: string;
  status: 'PENDING' | 'SERVED' | 'EXPIRED';
  servedAt?: string;
  shop?: string;
};
export type JuiceFavorite = { id: number; recipe: JuiceRecipe; createdAt: string };
export type RecipeInput = { name?: string; size: JuiceSize; ingredients: string[] };
export type JuiceStats = { served: number; revenue: number; created: number; topMixes: { label: string; sizeLabel: string; count: number }[] };

export class ApiError extends Error {
  constructor(public status: number, message: string, public code?: string) {
    super(message);
  }
}

// --- Sessions (stockage local, protégé contre un navigateur qui le refuse) ---

const CUSTOMER_KEY = 'mizaje.customer';
const STAFF_KEY = 'mizaje.staff';

function read(key: string): string | null {
  try {
    // Sessions ouvertes avant le passage au nom Mizaje (« mizaj.customer », « mizaj.staff ») : reprises une fois
    const legacy = key.replace(/^mizaje\./, 'mizaj.');
    const old = localStorage.getItem(legacy);
    if (old !== null && localStorage.getItem(key) === null) {
      localStorage.setItem(key, old);
      localStorage.removeItem(legacy);
    }
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}
function write(key: string, value: string | null) {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {
    /* navigation privée : la session ne survivra pas au rechargement */
  }
}

export const customerToken = {
  get: () => read(CUSTOMER_KEY),
  set: (t: string | null) => write(CUSTOMER_KEY, t),
};

export const staffSession = {
  get: (): StaffSession | null => {
    const raw = read(STAFF_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StaffSession;
    } catch {
      return null;
    }
  },
  set: (s: StaffSession | null) => write(STAFF_KEY, s ? JSON.stringify(s) : null),
};

// --- Requêtes ---

async function request<T>(method: string, path: string, body?: unknown, token?: string | null): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  let res: Response;
  try {
    res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, 'Connexion impossible. Vérifiez votre réseau et réessayez.');
  }
  const isJson = res.headers.get('content-type')?.includes('application/json');
  const data = res.status === 204 || !isJson ? null : await res.json().catch(() => null);
  // Réponse non JSON (ex. page HTML d'un hébergement sans backend) : service indisponible, pas un plantage.
  if (res.ok && res.status !== 204 && data === null) {
    throw new ApiError(503, 'Le service d’abonnement est momentanément indisponible. Réessayez dans quelques instants.');
  }
  if (!res.ok) {
    const message =
      data?.message ??
      (res.status === 401 ? 'Session expirée, reconnectez-vous.' : res.status === 403 ? 'Accès refusé.' : 'Erreur, réessayez.');
    throw new ApiError(res.status, message, data?.code);
  }
  return data as T;
}

export const api = {
  plans: () => request<Plan[]>('GET', '/api/public/plans'),
  /** verification=false : inscription directe sans code (Twilio non configuré). */
  config: () => request<{ verification: boolean }>('GET', '/api/public/config'),
  signup: (phone: string, firstName?: string, planCode?: string) =>
    request<{ token: string; card: Card }>('POST', '/api/public/signup', { phone, firstName, planCode }),
  otpStart: (phone: string, firstName?: string, planCode?: string, channel?: 'whatsapp' | 'sms') =>
    request<OtpStart>('POST', '/api/public/otp/start', { phone, firstName, planCode, channel }),
  otpVerify: (phone: string, code: string) =>
    request<{ token: string; card: Card }>('POST', '/api/public/otp/verify', { phone, code }),

  me: () => request<Card>('GET', '/api/me', undefined, customerToken.get()),
  rotateQr: () => request<Card>('POST', '/api/me/qr/rotate', undefined, customerToken.get()),
  updateProfile: (p: { firstName?: string; planCode?: string }) => request<Card>('PATCH', '/api/me', p, customerToken.get()),

  staffLogin: (email: string, password: string) => request<StaffSession>('POST', '/api/auth/staff/login', { email, password }),
  scan: (token: string) => request<StaffCard>('POST', '/api/staff/scan', { token }, staffSession.get()?.token),
  lookup: (phone: string) =>
    request<StaffCard>('GET', `/api/staff/lookup?phone=${encodeURIComponent(phone)}`, undefined, staffSession.get()?.token),
  redeem: (id: number, force = false) =>
    request<StaffCard>('POST', `/api/staff/customers/${id}/redeem`, { force }, staffSession.get()?.token),
  activate: (id: number, planCode: string, confirmReplace = false) =>
    request<StaffCard>('POST', `/api/staff/customers/${id}/activate`, { planCode, confirmReplace }, staffSession.get()?.token),
  undo: (redemptionId: number) =>
    request<StaffCard>('POST', `/api/staff/redemptions/${redemptionId}/undo`, undefined, staffSession.get()?.token),
  recoveryCode: (customerId: number) =>
    request<{ code: string; expiresAt: string }>('POST', `/api/staff/customers/${customerId}/recovery-code`, undefined, staffSession.get()?.token),
  today: () => request<Today>('GET', '/api/staff/today', undefined, staffSession.get()?.token),

  adminPlans: () => request<Plan[]>('GET', '/api/admin/plans', undefined, staffSession.get()?.token),
  createPlan: (p: PlanInput) => request<Plan>('POST', '/api/admin/plans', p, staffSession.get()?.token),
  updatePlan: (code: string, p: PlanInput) => request<Plan>('PUT', `/api/admin/plans/${code}`, p, staffSession.get()?.token),
  dashboard: (from: string, to: string, shop?: string) =>
    request<DashboardData>(
      'GET',
      `/api/admin/dashboard?from=${from}&to=${to}${shop ? `&shop=${encodeURIComponent(shop)}` : ''}`,
      undefined,
      staffSession.get()?.token,
    ),
  /** Télécharge un export CSV (le jeton est envoyé dans l'en-tête, pas dans l'URL). */
  downloadCsv: async (kind: 'consommations' | 'abonnements', from: string, to: string, shop?: string) => {
    const query = kind === 'consommations' ? `?from=${from}&to=${to}${shop ? `&shop=${encodeURIComponent(shop)}` : ''}` : '';
    const res = await fetch(`/api/admin/export/${kind}.csv${query}`, {
      headers: { Authorization: `Bearer ${staffSession.get()?.token ?? ''}` },
    });
    if (!res.ok) throw new ApiError(res.status, res.status === 401 ? 'Session expirée, reconnectez-vous.' : 'Export impossible.');
    const name = /filename="([^"]+)"/.exec(res.headers.get('content-disposition') ?? '')?.[1] ?? `mizaje-${kind}.csv`;
    const url = URL.createObjectURL(await res.blob());
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
  },
  juiceCatalog: () => request<JuiceCatalog>('GET', '/api/public/juice'),
  /** Abonné connecté : la commande est rattachée à son compte ; sinon commande anonyme. */
  juiceOrder: (r: RecipeInput) =>
    customerToken.get()
      ? request<JuiceOrder>('POST', '/api/me/juices/order', r, customerToken.get())
      : request<JuiceOrder>('POST', '/api/public/juice/orders', r),
  juiceFavorites: () => request<JuiceFavorite[]>('GET', '/api/me/juices', undefined, customerToken.get()),
  addJuiceFavorite: (r: RecipeInput) => request<JuiceFavorite>('POST', '/api/me/juices', r, customerToken.get()),
  deleteJuiceFavorite: (id: number) => request<null>('DELETE', `/api/me/juices/${id}`, undefined, customerToken.get()),
  orderJuiceFavorite: (id: number) => request<JuiceOrder>('POST', `/api/me/juices/${id}/order`, undefined, customerToken.get()),
  juiceScan: (code: string) => request<JuiceOrder>('POST', '/api/staff/juice/scan', { code }, staffSession.get()?.token),
  juiceServe: (code: string) => request<JuiceOrder>('POST', `/api/staff/juice/${encodeURIComponent(code)}/serve`, undefined, staffSession.get()?.token),
  juiceIngredients: () => request<JuiceIngredient[]>('GET', '/api/admin/juice/ingredients', undefined, staffSession.get()?.token),
  saveJuiceIngredient: (i: Omit<JuiceIngredient, 'code'>, code?: string) =>
    code
      ? request<JuiceIngredient>('PUT', `/api/admin/juice/ingredients/${code}`, i, staffSession.get()?.token)
      : request<JuiceIngredient>('POST', '/api/admin/juice/ingredients', i, staffSession.get()?.token),
  juiceStats: (from: string, to: string) =>
    request<JuiceStats>('GET', `/api/admin/juice/stats?from=${from}&to=${to}`, undefined, staffSession.get()?.token),
  stats: () => request<Stats>('GET', '/api/admin/stats', undefined, staffSession.get()?.token),
  subscribers: () => request<Subscriber[]>('GET', '/api/admin/subscribers', undefined, staffSession.get()?.token),
  staff: () => request<StaffMember[]>('GET', '/api/admin/staff', undefined, staffSession.get()?.token),
  createStaff: (s: { fullName: string; email: string; password: string; role: StaffRole; shop?: string }) =>
    request<StaffMember>('POST', '/api/admin/staff', s, staffSession.get()?.token),
  setStaffActive: (id: number, value: boolean) =>
    request<StaffMember>('POST', `/api/admin/staff/${id}/active?value=${value}`, undefined, staffSession.get()?.token),
};

// --- Affichage ---

const TZ = 'Africa/Casablanca';

export const fmtDate = (iso?: string) =>
  iso ? new Date(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', timeZone: TZ }) : '';

export const fmtDateTime = (iso?: string) =>
  iso
    ? new Date(iso).toLocaleString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: TZ })
    : '';

export const daysLeft = (iso?: string) => (iso ? Math.max(0, Math.ceil((new Date(iso).getTime() - Date.now()) / 86_400_000)) : 0);

export const STATUS_LABEL: Record<Status, string> = {
  NONE: 'En attente d’activation',
  ACTIVE: 'Abonnement actif',
  EMPTY: 'Espressos épuisés',
  EXPIRED: 'Abonnement expiré',
};
