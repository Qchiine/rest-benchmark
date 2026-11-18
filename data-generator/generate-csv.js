/*
 * Generateur de CSV pour JMeter.
 *
 * Produit, dans jmeter/csv :
 *   - ids.csv          : category_ids et item_ids existants
 *   - payload-light.csv : corps JSON de 1 Ko (name, price, stock)
 *   - payload-heavy.csv : corps JSON de 5 Ko (description longue)
 *
 * Usage : node generate-csv.js
 */

const fs = require('node:fs');
const path = require('node:path');

const RACINE = path.join(__dirname, '..');
const DOSSIER = path.join(RACINE, 'jmeter', 'csv');
const NB_CATEGORIES = 2000;
const NB_ITEMS = 100000;
const LIGNES_IDS = 5000;
const LIGNES_PAYLOAD = 1000;

fs.mkdirSync(DOSSIER, { recursive: true });

function pad(n, w) {
  return String(n).padStart(w, '0');
}

/**
 * Ajuste la longueur du nom pour que le corps JSON atteigne exactement
 * la taille cible demandee par le protocole de benchmark.
 */
function corpsDeTailleCible(base, champ, cible) {
  const sansRemplissage = JSON.stringify({ ...base, [champ]: '' });
  const manque = cible - sansRemplissage.length;
  if (manque < 0) {
    throw new Error(`payload de base deja plus grand que ${cible} octets`);
  }
  return JSON.stringify({ ...base, [champ]: 'x'.repeat(manque) });
}

// CSV d'identifiants existants
const ids = ['category_id,item_id'];
for (let i = 0; i < LIGNES_IDS; i++) {
  const categoryId = (i % NB_CATEGORIES) + 1;
  const itemId = ((i * 37) % NB_ITEMS) + 1;
  ids.push(`${categoryId},${itemId}`);
}
fs.writeFileSync(path.join(DOSSIER, 'ids.csv'), ids.join('\n'), 'utf8');

// Payload leger : 1024 octets exactement
const leger = ['body'];
for (let i = 0; i < LIGNES_PAYLOAD; i++) {
  const categoryId = (i % NB_CATEGORIES) + 1;
  const corps = corpsDeTailleCible(
    {
      sku: `BENCH-LIGHT-${pad(i, 6)}`,
      name: `Produit benchmark leger ${pad(i, 6)}`,
      price: Number(((i % 9900) / 100 + 1).toFixed(2)),
      stock: i % 300,
      categoryId,
    },
    'name',
    1024,
  );
  leger.push(`"${corps.replace(/"/g, '""')}"`);
}
fs.writeFileSync(path.join(DOSSIER, 'payload-light.csv'), leger.join('\n'), 'utf8');

// Payload lourd : 5120 octets exactement
const lourd = ['body'];
for (let i = 0; i < LIGNES_PAYLOAD; i++) {
  const categoryId = (i % NB_CATEGORIES) + 1;
  const corps = corpsDeTailleCible(
    {
      sku: `BENCH-HEAVY-${pad(i, 6)}`,
      name: `Produit benchmark lourd ${pad(i, 6)}`,
      price: Number(((i % 9900) / 100 + 1).toFixed(2)),
      stock: i % 300,
      categoryId,
      description: 'Description detaillee du produit pour simuler un corps volumineux. ',
    },
    'description',
    5120,
  );
  lourd.push(`"${corps.replace(/"/g, '""')}"`);
}
fs.writeFileSync(path.join(DOSSIER, 'payload-heavy.csv'), lourd.join('\n'), 'utf8');

function taille(nom) {
  const f = path.join(DOSSIER, nom);
  return `${nom} : ${(fs.statSync(f).size / 1024).toFixed(1)} Ko`;
}

console.log('CSV generes dans ' + DOSSIER);
console.log('  ' + taille('ids.csv'));
console.log('  ' + taille('payload-light.csv'));
console.log('  ' + taille('payload-heavy.csv'));

const exempleLeger = JSON.parse(leger[1].slice(1, -1).replace(/""/g, '"'));
const exempleLourd = JSON.parse(lourd[1].slice(1, -1).replace(/""/g, '"'));
console.log(`  taille payload leger : ${JSON.stringify(exempleLeger).length} octets`);
console.log(`  taille payload lourd : ${JSON.stringify(exempleLourd).length} octets`);
