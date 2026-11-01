import { DELIVERY_ZONES } from './delivery-demo';
import { quoteDelivery, remainingForFreeShipping, remainingForMinimum } from './delivery-pricing';

describe('quoteDelivery', () => {
  it('charges by distance band', () => {
    expect(quoteDelivery(30, 5)).toEqual({ kind: 'fee', fee: 3, free: false });
    expect(quoteDelivery(30, 10)).toEqual({ kind: 'fee', fee: 3, free: false });
    expect(quoteDelivery(30, 10.1)).toEqual({ kind: 'fee', fee: 4.5, free: false });
    expect(quoteDelivery(30, 25)).toEqual({ kind: 'fee', fee: 6, free: false });
    expect(quoteDelivery(30, 40)).toEqual({ kind: 'fee', fee: 8, free: false });
  });

  it('does not deliver beyond the maximum distance', () => {
    expect(quoteDelivery(30, 40.1)).toEqual({ kind: 'out-of-zone' });
    expect(quoteDelivery(100, 52)).toEqual({ kind: 'out-of-zone' });
  });

  it('is free from the free-shipping amount, whatever the distance within the zone', () => {
    expect(quoteDelivery(60, 38)).toEqual({ kind: 'fee', fee: 0, free: true });
    expect(quoteDelivery(59.99, 38)).toEqual({ kind: 'fee', fee: 8, free: false });
  });

  it('has sample zones for every band and one outside the area', () => {
    const kinds = DELIVERY_ZONES.map((zone) => quoteDelivery(30, zone.km).kind);
    expect(kinds.filter((kind) => kind === 'fee').length).toBeGreaterThan(5);
    expect(kinds).toContain('out-of-zone');
  });
});

describe('remaining amounts', () => {
  it('tells how much is missing for the minimum order', () => {
    expect(remainingForMinimum(12.5)).toBe(7.5);
    expect(remainingForMinimum(20)).toBe(0);
    expect(remainingForMinimum(45)).toBe(0);
  });

  it('tells how much is missing for free shipping', () => {
    expect(remainingForFreeShipping(41.35)).toBe(18.65);
    expect(remainingForFreeShipping(60)).toBe(0);
    expect(remainingForFreeShipping(0)).toBe(60);
  });
});
