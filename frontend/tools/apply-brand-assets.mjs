// Conecta el sistema de diseño con las dos aplicaciones de Angular (historia E0-06, parte 1):
//   - styles.scss de tienda y panel: carga los tokens y los estilos base de la librería ui;
//   - index.html de tienda y panel: idioma español, título, color del navegador e icono de iOS.
// Es idempotente: se puede ejecutar varias veces sin duplicar nada.
//
// Uso (desde la raíz del proyecto):   node tools/apply-brand-assets.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const apps = [
  { name: 'tienda', title: 'La Viñuela · Supermercado' },
  { name: 'panel', title: 'La Viñuela · Panel de gestión' },
];
const root = resolve(process.cwd(), 'frontend', 'projects');
if (!existsSync(root)) {
  console.error("No encuentro frontend/projects. Ejecuta este script desde la raíz del proyecto.");
  process.exit(1);
}

for (const app of apps) {
  // ---- styles.scss
  const stylesPath = resolve(root, app.name, 'src', 'styles.scss');
  if (!existsSync(stylesPath)) { console.error(`Falta ${stylesPath}`); process.exit(1); }
  let styles = readFileSync(stylesPath, 'utf8');
  const useLine = "@use '../../ui/src/styles/ui';";
  if (!styles.includes('ui/src/styles/ui')) {
    styles = `${useLine}\n\n${styles}`;
    writeFileSync(stylesPath, styles, 'utf8');
    console.log(`${app.name}: styles.scss carga el sistema de diseño`);
  } else {
    console.log(`${app.name}: styles.scss ya cargaba el sistema de diseño`);
  }

  // ---- index.html
  const indexPath = resolve(root, app.name, 'src', 'index.html');
  if (!existsSync(indexPath)) { console.error(`Falta ${indexPath}`); process.exit(1); }
  let html = readFileSync(indexPath, 'utf8');
  const before = html;
  html = html.replace(/<html\s+lang="[^"]*"/, '<html lang="es"');
  html = html.replace(/<title>[^<]*<\/title>/, `<title>${app.title}</title>`);
  if (!/name="theme-color"/.test(html)) {
    html = html.replace('</head>', '  <meta name="theme-color" content="#007924">\n</head>');
  }
  if (!/rel="apple-touch-icon"/.test(html)) {
    html = html.replace('</head>', '  <link rel="apple-touch-icon" href="apple-touch-icon.png">\n</head>');
  }
  if (html !== before) {
    writeFileSync(indexPath, html, 'utf8');
    console.log(`${app.name}: index.html actualizado (idioma, título, color del navegador e icono)`);
  } else {
    console.log(`${app.name}: index.html ya estaba actualizado`);
  }
}
console.log('\nListo. Comprueba con:  cd frontend; npx.cmd ng build tienda; npx.cmd ng build panel');
