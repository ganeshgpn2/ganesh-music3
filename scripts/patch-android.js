// Runs after `npx cap add android`: copies native code, edits Manifest + Gradle, replaces icons.
const fs = require('fs');
const path = require('path');
const root = path.join(__dirname, '..');
const A = path.join(root, 'android', 'app');
const PKG = 'np.ganesh.sangeet';

function must(p) { if (!fs.existsSync(p)) { console.error('MISSING: ' + p); process.exit(1); } return p; }
function rd(p) { return fs.readFileSync(p, 'utf8'); }
function wr(p, t) { fs.writeFileSync(p, t); }

// 1) Java files
const javaDst = path.join(A, 'src', 'main', 'java', ...PKG.split('.'));
fs.mkdirSync(javaDst, { recursive: true });
const javaSrc = path.join(root, 'native', 'java');
for (const f of fs.readdirSync(javaSrc)) {
  if (f.endsWith('.java')) { fs.copyFileSync(path.join(javaSrc, f), path.join(javaDst, f)); console.log('java ->', f); }
}

// 2) AndroidManifest.xml
const mf = must(path.join(A, 'src', 'main', 'AndroidManifest.xml'));
let m = rd(mf);
if (!m.includes('SgsAlarmReceiver')) {
  const perms = [
    'android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE', 'android.permission.WAKE_LOCK',
    'android.permission.FOREGROUND_SERVICE', 'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK',
    'android.permission.POST_NOTIFICATIONS', 'android.permission.RECEIVE_BOOT_COMPLETED',
    'android.permission.USE_EXACT_ALARM', 'android.permission.SCHEDULE_EXACT_ALARM',
    'android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS', 'android.permission.VIBRATE'
  ];
  let add = '';
  for (const p of perms) if (!m.includes('"' + p + '"')) add += '    <uses-permission android:name="' + p + '" />\n';
  m = m.replace('<application', add + '\n    <application');
  m = m.replace('<application', '<application android:usesCleartextTraffic="true"');
  const comps =
`        <receiver android:name=".AlarmReceiver" android:exported="false"><intent-filter><action android:name="${PKG}.ALARM" /></intent-filter></receiver>
        <receiver android:name=".BootReceiver" android:exported="true"><intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
            <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        </intent-filter></receiver>
        <service android:name=".AlarmPlayService" android:exported="false" android:foregroundServiceType="mediaPlayback" />
        <service android:name=".KeepAliveService" android:exported="false" android:foregroundServiceType="mediaPlayback" />
        <!-- SgsAlarmReceiver marker -->
`;
  m = m.replace('</application>', comps + '    </application>');
  wr(mf, m);
  console.log('manifest patched');
}

// 3) Gradle: fixed signing key so new APKs update the old one
const gf = must(path.join(A, 'build.gradle'));
let g = rd(gf);
if (!g.includes('signing/debug.keystore')) {
  g = g.replace(/^android \{/m,
`android {
    signingConfigs {
        debug {
            storeFile file('../../signing/debug.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }
    }`);
  wr(gf, g);
  console.log('gradle signing patched');
}

// 4) Icons (remove adaptive-icon xml so our PNGs are used everywhere)
const res = path.join(A, 'src', 'main', 'res');
for (const d of fs.readdirSync(res)) {
  if (d === 'mipmap-anydpi-v26') fs.rmSync(path.join(res, d), { recursive: true, force: true });
}
const iconSrc = path.join(root, 'native', 'res');
if (fs.existsSync(iconSrc)) {
  for (const d of fs.readdirSync(iconSrc)) {
    const to = path.join(res, d); fs.mkdirSync(to, { recursive: true });
    for (const f of fs.readdirSync(path.join(iconSrc, d))) fs.copyFileSync(path.join(iconSrc, d, f), path.join(to, f));
  }
  console.log('icons copied');
}
console.log('patch done');
