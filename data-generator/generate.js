#!/usr/bin/env node
/**
 * Generateur du jeu de donnees du benchmark.
 *
 * Produit un script SQL COPY (binaire texte) pour PostgreSQL :
 *   - 2000 categories
 *   - 100000 items repartis uniformement (50 par categorie)
 *
 * Usage :
 *   node generate.js --out ../db/seed.sql
 *   node generate.js --out ../db/seed.sql --categories 2000 --items 100000
 */

const fs = require('node:fs');
const path = require('node:path');

const args = process.argv.slice(2);
function arg(name, fallback) {
  const index = args.indexOf(`--${name}`);
  return index === -1 ? fallback : args[index + 1];
}

const NB_CATEGORIES = Number(arg('categories', 2000));
const NB_ITEMS = Number(arg('items', 100000));
const OUT = path.resolve(arg('out', path.join(__dirname, '..', 'db', 'seed.sql')));

const MOTS = [
  'Cable', 'Clavier', 'Souris', 'Ecran', 'Batterie', 'Chargeur', 'Support', 'Adaptateur',
  'Casque', 'Enceinte', 'Webcam', 'Hub', 'Disque', 'Memoire', 'Processeur', 'Carte',
  'Ventilateur', 'Boitier', 'Alimentation', 'Routeur', 'Switch', 'Modem', 'Antenne', 'Capteur',
];
const SUFFIXES = [
  'Standard', 'Pro', 'Slim', 'Compact', 'Industriel', 'Renforce', 'Ultra', 'Silencieux',
  'Rapide', 'Economique', 'Premium', 'Sans fil', 'USB-C', 'Bluetooth', 'Optique', 'Mecanique',
];

function pad(number, width) {
  return String(number).padStart(width, '0');
}

const lignes = [];
lignes.push('-- Jeu de donnees genere pour le benchmark REST');
lignes.push(`-- Categories : ${NB_CATEGORIES} | Items : ${NB_ITEMS}`);
lignes.push('TRUNCATE TABLE item, category RESTART IDENTITY CASCADE;');
lignes.push('');
lignes.push('COPY category (id, code, name, updated_at) FROM STDIN WITH (FORMAT text, DELIMITER \'|\');');

for (let i = 1; i <= NB_CATEGORIES; i++) {
  const code = `CAT-${pad(i, 5)}`;
  const name = `Categorie ${pad(i, 5)}`;
  lignes.push(`${i}|${code}|${name}|2026-01-01 00:00:00`);
}
lignes.push('\\.');
lignes.push('');
lignes.push('COPY item (id, sku, name, price, stock, category_id, updated_at) FROM STDIN WITH (FORMAT text, DELIMITER \'|\');');

for (let i = 1; i <= NB_ITEMS; i++) {
  const categoryId = ((i - 1) % NB_CATEGORIES) + 1;
  const mot = MOTS[i % MOTS.length];
  const suffixe = SUFFIXES[(i * 7) % SUFFIXES.length];
  const sku = `SKU-${pad(i, 7)}`;
  const name = `${mot} ${suffixe} ${pad(i, 6)}`;
  const price = ((i % 99000) / 100 + 1).toFixed(2);
  const stock = (i * 13) % 500;
  lignes.push(`${i}|${sku}|${name}|${price}|${stock}|${categoryId}|2026-01-01 00:00:00`);
}
lignes.push('\\.');
lignes.push('');
lignes.push('SELECT setval(pg_get_serial_sequence(\'category\', \'id\'), (SELECT MAX(id) FROM category));');
lignes.push('SELECT setval(pg_get_serial_sequence(\'item\', \'id\'), (SELECT MAX(id) FROM item));');

fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, lignes.join('\n'), 'utf8');

const taille = fs.statSync(OUT).size;
console.log(`Genere ${OUT}`);
console.log(`  ${NB_CATEGORIES} categories, ${NB_ITEMS} items`);
console.log(`  taille : ${(taille / 1024 / 1024).toFixed(1)} Mo`);
