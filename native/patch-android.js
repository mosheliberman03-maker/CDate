/* Runs in CI after `npx cap add android`, before sync/build. Adds the permissions that make
   reminders fire exactly and survive a reboot. Idempotent and tolerant: if the file shape
   changes it leaves things as they are rather than breaking the build. */
const fs = require('fs');
const man = 'android/app/src/main/AndroidManifest.xml';
try {
  let x = fs.readFileSync(man, 'utf8');
  const perms = [
    'android.permission.SCHEDULE_EXACT_ALARM',
    'android.permission.USE_EXACT_ALARM',
    'android.permission.POST_NOTIFICATIONS',
    'android.permission.RECEIVE_BOOT_COMPLETED',
    'android.permission.VIBRATE',
    'android.permission.ACCESS_COARSE_LOCATION',
    'android.permission.ACCESS_FINE_LOCATION',
    'android.permission.RECORD_AUDIO',
    'android.permission.MODIFY_AUDIO_SETTINGS'
  ];
  const add = perms
    .filter(p => !x.includes(p))
    .map(p => `    <uses-permission android:name="${p}" />`)
    .join('\n');
  if (add && x.includes('</manifest>')) {
    x = x.replace('</manifest>', add + '\n</manifest>');
    fs.writeFileSync(man, x);
    console.log('patch-android: added permissions');
  } else {
    console.log('patch-android: nothing to add');
  }
} catch (e) {
  console.log('patch-android: skipped (' + e.message + ')');
}

/* Sign every build with the app's permanent key (native/cdate-debug.keystore). Without this each CI run
   made a new random key and phones refused to install updates ("App not installed"). */
const gradle = 'android/app/build.gradle';
try {
  let g = fs.readFileSync(gradle, 'utf8');
  if (!g.includes('cdate-debug.keystore')) {
    g += `
android.signingConfigs.debug.storeFile = file("\${rootDir}/../cdate-debug.keystore")
android.signingConfigs.debug.storePassword = "android"
android.signingConfigs.debug.keyAlias = "androiddebugkey"
android.signingConfigs.debug.keyPassword = "android"
`;
    fs.writeFileSync(gradle, g);
    console.log('patch-android: permanent signing key set');
  }
} catch (e) {
  console.log('patch-android: signing not set (' + e.message + ')');
  process.exit(1);
}

/* The assistant character on notifications (owner, v48): native/assets/char/*.png → res/drawable, used as
   largeIcon 'cc_char1'…'cc_char9' (the face changes with the message) and smallIcon 'ic_stat' (white silhouette). */
try {
  const src = 'assets/char', dst = 'android/app/src/main/res/drawable';
  fs.mkdirSync(dst, { recursive: true });
  for (const f of fs.readdirSync(src)) if (f.endsWith('.png')) fs.copyFileSync(src + '/' + f, dst + '/' + f);
  console.log('patch-android: character pictures copied');
} catch (e) {
  console.log('patch-android: character pictures skipped (' + e.message + ')');
}
