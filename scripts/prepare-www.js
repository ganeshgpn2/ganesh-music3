// player/index.html  ->  www/index.html
const fs = require('fs');
const path = require('path');
const src = path.join(__dirname, '..', 'player', 'index.html');
const outDir = path.join(__dirname, '..', 'www');
if (!fs.existsSync(src)) { console.error('player/index.html not found'); process.exit(1); }
fs.mkdirSync(outDir, { recursive: true });
fs.copyFileSync(src, path.join(outDir, 'index.html'));
console.log('www/index.html ready (' + fs.statSync(src).size + ' bytes)');
