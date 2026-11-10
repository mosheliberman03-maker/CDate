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

/* App version (owner, v54: update the app from inside the app). native/app-version.txt holds the number; the web app's
   CC_APK says which number it needs, reads it back with App.getInfo().build and offers "Update the app" when it's older. */
try {
  const v = parseInt(fs.readFileSync('app-version.txt', 'utf8'), 10) || 1;
  let g = fs.readFileSync(gradle, 'utf8');
  g = g.replace(/versionCode\s+\d+/, 'versionCode ' + v).replace(/versionName\s+"[^"]*"/, 'versionName "' + v + '"');
  fs.writeFileSync(gradle, g);
  console.log('patch-android: app version ' + v);
} catch (e) {
  console.log('patch-android: app version not set (' + e.message + ')');
}

/* v81 (owner: Chabad.org English): our own Android code (native/android-src/java → the generated project). MainActivity
   registers PageGrabPlugin, a hidden WebView that opens chabad.org pages like a real browser (plain downloads get HTTP 403). */
try {
  const path = require('path');
  const copyDir = (a, b) => { fs.mkdirSync(b, { recursive: true }); for (const f of fs.readdirSync(a)) { const s = path.join(a, f), d = path.join(b, f); if (fs.statSync(s).isDirectory()) copyDir(s, d); else fs.copyFileSync(s, d); } };
  copyDir('android-src/java', 'android/app/src/main/java');
  console.log('patch-android: own Java code copied (MainActivity + PageGrabPlugin)');
} catch (e) {
  console.log('patch-android: own Java code NOT copied (' + e.message + ')');
  process.exit(1);
}

/* App version 6 (owner): the home-screen widget. Copies its layout/resources and registers it in the manifest. */
try {
  const path = require('path');
  const copyDir = (a, b) => { fs.mkdirSync(b, { recursive: true }); for (const f of fs.readdirSync(a)) { const s = path.join(a, f), d = path.join(b, f); if (fs.statSync(s).isDirectory()) copyDir(s, d); else fs.copyFileSync(s, d); } };
  copyDir('android-src/res', 'android/app/src/main/res');
  let x = fs.readFileSync(man, 'utf8');
  if (!x.includes('.CalWidget')) {
    const add = `
        <receiver android:name=".CalWidget" android:exported="true" android:label="Chabad Calendar">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
                <action android:name="android.intent.action.TIME_SET" />
                <action android:name="android.intent.action.DATE_CHANGED" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/cc_widget_info" />
        </receiver>
`;
    if (!x.includes('</application>')) throw new Error('no </application> in the manifest');
    x = x.replace('</application>', add + '    </application>');
    fs.writeFileSync(man, x);
  }
  console.log('patch-android: widget resources copied and registered');
} catch (e) {
  console.log('patch-android: widget NOT added (' + e.message + ')');
  process.exit(1);
}
