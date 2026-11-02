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
