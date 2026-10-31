import { TestBed } from '@angular/core/testing';
import { ApiConfiguration } from 'api';
import { appConfig } from './app.config';

describe('appConfig', () => {
  it('calls the API on the same address as the page, without a double slash', () => {
    TestBed.configureTestingModule({ providers: appConfig.providers });

    // Con la dirección base «/» las llamadas saldrían como «//api/...», que el navegador entiende como otra web.
    expect(TestBed.inject(ApiConfiguration).rootUrl).toBe('');
  });
});
