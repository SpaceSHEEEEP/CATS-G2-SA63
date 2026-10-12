export function parseSecret(text: string): Uint8Array {
  const normalized = text.toUpperCase().replace(/[\s-]/g, '');
  if (!/^[A-Z2-7]{32}$/.test(normalized)) throw new Error('Use the 32-character Base32 setup key from CATS.');
  const output = new Uint8Array(20);
  let bits = 0, acc = 0, index = 0;
  for (const char of normalized) {
    const digit = char >= 'A' && char <= 'Z' ? char.charCodeAt(0) - 65 : char.charCodeAt(0) - 50 + 26;
    acc = (acc << 5) | digit; bits += 5;
    if (bits >= 8) { bits -= 8; output[index++] = (acc >> bits) & 255; }
  }
  return output;
}

export function parseProvisioningUri(text: string): string {
  let url: URL;
  try { url = new URL(text.trim()); }
  catch { throw new Error('Scan the QR code shown on the CATS MFA setup page.'); }
  if (url.protocol !== 'otpauth:' || url.hostname !== 'totp' ||
      url.searchParams.get('issuer') !== 'CATS NUS-ISS' ||
      !decodeURIComponent(url.pathname.slice(1)).startsWith('CATS NUS-ISS:') ||
      (url.searchParams.get('algorithm') || 'SHA1').toUpperCase() !== 'SHA1' ||
      (url.searchParams.get('digits') || '6') !== '6' ||
      (url.searchParams.get('period') || '30') !== '30') {
    throw new Error('This is not a supported CATS MFA setup code.');
  }
  const seed = (url.searchParams.get('secret') || '').toUpperCase().replace(/[\s-]/g, '');
  parseSecret(seed);
  return seed;
}

export async function totp(secret: string, now = Date.now()): Promise<string> {
  const seed = parseSecret(secret);
  const step = BigInt(Math.floor(now / 30_000));
  const bytes = new ArrayBuffer(8);
  new DataView(bytes).setBigUint64(0, step, false);
  const key = await crypto.subtle.importKey('raw', seed.buffer as ArrayBuffer,
    { name: 'HMAC', hash: 'SHA-1' }, false, ['sign']);
  const digest = new Uint8Array(await crypto.subtle.sign('HMAC', key, bytes));
  const offset = digest[digest.length - 1] & 15;
  const binary = ((digest[offset] & 127) << 24) | ((digest[offset + 1] & 255) << 16) |
    ((digest[offset + 2] & 255) << 8) | (digest[offset + 3] & 255);
  return String(binary % 1_000_000).padStart(6, '0');
}
export function daysBetween(start: string, end: string, halfDay: boolean, holidayDates: string[]): number {
  const dates = new Set(holidayDates);
  const startMs = Date.parse(start + 'T12:00:00Z');
  const endMs = Date.parse(end + 'T12:00:00Z');
  if (!Number.isFinite(startMs) || !Number.isFinite(endMs) || endMs < startMs || endMs - startMs > 366 * 86400000)
    throw new Error('Enter a valid course date range of up to one year.');
  if (halfDay && startMs !== endMs) throw new Error('Half-day sessions must be on one date.');
  let total = 0;
  for (let day = startMs; day <= endMs; day += 86400000) {
    const date = new Date(day);
    const iso = date.toISOString().slice(0, 10);
    if (date.getUTCDay() !== 0 && date.getUTCDay() !== 6 && !dates.has(iso)) total += halfDay ? .5 : 1;
  }
  return total;
}
