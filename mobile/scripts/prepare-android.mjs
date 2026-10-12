import { existsSync, mkdirSync, copyFileSync, readFileSync, writeFileSync } from 'node:fs';
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
