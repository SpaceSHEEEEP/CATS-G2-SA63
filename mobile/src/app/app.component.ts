import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Capacitor } from '@capacitor/core';
import type { PluginListenerHandle } from '@capacitor/core';
import { App as NativeApp } from '@capacitor/app';
import { StatusBar } from '@capacitor/status-bar';
import { CapacitorBarcodeScanner } from '@capacitor/barcode-scanner';
import { vault } from './vault';
import { daysBetween, parseProvisioningUri, parseSecret, totp } from './totp';

type Tab = 'courses' | 'history' | 'planner' | 'notes' | 'authenticator' | 'settings';
interface Course { id: number; title: string; category: string; start: string; end: string; provider: string; location: string; duration: string; fee: string; }
interface Application { id: number; course: string; category: string; status: string; start: string; end: string; fee: string; managerReason: string | null; }
interface Holiday { date: string; name: string; }
interface LedgerEntry { date: string; category: string; description: string; approvedFee: number; reimbursement: number; }
interface Snapshot { syncedAt: string; year: number; employee: string; courses: Course[]; applications: Application[];
  holidays: Holiday[]; remainingDays: number; remainingBudget: number; ledger: LedgerEntry[]; }

@Component({
  selector: 'cats-app', standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {

  readonly currentTab = signal<Tab>('authenticator');
  readonly menuOpen = signal(false);
  readonly snapshot = signal<Snapshot | null>(null);
  readonly otp = signal('••••••');
  readonly seconds = signal(30);
  readonly status = signal('Welcome. Connect once to save your training details for offline use.');
  readonly busy = signal(false);
  readonly isNative = Capacitor.isNativePlatform();
  private timer: ReturnType<typeof setInterval> | null = null;
  private urlListener: PluginListenerHandle | null = null;
  serverUrl = '';
  pairingCode = '';
  search = '';
  secretInput = '';
  private savedTotpSecret = '';
  plannerStart = '';
  plannerEnd = '';
  plannerHalf = false;
  plannerResult = '';
  note = '';
  get courses(): Course[] {
    const value = this.search.toLowerCase().trim();
    return (this.snapshot()?.courses || []).filter(c => !value ||
      [c.title, c.category, c.provider].some(x => x.toLowerCase().includes(value)));
  }
  get applications(): Application[] { return this.snapshot()?.applications || []; }
  get holidays(): Holiday[] { return this.snapshot()?.holidays || []; }

  async ngOnInit(): Promise<void> {
    if (this.isNative) {
      // Keep WebView content below Android's system status bar.
      try {
        await StatusBar.setOverlaysWebView({ overlay: false });
        await StatusBar.setBackgroundColor({ color: '#0d3b56' });
      } catch { /* CSS safe-area insets remain as a fallback. */ }
    }
    try {
      this.serverUrl = await vault.get('serverUrl') || '';
      this.savedTotpSecret = await vault.get('totpSeed') || '';
      this.note = await vault.get('privateNote') || '';
      const cached = await vault.get('snapshot');
      if (cached) {
        this.snapshot.set(JSON.parse(cached) as Snapshot);
        this.status.set('Saved snapshot loaded. Browse even without a connection.');
      }
      await this.updateOtp();
    } catch {
      this.status.set('Could not decrypt local data. Unlock your device or reset the local vault.');
    }
    if (this.isNative) {
      try {
        this.urlListener = await NativeApp.addListener('appUrlOpen', event => {
          void this.handleAppLink(event.url);
        });
        const launch = await NativeApp.getLaunchUrl();
        if (launch?.url) await this.handleAppLink(launch.url);
      } catch { this.status.set('Unable to register Android app links. The authenticator remains available in the menu.'); }
    }
    this.timer = setInterval(() => { void this.updateOtp(); }, 1000);
  }
  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
    if (this.urlListener) void this.urlListener.remove();
  }

  private async handleAppLink(raw: string): Promise<void> {
    let link: URL;
    try { link = new URL(raw); } catch { return; }
    if (link.protocol !== 'catsmobile:' || !['authenticator', 'enroll'].includes(link.hostname)) return;
    this.open('authenticator');
    if (link.hostname !== 'enroll') return;
    const uri = link.searchParams.get('uri');
    if (!uri) return;
    try {
      const seed = parseProvisioningUri(uri);
      if (this.savedTotpSecret && !confirm('Replace the existing authenticator on this device?')) return;
      this.secretInput = seed;
      this.status.set('CATS MFA setup key received. Tap Save authenticator securely to confirm.');
    } catch { this.status.set('Unsupported MFA setup link. Scan the CATS QR or enter the setup key manually.'); }
  }
  open(tab: Tab): void { this.currentTab.set(tab); this.menuOpen.set(false); }
  toggleMenu(): void { this.menuOpen.update(open => !open); }
  closeMenu(): void { this.menuOpen.set(false); }
  @HostListener('document:keydown.escape')
  escapeMenu(): void { this.closeMenu(); }

  async saveAuthenticator(): Promise<void> {
    try {
      const seed = this.secretInput.toUpperCase().replace(/[\s-]/g, '');
      parseSecret(seed);
      await vault.set('totpSeed', seed);
      this.savedTotpSecret = seed;
      this.secretInput = '';
      await this.updateOtp();
      this.status.set('Authenticator saved to Android Keystore encrypted storage. Confirm a code in the browser enrolment screen.');
    } catch (error) { this.status.set(this.message(error)); }
  }
  async scanAuthenticatorQr(): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    try {
      const result = await CapacitorBarcodeScanner.scanBarcode({
        hint: 0, // QR_CODE
        scanInstructions: 'Scan the CATS MFA setup QR code shown in your browser'
      });
      if (!result.ScanResult) return; // cancelled
      const seed = parseProvisioningUri(result.ScanResult);
      if (this.savedTotpSecret && !confirm('Replace the existing authenticator on this device?')) return;
      this.secretInput = seed;
      await this.saveAuthenticator();
    } catch (error) {
      this.status.set(this.message(error));
    } finally {
      this.busy.set(false);
    }
  }

  async eraseAuthenticator(): Promise<void> {
    if (!confirm('Remove this authenticator from this device? You may lose access to CATS without another enrolled device.')) return;
    await vault.remove('totpSeed'); this.savedTotpSecret = ''; this.otp.set('••••••');
    this.status.set('Local TOTP seed removed. Your server MFA setting was not reset.');
  }
  private async updateOtp(): Promise<void> {
    this.seconds.set(30 - (Math.floor(Date.now() / 1000) % 30));
    if (this.savedTotpSecret) {
      try { this.otp.set(await totp(this.savedTotpSecret)); }
      catch { this.otp.set('ERROR'); }
    }
  }
  async copyOtp(): Promise<void> {
    if (this.otp().length === 6 && /^\d{6}$/.test(this.otp())) {
      try { await navigator.clipboard.writeText(this.otp()); this.status.set('Code copied.'); }
      catch { this.status.set('Clipboard unavailable. Read the displayed code instead.'); }
    }
  }

  private normalizeUrl(): string {
    const parsed = new URL(this.serverUrl.trim());
    if (parsed.protocol !== 'https:' || parsed.username || parsed.password || parsed.hash || parsed.search)
      throw new Error('Use a CATS HTTPS origin, e.g. https://cats.example.org');
    return parsed.origin;
  }
  private message(error: unknown): string { return error instanceof Error ? error.message : 'An unexpected error occurred.'; }

  async pair(): Promise<void> {
    this.busy.set(true);
    try {
      const url = this.normalizeUrl();
      if (!/^[A-Za-z0-9_-]{32}$/.test(this.pairingCode.trim())) throw new Error('Enter the full pairing code from CATS web.');
      const response = await fetch(url + '/api/mobile/pair', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pairingCode: this.pairingCode.trim(), deviceName: 'CATS Mobile Android' })
      });
      if (!response.ok) throw new Error('Pairing failed or expired. Generate another code on the CATS website.');
      const body = await response.json() as { accessToken: string };
      if (!body.accessToken) throw new Error('Server did not issue a token.');
      await vault.set('deviceToken', body.accessToken);
      await vault.set('serverUrl', url);
      this.serverUrl = url;
      this.pairingCode = '';
      this.status.set('Device paired. You can now download a read-only snapshot.');
      await this.sync();
    } catch (error) { this.status.set(this.message(error)); }
    finally { this.busy.set(false); }
  }

  async sync(): Promise<void> {
    this.busy.set(true);
    try {
      const token = await vault.get('deviceToken');
      if (!token) throw new Error('Pair the device to CATS first. Previously saved information remains available offline.');
      const url = this.normalizeUrl();
      const response = await fetch(url + '/api/mobile/snapshot', {
        headers: { Authorization: 'Bearer ' + token }, cache: 'no-store'
      });
      if (response.status === 401) throw new Error('Mobile access expired or revoked. Pair again. Offline data remains saved.');
      if (!response.ok) throw new Error('Sync failed (HTTP ' + response.status + '). Offline data remains saved.');
      const body = await response.json() as Snapshot;
      if (!Array.isArray(body.courses) || !Array.isArray(body.applications) || !Array.isArray(body.holidays))
        throw new Error('Unexpected response from server.');
      await vault.set('snapshot', JSON.stringify(body));
      this.snapshot.set(body);
      this.status.set('Synced ' + new Date(body.syncedAt).toLocaleString() + '. Data can now be used offline.');
    } catch (error) { this.status.set(this.message(error)); }
    finally { this.busy.set(false); }
  }
  async disconnect(): Promise<void> {
    if (!confirm('Remove this device’s server access token? Your offline snapshot and authenticator remain on this device.')) return;
    await vault.remove('deviceToken');
    this.status.set('Pairing removed locally. Revoke this device in the CATS website to invalidate its server token.');
  }
  async clearOffline(): Promise<void> {
    if (!confirm('Delete downloaded course, application and budget records from this device?')) return;
    await vault.remove('snapshot'); this.snapshot.set(null);
    this.status.set('Offline snapshot deleted. Your authenticator is unaffected.');
  }
  async storeNote(): Promise<void> {
    try { await vault.set('privateNote', this.note.slice(0, 10000)); this.status.set('Encrypted personal note saved locally, never sent to CATS.'); }
    catch { this.status.set('Could not save the note securely.'); }
  }
  calculate(): void {
    try {
      const days = daysBetween(this.plannerStart, this.plannerEnd, this.plannerHalf, this.holidays.map(h => h.date));
      const endYear = Number(this.plannerEnd.slice(0,4));
      const firstYear = Number(this.plannerStart.slice(0,4));
      const cachedYear = this.snapshot()?.year;
      const caveat = !cachedYear || firstYear < cachedYear || endYear > cachedYear + 1
        ? ' (public holidays outside your saved years may be missing)' : '';
      this.plannerResult = days + ' training day(s)' + caveat + '. Estimate only; server validates applications.';
    } catch (error) { this.plannerResult = this.message(error); }
  }
}
