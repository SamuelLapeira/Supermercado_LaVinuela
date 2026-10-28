// Configuración de ESLint del frontend (generada por tools/setup-frontend-lint.mjs).
// @ts-check
import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';
import angular from 'angular-eslint';

// defineConfig existe desde ESLint 9.22; con versiones anteriores se usa la ayuda de typescript-eslint.
const defineConfig = (await import('eslint/config').catch(() => null))?.defineConfig ?? tseslint.config;

/**
 * Fronteras entre proyectos (Fase 4, apartado 2.1):
 *   tienda, panel -> ui, core, api, util
 *   ui            -> util
 *   core          -> api, util
 *   api           -> (nada)
 *   util          -> (nada)
 *   tienda <-> panel: prohibido
 */
const forbid = (message, ...groups) => ({
  'no-restricted-imports': ['error', { patterns: [{ group: groups.flat(), message }] }],
});
// Los patrones usan la sintaxis de .gitignore. La barra inicial ancla el nombre al principio de la ruta de
// importación: sin ella, "core" también bloquearía "@angular/core".
const lib = (name) => [`/${name}`, `/${name}/**`, `**/projects/${name}/**`];

export default defineConfig(
  {
    // Código generado y la plantilla de ejemplo de Angular, que se sustituye en la historia E0-06.
    ignores: [
      'dist/**', '.angular/**', 'node_modules/**', 'coverage/**',
      'projects/tienda/src/app/app.html', 'projects/panel/src/app/app.html',
    ],
  },
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      // Prefijo "lv" en todos los componentes y directivas.
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'lv', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: 'lv', style: 'kebab-case' }],

      // Sin "any" (Fase 4, apartado 1).
      '@typescript-eslint/no-explicit-any': 'error',
      'no-console': ['error', { allow: ['warn', 'error'] }],

      // Seguridad (Fase 4, apartado 12): nunca saltarse el saneado de Angular.
      'no-restricted-properties': [
        'error',
        ...['bypassSecurityTrustHtml', 'bypassSecurityTrustScript', 'bypassSecurityTrustStyle',
          'bypassSecurityTrustUrl', 'bypassSecurityTrustResourceUrl'].map((property) => ({
          property,
          message: 'Prohibido: salta la protección de Angular contra XSS. El texto enriquecido llega ya saneado del servidor.',
        })),
      ],
      // El token de acceso vive solo en memoria; nada sensible en el almacenamiento del navegador.
      'no-restricted-globals': [
        'error',
        { name: 'localStorage', message: 'No guardes datos sensibles en el navegador. Si es un dato inocuo, justifica la excepción con un comentario.' },
        { name: 'sessionStorage', message: 'No guardes datos sensibles en el navegador. Si es un dato inocuo, justifica la excepción con un comentario.' },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },

  // ---- fronteras entre proyectos ----
  {
    files: ['projects/ui/**/*.ts'],
    rules: forbid('ui es solo visual: no puede depender de api, core ni de las aplicaciones.',
      lib('api'), lib('core'), lib('tienda'), lib('panel')),
  },
  {
    files: ['projects/core/**/*.ts'],
    rules: forbid('core solo puede usar api y util.', lib('ui'), lib('tienda'), lib('panel')),
  },
  {
    files: ['projects/api/**/*.ts'],
    rules: forbid('api no depende de nada del proyecto.', lib('ui'), lib('core'), lib('util'), lib('tienda'), lib('panel')),
  },
  {
    files: ['projects/util/**/*.ts'],
    rules: forbid('util no depende de nada del proyecto.', lib('ui'), lib('core'), lib('api'), lib('tienda'), lib('panel')),
  },
  {
    files: ['projects/tienda/**/*.ts'],
    rules: forbid('tienda y panel son independientes: no se importan entre sí.', lib('panel')),
  },
  {
    files: ['projects/panel/**/*.ts'],
    rules: forbid('tienda y panel son independientes: no se importan entre sí.', lib('tienda')),
  },
);
