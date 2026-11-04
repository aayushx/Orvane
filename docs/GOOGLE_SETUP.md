# Google sign-in setup (G3–G5)

Orvane's Google features (Calendar sync, Drive backup) use **Google Identity
Services via Credential Manager**. The app itself never stores secrets — the
only configuration it needs is a **Web application client ID**, supplied at
build time from `local.properties` (gitignored).

## 1. Create a Google Cloud project

1. Go to <https://console.cloud.google.com/> and create a project (e.g. `Orvane`).
2. **APIs & Services → Library** → enable:
   - **Google Calendar API** (G4: two-way sync)
   - **Google Drive API** (G5: AppData backup)

## 2. Configure the OAuth consent screen

**APIs & Services → OAuth consent screen**

- User type: **External** (test mode is fine for development).
- App name: `Orvane`; add your own account as a **test user**.
- Scopes can be added later when G4/G5 land; for sign-in itself none are
  required beyond the default `openid` / `email` / `profile`.

## 3. Create the OAuth 2.0 clients

**APIs & Services → Credentials → Create credentials → OAuth client ID**

### a) Android client (authenticates the app itself)

- Application type: **Android**
- Package name: `com.orvane.app`
- SHA-1 (debug, pinned project keystore):
  ```
  59bec58b bfd6399d 786fdd95 4dd6a9f9 2786425a
  ```
  (Get it any time with:
  `keytool -list -v -keystore keystore/orvane-debug.keystore -alias androiddebugkey -storepass android | grep SHA1`)

When a release build exists, create a second Android client with the release
keystore's SHA-1. Multiple Android clients per package are fine.

### b) Web application client (the ID token audience)

- Application type: **Web application** — no redirect URIs needed.
- Copy its **Client ID** (looks like `1234567890-abc.apps.googleusercontent.com`).

This Web client ID is what the app passes to `GetGoogleIdOption.setServerClientId()`.
The Android client authorizes the *app*; the Web client is the *audience* of
the ID token.

## 4. Wire the client ID into the build

Add it to `local.properties` (never committed):

```properties
GOOGLE_WEB_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
```

The Gradle build turns this into `BuildConfig.GOOGLE_WEB_CLIENT_ID`. When the
value is missing the app stays fully usable — the Settings → Account card
shows a "not configured" hint and sign-in is disabled.

## 5. Testing

Sign-in, Calendar sync (G4) and Drive backup (G5) are **manual tests** — the
app must be signed in interactively by a human. See the smoke-test checklists
in the round report.
