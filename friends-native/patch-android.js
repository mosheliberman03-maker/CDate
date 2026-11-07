/* Runs in CI after `npx cap add android`, before sync/build.
   1) Copies the home-screen widgets (android-src/) into the generated Android project.
   2) Registers them in the app's manifest.
   3) Signs every build with the permanent key (shared with the calendar app: native/cdate-debug.keystore),
      so phones accept updates instead of saying "App not installed". */
const fs = require('fs');
const path = require('path');

function copyDir(from, to) {
  fs.mkdirSync(to, { recursive: true });
  for (const f of fs.readdirSync(from)) {
    const a = path.join(from, f), b = path.join(to, f);
    if (fs.statSync(a).isDirectory()) copyDir(a, b); else fs.copyFileSync(a, b);
  }
}

const main = 'android/app/src/main';
copyDir('android-src/java', path.join(main, 'java'));
copyDir('android-src/res', path.join(main, 'res'));
console.log('patch-android: widget code and layouts copied');

const man = path.join(main, 'AndroidManifest.xml');
let x = fs.readFileSync(man, 'utf8');
if (!x.includes('android.permission.RECEIVE_BOOT_COMPLETED')) {
  x = x.replace('</manifest>', '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n</manifest>');
}
if (!x.includes('.WhoWidget')) {
  const add = `
        <receiver android:name=".WhoWidget" android:exported="true" android:label="Who’s up now">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
                <action android:name="android.intent.action.TIME_SET" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/kit_who_info" />
        </receiver>
        <receiver android:name=".BestWidget" android:exported="true" android:label="Call now">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/kit_best_info" />
        </receiver>
        <service android:name=".WhoWidgetService" android:exported="false"
            android:permission="android.permission.BIND_REMOTEVIEWS" />
        <activity android:name=".WidgetAction" android:exported="false"
            android:theme="@style/KitInvisible" android:excludeFromRecents="true"
            android:noHistory="true" android:taskAffinity="" />
`;
  if (!x.includes('</application>')) { console.log('patch-android: no </application> in the manifest'); process.exit(1); }
  x = x.replace('</application>', add + '    </application>');
}
fs.writeFileSync(man, x);
console.log('patch-android: widgets registered in the manifest');

const gradle = 'android/app/build.gradle';
let g = fs.readFileSync(gradle, 'utf8');
if (!g.includes('cdate-debug.keystore')) {
  g += `
android.signingConfigs.debug.storeFile = file("\${rootDir}/../../native/cdate-debug.keystore")
android.signingConfigs.debug.storePassword = "android"
android.signingConfigs.debug.keyAlias = "androiddebugkey"
android.signingConfigs.debug.keyPassword = "android"
`;
  fs.writeFileSync(gradle, g);
  console.log('patch-android: permanent signing key set');
}
