import { describe, expect, it } from 'vitest';
import { dateFr, money } from './components/format';

describe('format', () => {
  it('formats money in MAD', () => {
    expect(money(1234.5)).toMatch(/1.234,50/);
    expect(money(null)).toMatch(/0,00/);
  });
  it('formats dates', () => {
    expect(dateFr('2026-11-10')).toBe('10/11/2026');
    expect(dateFr(undefined)).toBe('');
  });
});
