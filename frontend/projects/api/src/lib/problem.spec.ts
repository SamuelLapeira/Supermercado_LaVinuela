import { HttpErrorResponse } from '@angular/common/http';
import { fieldErrors, PROBLEM_CODES, problemCode, problemFrom } from './problem';

function serverError(status: number, body: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, statusText: 'Error', error: body });
}

const validationProblem = {
  type: 'urn:lavinuela:problem:validation-failed',
  title: 'Validation failed',
  status: 400,
  detail: 'Hay datos que no son correctos. Revísalos e inténtalo de nuevo.',
  code: 'VALIDATION_FAILED',
  traceId: '01KB3Z6Q8M2R7V1N4P9XJ5C0TD',
  errors: [
    { field: 'password', message: 'La contraseña debe incluir al menos una mayúscula.' },
    { field: 'password', message: 'Otro mensaje del mismo campo.' },
    { field: 'email', message: 'Escribe un correo electrónico válido.' },
  ],
};

describe('problemFrom', () => {
  it('extracts the standard error from an HTTP error', () => {
    const problem = problemFrom(serverError(400, validationProblem));

    expect(problem?.code).toBe('VALIDATION_FAILED');
    expect(problem?.status).toBe(400);
  });

  it('returns null when the connection failed (no response body)', () => {
    expect(problemFrom(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }))).toBeNull();
  });

  it('returns null when the body is not a standard error', () => {
    expect(problemFrom(serverError(500, '<html>Bad gateway</html>'))).toBeNull();
    expect(problemFrom(serverError(500, { message: 'algo' }))).toBeNull();
    expect(problemFrom(serverError(500, null))).toBeNull();
    expect(problemFrom(serverError(500, { code: 42, status: '500', detail: 1 }))).toBeNull();
  });

  it('returns null when the error is not an HTTP error at all', () => {
    expect(problemFrom(new Error('fallo'))).toBeNull();
    expect(problemFrom(undefined)).toBeNull();
    expect(problemFrom('texto')).toBeNull();
  });
});

describe('problemCode', () => {
  it('returns the code of the server error', () => {
    const conflict = { ...validationProblem, status: 409, code: PROBLEM_CODES.usernameTaken, errors: undefined };

    expect(problemCode(serverError(409, conflict))).toBe('USERNAME_TAKEN');
  });

  it('returns null for anything that is not a standard error', () => {
    expect(problemCode(new Error('fallo'))).toBeNull();
    expect(problemCode(serverError(500, 'texto'))).toBeNull();
  });
});

describe('fieldErrors', () => {
  it('maps each field to its first message', () => {
    const errors = fieldErrors(problemFrom(serverError(400, validationProblem)));

    expect(errors).toEqual({
      password: 'La contraseña debe incluir al menos una mayúscula.',
      email: 'Escribe un correo electrónico válido.',
    });
  });

  it('is empty when there are no field errors', () => {
    expect(fieldErrors(null)).toEqual({});
    expect(fieldErrors(problemFrom(serverError(409, { ...validationProblem, errors: undefined })))).toEqual({});
  });
});

describe('PROBLEM_CODES', () => {
  it('matches the codes the server sends', () => {
    expect(Object.values(PROBLEM_CODES)).toEqual([
      'VALIDATION_FAILED',
      'USERNAME_TAKEN',
      'VERIFICATION_CODE_INVALID',
      'VERIFICATION_CODE_EXPIRED',
      'VERIFICATION_TOO_MANY_ATTEMPTS',
      'EMAIL_SERVICE_UNAVAILABLE',
    ]);
  });
});
