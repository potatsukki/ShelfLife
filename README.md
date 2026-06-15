# ShelfLife

ShelfLife is an Android pantry companion for tracking ingredients, reducing food waste, scanning products, managing shopping lists, and generating recipe ideas.

The Android app uses Firebase Auth for sign-in and a Cloudflare Worker as the AI proxy. OpenRouter and Pexels keys stay in Cloudflare secrets, never in Android source or inside the APK.

## Run Locally

**Prerequisites**

- Android Studio
- Firebase project with Email/Password and Google sign-in enabled
- `app/google-services.json`
- Node.js, for deploying the Cloudflare Worker
- Cloudflare account
- OpenRouter API key
- Pexels API key, optional but recommended for recipe images

## Android Setup

1. Open this folder in Android Studio.
2. Add your Firebase config file at `app/google-services.json`.
3. Create `.env` in the project root.
4. Add your deployed Worker URL:

```text
SHELFLIFE_WORKER_BASE_URL="https://shelflife-api.<your-cloudflare-subdomain>.workers.dev"
```

5. Sync Gradle and run the app on an emulator or physical device.

Do not put `OPENROUTER_API_KEY` or `PEXELS_API_KEY` in `.env`. Those belong only in Cloudflare Worker secrets.

## Cloudflare Worker Setup

From the project root:

```powershell
cd worker
npm install
npx wrangler login
npx wrangler secret put OPENROUTER_API_KEY
npx wrangler secret put PEXELS_API_KEY
npx wrangler deploy
```

After deploy, copy the Worker URL into the Android `.env` file as `SHELFLIFE_WORKER_BASE_URL`.

## Notes

- Cloudflare Workers avoid the Firebase Functions Blaze requirement.
- Firebase Auth still protects the Android app data locally per user.
- The Worker verifies Firebase ID tokens when `FIREBASE_PROJECT_ID` is configured in `worker/wrangler.toml`.
- Open Food Facts remains the first barcode source. The AI barcode fallback runs only after Open Food Facts does not return a product.
