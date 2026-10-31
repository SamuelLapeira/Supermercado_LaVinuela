import { parseVerificationState } from './verification-state';

describe('parseVerificationState', () => {
  it('reads a valid state', () => {
    expect(parseVerificationState({ email: 'a@example.com', maskedEmail: 'a***@example.com', resendAfterSeconds: 60 })).toEqual({
      email: 'a@example.com',
      maskedEmail: 'a***@example.com',
      resendAfterSeconds: 60,
    });
  });

  it('ignores extra data the browser adds to the history state', () => {
    const state = parseVerificationState({ email: 'a@example.com', maskedEmail: 'a***', resendAfterSeconds: 60, navigationId: 3 });
    expect(state?.email).toBe('a@example.com');
  });

  it('returns null when it is missing or has the wrong shape', () => {
    for (const bad of [null, undefined, 'texto', 42, {}, { email: 'a@example.com' }, { email: 1, maskedEmail: 'x', resendAfterSeconds: 60 }, { email: 'a', maskedEmail: 'x', resendAfterSeconds: '60' }]) {
      expect(parseVerificationState(bad)).toBeNull();
    }
  });
});
