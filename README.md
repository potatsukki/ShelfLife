<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# ShelfLife

ShelfLife is an Android pantry companion for tracking ingredients, reducing food waste, scanning products, managing shopping lists, and generating recipe ideas through a secure Firebase Functions AI proxy.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a Firebase project, enable Email/Password authentication, and add `app/google-services.json`.
5. Deploy Firebase Functions from the `functions` directory and set `OPENROUTER_API_KEY` and `PEXELS_API_KEY` as Functions secrets.
6. Run the app on an emulator or physical device.
