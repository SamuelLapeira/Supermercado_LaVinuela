/* eslint-disable no-restricted-globals -- las pruebas limpian el almacenamiento del navegador, que solo guarda datos de ejemplo */
import { sessionHint } from './session-hint';

describe('sessionHint', () => {
  beforeEach(() => localStorage.clear());

  it('is not set at first, can be set and can be cleared', () => {
    expect(sessionHint.isSet()).toBe(false);

    sessionHint.set();
    expect(sessionHint.isSet()).toBe(true);

    sessionHint.clear();
    expect(sessionHint.isSet()).toBe(false);
  });

  it('only ever stores a yes, never a token or personal data', () => {
    sessionHint.set();

    expect(Object.values({ ...localStorage })).toEqual(['1']);
  });
});
