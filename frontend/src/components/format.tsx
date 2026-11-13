export const money = (n?: number | null) =>
  new Intl.NumberFormat('fr-MA', { style: 'currency', currency: 'MAD' }).format(n ?? 0);

export const dateFr = (s?: string | null) => (s ? new Date(s).toLocaleDateString('fr-FR') : '');

const COLORS: Record<string, string> = {
  DRAFT: '', CONFIRMED: 'info', SHIPPED: 'info', DELIVERED: 'ok', CANCELLED: 'bad',
  UNPAID: 'warn', PARTIALLY_PAID: 'info', PAID: 'ok',
  IN: 'ok', OUT: 'warn', ADJUSTMENT: 'info',
};

export function Badge({ value }: { value: string }) {
  return <span className={`badge ${COLORS[value] ?? ''}`}>{value}</span>;
}
