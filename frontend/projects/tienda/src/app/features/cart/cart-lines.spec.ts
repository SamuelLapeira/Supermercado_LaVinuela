import { addToLines, parseStoredLines, removeLine, setLineQuantity } from './cart-lines';

describe('addToLines', () => {
  it('creates the line the first time and adds up after that', () => {
    const one = addToLines([], 'leche', 1, 40);
    const three = addToLines(one, 'leche', 2, 40);

    expect(one).toEqual([{ slug: 'leche', quantity: 1 }]);
    expect(three).toEqual([{ slug: 'leche', quantity: 3 }]);
  });

  it('never goes past the stock', () => {
    expect(addToLines([{ slug: 'leche', quantity: 2 }], 'leche', 5, 3)).toEqual([{ slug: 'leche', quantity: 3 }]);
  });

  it('never goes past 99 units even with a lot of stock', () => {
    expect(addToLines([{ slug: 'leche', quantity: 98 }], 'leche', 5, 1000)).toEqual([{ slug: 'leche', quantity: 99 }]);
  });

  it('adds nothing when the product has no stock', () => {
    expect(addToLines([], 'agotado', 1, 0)).toEqual([]);
  });

  it('does not modify the original list', () => {
    const original = [{ slug: 'leche', quantity: 1 }];
    addToLines(original, 'leche', 1, 40);
    expect(original).toEqual([{ slug: 'leche', quantity: 1 }]);
  });
});

describe('setLineQuantity', () => {
  it('changes the units of a line and keeps the others', () => {
    const lines = [{ slug: 'a', quantity: 1 }, { slug: 'b', quantity: 2 }];
    expect(setLineQuantity(lines, 'a', 4, 40)).toEqual([{ slug: 'a', quantity: 4 }, { slug: 'b', quantity: 2 }]);
  });

  it('removes the line with zero or less', () => {
    const lines = [{ slug: 'a', quantity: 1 }, { slug: 'b', quantity: 2 }];
    expect(setLineQuantity(lines, 'a', 0, 40)).toEqual([{ slug: 'b', quantity: 2 }]);
    expect(setLineQuantity(lines, 'a', -3, 40)).toEqual([{ slug: 'b', quantity: 2 }]);
  });

  it('ignores decimals', () => {
    expect(setLineQuantity([], 'a', 2.9, 40)).toEqual([{ slug: 'a', quantity: 2 }]);
  });
});

describe('removeLine', () => {
  it('removes only that line', () => {
    expect(removeLine([{ slug: 'a', quantity: 1 }, { slug: 'b', quantity: 2 }], 'a')).toEqual([{ slug: 'b', quantity: 2 }]);
  });
});

describe('parseStoredLines', () => {
  it('reads a valid saved cart', () => {
    expect(parseStoredLines('[{"slug":"a","quantity":2}]')).toEqual([{ slug: 'a', quantity: 2 }]);
  });

  it('survives anything unexpected', () => {
    for (const bad of [null, '', 'no es json', '{}', '"texto"', '[1,2]', '[{"slug":1,"quantity":2}]', '[{"slug":"a","quantity":-1}]', '[{"slug":"a","quantity":1.5}]', '[{"slug":"a","quantity":500}]']) {
      expect(parseStoredLines(bad)).toEqual([]);
    }
  });

  it('keeps the valid lines and drops the invalid ones', () => {
    expect(parseStoredLines('[{"slug":"a","quantity":1},{"slug":2},{"slug":"b","quantity":3}]')).toEqual([
      { slug: 'a', quantity: 1 },
      { slug: 'b', quantity: 3 },
    ]);
  });
});
