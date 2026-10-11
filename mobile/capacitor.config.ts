import type { CapacitorConfig } from '@capacitor/cli';
const config: CapacitorConfig = {
  appId: 'sg.edu.iss.cats.mobile',
  appName: 'CATS Mobile',
  webDir: 'dist/web/browser',
  android: { allowMixedContent: false }
};
export default config;
