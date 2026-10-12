// Client de l'API Spring Boot (même domaine en production, proxy Vite en développement).

export type Status = 'NONE' | 'ACTIVE' | 'EMPTY' | 'EXPIRED';
export type Plan = { price: number; drinks: number; days: number };

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
  delivered: boolean;
  devCode?: string;
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
export type Subscriber = { customerId: number; firstName?: string; phone: string; status: Status; drinksLeft: number; expiresAt: string };
export type StaffMember = { id: number; fullName: string; email: string; role: StaffRole; shop?: string; active: boolean };

export class ApiError extends Error {
  constructor(public status: number, message: string, public code?: string) {
    super(message);
  }
}

// --- Sessions (stockage local, protégé contre un navigateur qui le refuse) ---

const CUSTOMER_KEY = 'mizaj.customer';
const STAFF_KEY = 'mizaj.staff';

function read(key: string): string | null {
  try {
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
  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) {
    const message =
      data?.message ??
      (res.status === 401 ? 'Session expirée, reconnectez-vous.' : res.status === 403 ? 'Accès refusé.' : 'Erreur, réessayez.');
    throw new ApiError(res.status, message, data?.code);
  }
  return data as T;
}

export const api = {
  plan: () => request<Plan>('GET', '/api/public/plan'),
  otpStart: (phone: string, firstName?: string) => request<OtpStart>('POST', '/api/public/otp/start', { phone, firstName }),
  otpVerify: (phone: string, code: string) =>
    request<{ token: string; card: Card }>('POST', '/api/public/otp/verify', { phone, code }),

  me: () => request<Card>('GET', '/api/me', undefined, customerToken.get()),
  rotateQr: () => request<Card>('POST', '/api/me/qr/rotate', undefined, customerToken.get()),

  staffLogin: (email: string, password: string) => request<StaffSession>('POST', '/api/auth/staff/login', { email, password }),
  scan: (token: string) => request<StaffCard>('POST', '/api/staff/scan', { token }, staffSession.get()?.token),
  lookup: (phone: string) =>
    request<StaffCard>('GET', `/api/staff/lookup?phone=${encodeURIComponent(phone)}`, undefined, staffSession.get()?.token),
  redeem: (id: number, force = false) =>
    request<StaffCard>('POST', `/api/staff/customers/${id}/redeem`, { force }, staffSession.get()?.token),
  activate: (id: number, confirmReplace = false) =>
    request<StaffCard>('POST', `/api/staff/customers/${id}/activate`, { confirmReplace }, staffSession.get()?.token),
  undo: (redemptionId: number) =>
    request<StaffCard>('POST', `/api/staff/redemptions/${redemptionId}/undo`, undefined, staffSession.get()?.token),
  today: () => request<Today>('GET', '/api/staff/today', undefined, staffSession.get()?.token),

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
