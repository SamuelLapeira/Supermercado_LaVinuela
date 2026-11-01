import {
  isCodeValid,
  isEmailValid,
  isPasswordValid,
  isUsernameValid,
  maskEmail,
  normalizeEmail,
  normalizeUsername,
  passwordChecks,
  safeReturnUrl,
} from './account-rules';

describe('username', () => {
  it('accepts what the server accepts', () => {
    for (const ok of ['ana', 'ana.lopez', 'a_b-c.d', 'usuario123', ' Ana.Lopez ']) {
      expect(isUsernameValid(ok)).toBe(true);
    }
  });

  it('rejects what the server rejects', () => {
    for (const bad of ['ab', 'a'.repeat(31), 'con espacios', '.empieza', 'termina.', 'ñandú', 'a/b/c', '<script>', '']) {
      expect(isUsernameValid(bad)).toBe(false);
    }
  });

  it('is normalised to lowercase without surrounding spaces', () => {
    expect(normalizeUsername('  Ana.Lopez ')).toBe('ana.lopez');
  });
});

describe('email', () => {
  it('accepts valid addresses', () => {
    for (const ok of ['ana@example.com', ' Ana@Example.COM ', 'a.b+c@sub.example.es']) {
      expect(isEmailValid(ok)).toBe(true);
    }
  });

  it('rejects malformed addresses', () => {
    for (const bad of ['', 'sin-arroba', 'dos@@example.com', 'sin@punto', 'con espacio@example.com', '@example.com', 'a@b.c']) {
      expect(isEmailValid(bad)).toBe(false);
    }
  });

  it('is normalised to lowercase without surrounding spaces', () => {
    expect(normalizeEmail(' Ana@Example.COM ')).toBe('ana@example.com');
  });
});

describe('password', () => {
  const met = (password: string) => passwordChecks(password).filter((c) => c.met).map((c) => c.id);

  it('reports each condition separately', () => {
    expect(met('')).toEqual([]);
    expect(met('abc')).toEqual(['lower']);
    expect(met('ABC')).toEqual(['upper']);
    expect(met('123')).toEqual(['digit']);
    expect(met('Aa1')).toEqual(['upper', 'lower', 'digit']);
    expect(met('Aa1bcdefgh')).toEqual(['length', 'upper', 'lower', 'digit']);
  });

  it('is valid only when every condition is met', () => {
    expect(isPasswordValid('Una-contraseña-2026')).toBe(true);
    expect(isPasswordValid('una-contraseña-2026')).toBe(false);
    expect(isPasswordValid('UNA-CONTRASEÑA-2026')).toBe(false);
    expect(isPasswordValid('Una-contraseña-larga')).toBe(false);
    expect(isPasswordValid('Corta-1')).toBe(false);
  });

  it('counts characters the way a person does (an emoji is one character)', () => {
    expect(isPasswordValid('Aa1😀😁😂🤣😃😄😅')).toBe(true);
    expect(isPasswordValid('Aa1😀😁😂🤣😃😄')).toBe(false);
  });

  it('accepts accents and any other letters', () => {
    expect(isPasswordValid('Ñandú-Ángel-77')).toBe(true);
  });

  it('rejects passwords that are too long', () => {
    expect(isPasswordValid('Ab1'.repeat(43))).toBe(false);
  });
});

describe('verification code', () => {
  it('needs exactly six digits', () => {
    expect(isCodeValid('482915')).toBe(true);
    for (const bad of ['', '12345', '1234567', '12ab56', ' 482915']) {
      expect(isCodeValid(bad)).toBe(false);
    }
  });
});

describe('safeReturnUrl', () => {
  it('keeps addresses of this same site', () => {
    expect(safeReturnUrl('/cuenta')).toBe('/cuenta');
    expect(safeReturnUrl('/catalogo?categoria=lacteos')).toBe('/catalogo?categoria=lacteos');
  });

  it('ignores anything that could lead to another site', () => {
    for (const bad of ['//evil.example.com', 'https://evil.example.com', 'http://evil.example.com', '/\\evil.example.com', 'javascript:alert(1)', 'cuenta', '', null, undefined, '/ok\nmalo']) {
      expect(safeReturnUrl(bad)).toBe('/cuenta');
    }
  });

  it('uses the fallback it is given', () => {
    expect(safeReturnUrl('//evil.example.com', '/')).toBe('/');
  });
});

describe('maskEmail', () => {
  it('hides the local part like the server does', () => {
    expect(maskEmail('ana.lopez@example.com')).toBe('a***@example.com');
  });

  it('hides everything when it is not an address', () => {
    expect(maskEmail('sin-arroba')).toBe('***');
    expect(maskEmail('@example.com')).toBe('***');
  });
});
