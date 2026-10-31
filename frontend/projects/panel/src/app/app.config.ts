import { provideApiConfiguration } from 'api';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideApiConfiguration(''),
    provideHttpClient(withFetch()),provideBrowserGlobalErrorListeners(), provideRouter(routes)],
};
