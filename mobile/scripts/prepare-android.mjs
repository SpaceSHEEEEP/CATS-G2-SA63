import { existsSync, mkdirSync, copyFileSync, cpSync, readFileSync, writeFileSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

const root = resolve(import.meta.dirname, '..');
process.chdir(root);
const npx = process.platform === 'win32' ? 'npx.cmd' : 'npx';
function run(...args) {
  const result = spawnSync(npx, ['--no-install', ...args], { stdio: 'inherit', shell: false });
  if (result.status !== 0) throw new Error(`npx ${args.join(' ')} failed (${result.status})`);
}
if (!existsSync('dist/web/browser/index.html')) {
  throw new Error('Build Angular first: npm run build');
}
if (!existsSync('android/app/build.gradle')) run('cap', 'add', 'android');
run('cap', 'sync', 'android');

const androidStylesPath = join(root, 'android/app/src/main/res/values/styles.xml');
let androidStyles = readFileSync(androidStylesPath, 'utf8');
const optOutInset = '<item name="android:windowOptOutEdgeToEdgeEnforcement">true</item>';
if (!androidStyles.includes(optOutInset)) {
  const themePattern = /(<style name="AppTheme.NoActionBar"[^>]*>)/;
  if (!themePattern.test(androidStyles)) throw new Error('Cannot locate Capacitor app theme');
  androidStyles = androidStyles.replace(themePattern, `$1\n        ${optOutInset}`);
  writeFileSync(androidStylesPath, androidStyles);
}

cpSync(join(root, 'native/android/res'), join(root, 'android/app/src/main/res'),
       { recursive: true, force: true });

// Capacitor 7 generates minSdk 23; the official QR scanner needs Android 26.
const variablesFile = join(root, 'android/variables.gradle');
let variables = readFileSync(variablesFile, 'utf8');
const minSdk = variables.match(/minSdkVersion\s*=\s*(\d+)/);
if (!minSdk) throw new Error('Cannot locate Android minSdkVersion');
if (Number(minSdk[1]) < 26) {
  variables = variables.replace(/minSdkVersion\s*=\s*\d+/, 'minSdkVersion = 26');
  writeFileSync(variablesFile, variables);
}

const targetPackage = join(root, 'android/app/src/main/java/sg/edu/iss/cats/mobile');
mkdirSync(targetPackage, { recursive: true });
for (const filename of ['MainActivity.java', 'CatsVaultPlugin.java']) {
  copyFileSync(join(root, 'native/android/java/sg/edu/iss/cats/mobile', filename), join(targetPackage, filename));
}
const manifestFile = join(root, 'android/app/src/main/AndroidManifest.xml');
let manifest = readFileSync(manifestFile, 'utf8');
if (manifest.includes('android:allowBackup="true"')) {
  manifest = manifest.replace('android:allowBackup="true"', 'android:allowBackup="false"');
} else if (!manifest.includes('android:allowBackup=')) {
  manifest = manifest.replace('<application', '<application android:allowBackup="false"');
}
if (manifest.includes('android:usesCleartextTraffic="true"')) {
  manifest = manifest.replace('android:usesCleartextTraffic="true"', 'android:usesCleartextTraffic="false"');
}
// Android deep links let an authenticated browser open the installed CATS
// Mobile authenticator directly, without using the web SPA as a substitute.
// No application token or shared TOTP seed is ever sent to an HTTPS endpoint.
if (!manifest.includes('android:scheme="catsmobile"')) {
  const firstActivityEnd = manifest.indexOf('</activity>');
  if (firstActivityEnd < 0) throw new Error('Cannot locate the Capacitor MainActivity');
  const filter = `        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="catsmobile" />
        </intent-filter>
`;
  manifest = manifest.slice(0, firstActivityEnd) + filter + manifest.slice(firstActivityEnd);
}
writeFileSync(manifestFile, manifest);

const gradleFile = join(root, 'android/app/build.gradle');
let gradle = readFileSync(gradleFile, 'utf8');
const marker = '// CATS_SIGNING_CONFIG_V1';
if (!gradle.includes(marker)) {
  const signing = `
    ${marker}
    if (System.getenv("ANDROID_KEYSTORE_FILE")) {
        signingConfigs {
            catsRelease {
                storeFile file(System.getenv("ANDROID_KEYSTORE_FILE"))
                storePassword System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias System.getenv("ANDROID_KEY_ALIAS")
                keyPassword System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
        buildTypes.release.signingConfig signingConfigs.catsRelease
    }
`;
  if (!gradle.includes('android {')) throw new Error('Capacitor Android Gradle configuration not recognized');
  gradle = gradle.replace('android {', 'android {' + signing);
  writeFileSync(gradleFile, gradle);
}
console.log('Capacitor Android project synchronized; native Keystore vault registered.');
