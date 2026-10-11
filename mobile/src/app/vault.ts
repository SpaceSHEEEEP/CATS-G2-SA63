import { Capacitor, registerPlugin } from '@capacitor/core';

interface NativeVault {
  set(options: { key: string; value: string }): Promise<void>;
  get(options: { key: string }): Promise<{ value: string | null }>;
  remove(options: { key: string }): Promise<void>;
}

const native = registerPlugin<NativeVault>('CatsVault');
export const vault = {
  async get(key: string): Promise<string | null> {
    if (Capacitor.isNativePlatform()) return (await native.get({ key })).value;
    return sessionStorage.getItem('cats-preview-' + key);
  },
  async set(key: string, value: string): Promise<void> {
    if (Capacitor.isNativePlatform()) return native.set({ key, value });
    sessionStorage.setItem('cats-preview-' + key, value);
  },
  async remove(key: string): Promise<void> {
    if (Capacitor.isNativePlatform()) return native.remove({ key });
    sessionStorage.removeItem('cats-preview-' + key);
  }
};
