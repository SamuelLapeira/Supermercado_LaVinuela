// Comprueba el contraste (WCAG 2.2 AA) de todos los pares de color de los tokens de diseño.
//
// Lee frontend/projects/ui/src/styles/_tokens.scss, que es la única fuente de colores, y falla si algún par
// de texto y fondo baja de 4,5:1 (texto) o de 3:1 (foco, bordes de controles, gráficos).
// Se ejecuta en local y en la integración continua:   node tools/check-contrast.mjs
import { readFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const file = resolve(root, 'frontend/projects/ui/src/styles/_tokens.scss');
const css = readFileSync(file, 'utf8');

// ---- lectura de los tokens: la sección "light" y la "dark" (esta hereda lo que no redefine)
const [, afterLight = ''] = css.split('/* theme:light */');
const [lightPart = '', darkPart = ''] = afterLight.split('/* theme:dark */');
const read = (text) => {
  const tokens = {};
  for (const m of text.matchAll(/--([a-z0-9-]+):\s*(#[0-9a-fA-F]{6})\s*;/g)) tokens[m[1]] = m[2].toLowerCase();
  return tokens;
};
const light = read(lightPart);
const dark = { ...light, ...read(darkPart) };

// ---- cálculo de contraste
const channel = (v) => { const s = v / 255; return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4; };
const luminance = (hex) => {
  const n = parseInt(hex.slice(1), 16);
  return 0.2126 * channel((n >> 16) & 255) + 0.7152 * channel((n >> 8) & 255) + 0.0722 * channel(n & 255);
};
const contrast = (a, b) => {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
};

// ---- pares que se deben cumplir: [texto o elemento, fondo, mínimo, temas]
const TEXT = 4.5;
const NONTEXT = 3;
const both = ['light', 'dark'];
const pairs = [
  ['ink', 'bg', TEXT, both], ['ink', 'surface', TEXT, both], ['ink', 'surface-2', TEXT, both],
  ['muted', 'bg', TEXT, both], ['muted', 'surface', TEXT, both], ['muted', 'surface-2', TEXT, both],
  ['on-brand', 'brand', TEXT, both], ['on-brand', 'brand-hover', TEXT, both],
  ['brand-text', 'surface', TEXT, both], ['brand-text', 'bg', TEXT, both], ['brand-text', 'brand-soft', TEXT, both],
  ['ink', 'brand-soft', TEXT, both],
  ['on-accent', 'accent', TEXT, both],
  ['ink', 'water', TEXT, ['light']], ['ink', 'water-soft', TEXT, both],
  ['earth', 'surface', TEXT, both],
  ['ok', 'ok-soft', TEXT, both], ['warn', 'warn-soft', TEXT, both],
  ['danger', 'danger-soft', TEXT, both], ['info', 'info-soft', TEXT, both],
  ['danger', 'surface', TEXT, both], ['ok', 'surface', TEXT, both], ['info', 'surface', TEXT, both],
  // No texto (WCAG 1.4.11): foco y borde de campos de formulario
  ['focus', 'surface', NONTEXT, both], ['focus', 'bg', NONTEXT, both], ['focus', 'surface-2', NONTEXT, both],
  ['control-border', 'surface', NONTEXT, both], ['control-border', 'bg', NONTEXT, both],
  ...[1, 2, 3, 4, 5, 6].map((n) => [`cat-${n}-ink`, `cat-${n}`, TEXT, both]),
];

let failures = 0;
const missing = new Set();
for (const theme of both) {
  const t = theme === 'light' ? light : dark;
  console.log(`\nTema ${theme === 'light' ? 'claro' : 'oscuro'}:`);
  for (const [fg, bg, min, themes] of pairs) {
    if (!themes.includes(theme)) continue;
    if (!t[fg] || !t[bg]) { missing.add(!t[fg] ? fg : bg); failures++; continue; }
    const ratio = contrast(t[fg], t[bg]);
    const ok = ratio >= min;
    if (!ok) failures++;
    console.log(`  ${ok ? 'OK    ' : 'FALLA '} ${ratio.toFixed(1).padStart(5)}:1 (mín. ${min})  ${fg} ${t[fg]} sobre ${bg} ${t[bg]}`);
  }
}
if (missing.size) console.error(`\nFaltan estos tokens en el fichero: ${[...missing].join(', ')}`);
if (failures) {
  console.error(`\n✗ ${failures} comprobaciones de contraste no se cumplen. Ajusta los colores en _tokens.scss.`);
  process.exit(1);
}
console.log(`\n✓ Todos los pares de color cumplen WCAG 2.2 AA (${pairs.length} comprobaciones por tema).`);
