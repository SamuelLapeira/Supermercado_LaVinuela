import { formatEuro, roundToCents } from './format';

describe('formatEuro', () => {
  it('writes amounts the Spanish way, with a comma and the euro sign', () => {
    expect(formatEuro(1.5).replace(/\s/g, ' ')).toBe('1,50 €');
    expect(formatEuro(0).replace(/\s/g, ' ')).toBe('0,00 €');
    expect(formatEuro(39.9).replace(/\s/g, ' ')).toBe('39,90 €');
  });

  it('groups thousands with a dot', () => {
    expect(formatEuro(1234.5).replace(/\s/g, ' ')).toBe('1234,50 €');
    expect(formatEuro(12345.67).replace(/\s/g, ' ')).toBe('12.345,67 €');
  });
});

describe('roundToCents', () => {
  it('avoids floating point noise', () => {
    expect(roundToCents(0.1 + 0.2)).toBe(0.3);
    expect(roundToCents(1.005)).toBe(1.01);
    expect(roundToCents(2.675)).toBe(2.68);
  });
});
