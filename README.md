# Course Application Tracking System (CATS)

CATS is a staff-training application built with Java 21, Spring Boot 4, Thymeleaf, MySQL and Flyway. Employees submit course applications, managers approve training and reimbursement claims, and administrators manage staff, catalogues and entitlements. An Angular/Capacitor Android app provides optional read-only synchronisation, offline planning/notes and offline TOTP authentication codes.

## Requirements

- **Docker route:** Docker Engine or Docker Desktop and Docker Compose v2.
- **Source builds:** Java 21; Node.js 22 and npm for the mobile/Angular frontend.
- **Android APK builds:** Android SDK, Java 21, Node.js 22; a signing keystore for release APKs.
- **Database:** MySQL 8.4 or another compatible MySQL 8 server, reachable from the application.

## Local quick start

1. Run `cp .env.example .env` and edit `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` (different strong passwords). Generate `MFA_ENCRYPTION_KEY` with `openssl rand -base64 32` and store it safely; changing it invalidates enrolled authenticators.
2. For an **empty database only**, set `BOOTSTRAP_ADMIN_ENABLED=true` and the `BOOTSTRAP_ADMIN_*` fields. No demo accounts or courses are created automatically.
3. Run:

   ```sh
   docker compose up --build -d
   docker compose ps
   docker compose logs -f app
   ```

4. Open `http://localhost:8080/admin/login` to sign in with the newly created administrator. Complete MFA enrollment with CATS Mobile or another TOTP authenticator.
5. Set `BOOTSTRAP_ADMIN_ENABLED=false`, remove the bootstrap password from `.env`, and run `docker compose up -d` again.

The local Compose file includes a persistent MySQL volume. `docker compose down` stops the services; **`down -v` also deletes the database volume**.

## Application configuration

CATS reads its settings from environment variables. Key settings:

| Setting | Description |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | JDBC URL and application database credentials |
| `MFA_ENCRYPTION_KEY` | Stable Base64-encoded 32-byte key for encrypted TOTP seeds |
| `PUBLIC_BASE_URL` | External application URL used in email links, normally HTTPS |
| `SESSION_COOKIE_SECURE` | `true` for HTTPS browser access; `false` for local HTTP development only |
| `SMTP_HOST`, `SMTP_PORT`, `ADMIN_MAIL_USERNAME`, `ADMIN_MAIL_PASSWORD` | Optional SMTP delivery for workflow notifications and directory exports |
| `BOOTSTRAP_ADMIN_*` | One-time administrator provisioning on an empty database |
| `MOBILE_APK_PATH` | Location of the signed Android APK offered in the employee download page |

Flyway runs automatically and Hibernate validates the schema. **Start with an empty database when installing a new instance.** Apply reviewed data-preserving migrations for existing deployments, back up before changes, and do not use `flyway clean` on valuable data.

## Production Docker and database deployment

The application can connect to an existing MySQL server or run MySQL as a separate container in the **same Compose project**. This is a deployment choice, independent of how you invoke Docker Compose (manually, by script, or from CI/CD).

| `DEPLOY_DB_MODE` | Result |
|---|---|
| `external` (default) | Only the CATS app container starts. It uses `DB_URL` from the production environment file. |
| `container` | Compose also starts MySQL 8.4, with a persistent volume and no exposed database port. The app automatically uses the Compose database service. |

1. Install Docker Engine and Compose v2 on the host and copy `deploy/` into your deployment directory (the examples use `/opt/cats`).
2. Copy `deploy/.env.production.example` to `/opt/cats/.env.production` and set `DEPLOY_DB_MODE`, database credentials, `CATS_IMAGE`, `PUBLIC_BASE_URL`, and `MFA_ENCRYPTION_KEY`. Set file permissions with `chmod 600 /opt/cats/.env.production`. Do not commit credentials to source control.
3. Run the stack helper from the deployment directory:

   ```sh
   cd /opt/cats
   sh deploy/stack.sh mode                # confirm which DB mode is selected
   # Only in container mode: sh deploy/stack.sh up -d --wait db
   sh deploy/stack.sh pull cats
   sh deploy/stack.sh up -d cats
   sh deploy/stack.sh ps
   ```

`deploy/stack.sh` reads `DEPLOY_DB_MODE` from `.env.production`. An exported environment variable with the same name overrides the file for that invocation (for example, `DEPLOY_DB_MODE=container sh deploy/stack.sh ps`). Use a real `DB_URL` with appropriate transport security when connecting to external MySQL. The container database is private to Compose and retains data when the application is updated.

**Changing modes does not migrate data.** Back up and transfer database contents explicitly before switching. Retain the persistent database volume; `docker compose down -v` removes volumes.

### HTTPS and reverse proxies

Use HTTPS for **public browser and Android access**. TLS may terminate at a load balancer, edge proxy, reverse proxy, or the origin; choose the deployment architecture and certificates appropriate for your environment. Spring Boot listens on plain HTTP **inside its container** (port 8080); the production host mapping is configured with `CATS_HTTP_BIND_ADDRESS` and `CATS_HTTP_PORT`. For a local reverse proxy, `127.0.0.1:8080` is a useful host mapping; for an external ingress, configure the reachable listener and firewall explicitly.

Configure the TLS terminator to preserve `Host` and supply correct `X-Forwarded-Proto` / `X-Forwarded-Host` headers from **trusted proxies only**. Spring uses these headers when producing browser URLs (`server.forward-headers-strategy=framework`). Set `PUBLIC_BASE_URL=https://...` and `SESSION_COOKIE_SECURE=true`. Do not expose an unencrypted origin directly to the public internet; use trusted networks, authenticated tunnels, or origin TLS where possible. Test login redirects, session cookies and a signed APK download through the real public URL.

## Manual GitHub Actions build and deployment

The workflow `.github/workflows/manual-build-test-package-publish-deploy.yml` runs **only when manually started** with **Actions → Manual CATS build, test, Android package, image publish and deploy → Run workflow**.

- **Deploy unchecked (default):** run Java tests on Linux, Windows and macOS; build Angular and a debug Android APK; upload test and build artifacts. No image is published and no server is changed.
- **Deploy checked:** also sign the Android release APK, publish the container image to GHCR, and deploy that image and APK to the production environment after successful checks. The deployment verifies container health and attempts rollback on failure.

Protect the `production` GitHub Environment with required reviewers as appropriate. Configure connection secrets `DEPLOY_HOST`, `DEPLOY_USERNAME`, `DEPLOY_PASSWORD`, `DEPLOY_HOST_FINGERPRINT` (a verified SSH host fingerprint) and, if needed, `DEPLOY_SSH_PORT`. Release builds additionally require `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`, `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_PASSWORD`.

Provision `/opt/cats/.env.production` on the server before deploying; database, MFA and SMTP credentials are never uploaded by the workflow. As with any other deployment caller, the workflow uses the server's database-mode setting unless explicitly overridden through its environment. Image rollback does not reverse database migrations: back up before schema upgrades.

## Build and test from source

```sh
./gradlew clean test bootJar             # Java 21; uses the test profile / H2
cd mobile && npm install
npm run build:web                         # Angular browser assets
npm run android:debug                     # Android SDK and Java 21 required
```

For a combined backend + mobile build, use `./scripts/build-all.sh`. `docker build -t cats:local .` also compiles the web assets and Spring Boot image. Native Android projects are generated/synchronised by Capacitor as described in `mobile/package.json` and `mobile/scripts/prepare-android.mjs`; keep the mobile signing keystore outside the repository.

## Mobile app and MFA

1. Download a signed Android APK from **My Training → Mobile / MFA** after the server administrator has published it. Android is supported; iOS release packaging is not supplied.
2. Log in on the website with username and password, then enroll MFA by scanning the QR code with a TOTP authenticator or entering the manual setup key in CATS Mobile's **Authenticator** tab. Codes work offline, change every 30 seconds, and require accurate device time.
3. After web MFA, generate a temporary pairing code on the **Mobile / MFA** page. Enter the public **HTTPS** site address and the pairing code in CATS Mobile to perform the initial read-only synchronisation.
4. Use cached courses, training history, entitlement snapshots, training-day estimates, and encrypted local notes offline. Refreshing server data requires Internet. Revoke a lost device from the website. Never share MFA setup keys or pairing codes.

Logins, MFA verification, selected administrative email actions, reimbursement uploads and pairing-code generation use three interactive human-verification challenges: icon sequence, orienting a cat toward food, and matching the same cat among transformed illustrations. These challenges require JavaScript. If a user cannot operate a challenge, contact a system administrator for accessibility assistance.

## Troubleshooting

| Symptom | Check |
|---|---|
| App not healthy | `sh deploy/stack.sh ps` and `sh deploy/stack.sh logs cats`; database connectivity and Flyway logs |
| Bundled database does not start | `DEPLOY_DB_MODE=container`, `MYSQL_ROOT_PASSWORD`, persistent volume permissions and DB health |
| External DB connection fails | JDBC URL, network/firewall reachability, credentials and MySQL server TLS configuration |
| Redirect loop or missing session | Public HTTPS URL, proxy forwarding headers, `SESSION_COOKIE_SECURE`, MFA completion |
| Human verification does not load | Enable browser JavaScript and check the `/captcha/new` response; request a fresh challenge |
| No Android download | Signed APK at the configured `MOBILE_APK_PATH` |
| Notifications remain queued | SMTP settings, public base URL and application logs |
