const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "access-control-allow-origin": "*",
  "access-control-allow-methods": "POST, OPTIONS",
  "access-control-allow-headers": "content-type, authorization"
};

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: JSON_HEADERS });
    }

    if (request.method !== "POST") {
      return json({ error: "Use POST." }, 405);
    }

    const url = new URL(request.url);
    try {
      await requireFirebaseUser(request, env);
      const body = await request.json();
      if (url.pathname === "/generateRecipes") {
        return json(await generateRecipes(body, env));
      }
      if (url.pathname === "/chatAssistant") {
        return json(await chatAssistant(body, env));
      }
      if (url.pathname === "/identifyBarcodeFallback") {
        return json(await identifyBarcodeFallback(body, env));
      }
      return json({ error: "Unknown endpoint." }, 404);
    } catch (error) {
      const message = error && error.message ? error.message : "Unexpected Worker error.";
      const status = message.includes("Unauthorized") ? 401 : message.includes("rate limit") ? 429 : 500;
      return json({ error: message }, status);
    }
  }
};

async function generateRecipes(body, env) {
  const pantry = Array.isArray(body.ingredients)
    ? body.ingredients
    : Array.isArray(body.pantryIngredients)
      ? body.pantryIngredients
      : [];
  const pantryText = pantry.length
    ? pantry.map((item) => `- ${item.name}: ${item.quantity} ${item.unit}, category ${item.category}, location ${item.location}, expires ${item.expirationDate}`).join("\n")
    : "No pantry ingredients are available.";

  const prompt = `Generate 2 practical recipes for this pantry:\n${pantryText}\n\nReturn raw JSON only with this exact shape: {"recipes":[{"id":"stable_slug","name":"Recipe Name","prepTime":"20 min","difficulty":"Easy","whySuggested":"short pantry-based reason","ingredients":[{"name":"Ingredient","quantity":1,"unit":"cup","required":true}],"steps":[{"text":"Step one"}]}]}. Use pantry items first. Mark ingredients the user does not have as required true; do not claim missing ingredients are available.`;

  const content = await callDeepSeek(env, [
    {
      role: "system",
      content: "You are ShelfLife's recipe engine. Return valid JSON only. Prefer recipes that reduce food waste and use pantry items first."
    },
    { role: "user", content: prompt }
  ], true);

  const parsed = parseJson(content);
  const recipes = Array.isArray(parsed.recipes) ? parsed.recipes : Array.isArray(parsed) ? parsed : [];
  const enriched = [];
  for (const recipe of recipes.slice(0, 4)) {
    const image = await findRecipeImage(env, recipe.name);
    const ingredients = normalizeIngredients(recipe.ingredients, recipe.ingredientsCsv);
    const steps = normalizeStepObjects(recipe.steps, recipe.stepsCsv);
    enriched.push({
      id: sanitizeId(recipe.id || recipe.name || crypto.randomUUID()),
      name: stringOr(recipe.name, "Generated Recipe"),
      prepTime: stringOr(recipe.prepTime, "20 min"),
      difficulty: stringOr(recipe.difficulty, "Easy"),
      imageUrl: image?.imageUrl || stringOr(recipe.imageUrl || recipe.imageResUrl, ""),
      imageResUrl: image?.imageUrl || stringOr(recipe.imageUrl || recipe.imageResUrl, ""),
      imageProvider: image?.imageProvider || "",
      photographerName: image?.photographerName || "",
      photographerUrl: image?.photographerUrl || "",
      photoPageUrl: image?.photoPageUrl || "",
      whySuggested: stringOr(recipe.whySuggested, "Generated from your current pantry."),
      ingredients,
      steps,
      ingredientsCsv: ingredients.map((item) => `${item.name}${item.quantity ? ` (${item.quantity}${item.unit ? ` ${item.unit}` : ""})` : ""}`).join(", "),
      stepsCsv: steps.map((step) => step.text).join("|")
    });
  }

  return { recipes: enriched };
}

async function chatAssistant(body, env) {
  const history = Array.isArray(body.history) ? body.history.slice(-12) : [];
  const pantry = Array.isArray(body.pantry)
    ? body.pantry
    : Array.isArray(body.pantryIngredients)
      ? body.pantryIngredients
      : [];
  const recipeContext = body.recipeContext && typeof body.recipeContext === "object" ? body.recipeContext : null;
  const pantryText = pantry.length
    ? pantry.map((item) => `${item.name} (${item.quantity} ${item.unit}, expires ${item.expirationDate || "unknown"})`).join(", ")
    : "empty pantry";
  const contextText = recipeContext
    ? `Current recipe: ${recipeContext.recipeName}. Available ingredients: ${(recipeContext.availableIngredients || []).join(", ")}. Missing ingredients: ${(recipeContext.missingIngredients || []).join(", ")}. Steps: ${(recipeContext.steps || []).join(" | ")}.`
    : "No selected recipe context.";

  const messages = [
    {
      role: "system",
      content: recipeContext
        ? `You are Kitchen AI inside ShelfLife. Answer specifically about the selected recipe. When the user requests cheaper ingredients, substitutions, vegetarian changes, removals, or another recipe modification, return JSON only with this shape: {"reply":"clear explanation","recipeUpdate":{"summary":"short description","ingredients":[{"name":"ingredient","quantity":1,"unit":"cup","required":true}],"steps":[{"text":"updated step"}]}}. recipeUpdate must contain the complete revised recipe, not only changed items. If no recipe change is requested, set recipeUpdate to null. Current pantry: ${pantryText}. ${contextText}`
        : `You are Kitchen AI inside ShelfLife. Give concise, practical cooking and pantry answers. Current pantry: ${pantryText}. If data is uncertain, say so.`
    },
    ...history.map((item) => ({
      role: item.role === "assistant" ? "assistant" : "user",
      content: String(item.content || "").slice(0, 2000)
    })),
    { role: "user", content: String(body.message || "").slice(0, 2000) }
  ];

  if (!recipeContext) {
    const reply = await callDeepSeek(env, messages, false);
    return { reply };
  }

  const content = await callDeepSeek(env, messages, true);
  try {
    const parsed = parseJson(content);
    return {
      reply: stringOr(parsed.reply, "I prepared an updated version of this recipe."),
      recipeUpdate: normalizeRecipeUpdate(parsed.recipeUpdate)
    };
  } catch (_) {
    return { reply: content, recipeUpdate: null };
  }
}

async function identifyBarcodeFallback(body, env) {
  const barcode = String(body.barcode || "").replace(/\D/g, "").slice(0, 32);
  if (!barcode) {
    return { error: "Barcode is required." };
  }

  const content = await callDeepSeek(env, [
    {
      role: "system",
      content: "You identify grocery products from barcodes. Return valid JSON only. If uncertain, use a conservative generic grocery name and note that it needs review."
    },
    {
      role: "user",
      content: `Identify barcode ${barcode}. Return {"name":"Brand Product","category":"Produce|Dairy|Meat|Grains|Bakery|Beverages|Pantry","quantity":1,"unit":"pcs","location":"Pantry","notes":"short confidence/review note"}.`
    }
  ], true);

  const parsed = parseJson(content);
  return {
    name: stringOr(parsed.name, ""),
    category: stringOr(parsed.category, "Pantry"),
    quantity: Number(parsed.quantity) || 1,
    unit: stringOr(parsed.unit, "pcs"),
    location: stringOr(parsed.location, "Pantry"),
    notes: stringOr(parsed.notes, "AI barcode fallback. Please review before adding.")
  };
}

async function requireFirebaseUser(request, env) {
  if (!env.FIREBASE_PROJECT_ID) {
    throw new Error("Unauthorized: FIREBASE_PROJECT_ID is not configured.");
  }
  const auth = request.headers.get("authorization") || "";
  const token = auth.replace(/^Bearer\s+/i, "").trim();
  if (!token) {
    throw new Error("Unauthorized: missing Firebase ID token.");
  }

  const [headerPart, payloadPart, signaturePart] = token.split(".");
  if (!headerPart || !payloadPart || !signaturePart) {
    throw new Error("Unauthorized: malformed Firebase ID token.");
  }
  const header = JSON.parse(decodeBase64Url(headerPart));
  const payload = JSON.parse(decodeBase64Url(payloadPart));
  const now = Math.floor(Date.now() / 1000);
  const expectedIssuer = `https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}`;

  if (header.alg !== "RS256" || !header.kid) throw new Error("Unauthorized: unsupported token header.");
  if (payload.aud !== env.FIREBASE_PROJECT_ID) throw new Error("Unauthorized: invalid token audience.");
  if (payload.iss !== expectedIssuer) throw new Error("Unauthorized: invalid token issuer.");
  if (!payload.sub) throw new Error("Unauthorized: missing user subject.");
  if (payload.exp <= now || payload.iat > now + 60) throw new Error("Unauthorized: expired or invalid token time.");

  const jwks = await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com").then((res) => res.json());
  const jwk = jwks.keys?.find((key) => key.kid === header.kid);
  if (!jwk) throw new Error("Unauthorized: Firebase signing key not found.");

  const key = await crypto.subtle.importKey(
    "jwk",
    jwk,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"]
  );
  const verified = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    key,
    base64UrlToBytes(signaturePart),
    new TextEncoder().encode(`${headerPart}.${payloadPart}`)
  );
  if (!verified) throw new Error("Unauthorized: invalid token signature.");
  return payload;
}

async function callDeepSeek(env, messages, jsonMode) {
  if (!env.DEEPSEEK_API_KEY) {
    throw new Error("DEEPSEEK_API_KEY is not configured in Cloudflare Worker secrets.");
  }

  const response = await fetch("https://api.deepseek.com/chat/completions", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "authorization": `Bearer ${env.DEEPSEEK_API_KEY}`
    },
    body: JSON.stringify({
      model: env.DEEPSEEK_MODEL || "deepseek-v4-flash",
      messages,
      thinking: { type: "disabled" },
      max_tokens: jsonMode ? 1800 : 1200,
      response_format: jsonMode ? { type: "json_object" } : undefined
    })
  });

  if (!response.ok) {
    const text = await response.text();
    if (response.status === 402) {
      throw new Error("DeepSeek account balance is insufficient.");
    }
    if (response.status === 429) {
      throw new Error("DeepSeek rate limit reached. Try again shortly.");
    }
    throw new Error(`DeepSeek request failed (${response.status}): ${text.slice(0, 240)}`);
  }

  const data = await response.json();
  return data?.choices?.[0]?.message?.content || "";
}

async function findRecipeImage(env, recipeName) {
  if (!env.PEXELS_API_KEY || !recipeName) return null;

  const query = encodeURIComponent(`${recipeName} food`);
  const response = await fetch(`https://api.pexels.com/v1/search?query=${query}&per_page=1&orientation=landscape`, {
    headers: { authorization: env.PEXELS_API_KEY }
  });
  if (!response.ok) return null;

  const data = await response.json();
  const photo = data?.photos?.[0];
  if (!photo?.src?.large) return null;
  return {
    imageUrl: photo.src.large,
    imageProvider: "Pexels",
    photographerName: photo.photographer,
    photographerUrl: photo.photographer_url,
    photoPageUrl: photo.url
  };
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });
}

function parseJson(text) {
  const clean = String(text || "").trim().replace(/^```json\s*/i, "").replace(/^```\s*/i, "").replace(/```$/i, "").trim();
  const firstObject = clean.indexOf("{");
  const firstArray = clean.indexOf("[");
  const startsAt = firstArray >= 0 && (firstArray < firstObject || firstObject < 0) ? firstArray : firstObject;
  if (startsAt < 0) return {};
  return JSON.parse(clean.slice(startsAt));
}

function stringOr(value, fallback) {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

function sanitizeId(value) {
  return String(value)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 80) || crypto.randomUUID();
}

function normalizeIngredients(value, fallbackCsv) {
  if (Array.isArray(value)) {
    return value.map((item) => ({
      name: stringOr(item?.name, String(item || "")),
      quantity: Number(item?.quantity) || null,
      unit: stringOr(item?.unit, ""),
      required: item?.required !== false
    })).filter((item) => item.name);
  }
  return String(fallbackCsv || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .map((name) => ({ name, quantity: null, unit: "", required: true }));
}

function normalizeStepObjects(value, fallbackCsv) {
  if (Array.isArray(value)) {
    return value.map((step) => ({ text: stringOr(step?.text, String(step || "")) })).filter((step) => step.text);
  }
  return String(fallbackCsv || "")
    .split("|")
    .map((step) => step.replace(/^\d+[\).]\s*/, "").trim())
    .filter(Boolean)
    .map((text) => ({ text }));
}

function normalizeRecipeUpdate(update) {
  if (!update || typeof update !== "object") return null;
  const ingredients = normalizeIngredients(update.ingredients, "");
  const steps = normalizeStepObjects(update.steps, "");
  if (ingredients.length === 0 || steps.length === 0) return null;
  return {
    summary: stringOr(update.summary, "Apply the suggested ingredient changes"),
    ingredients,
    steps
  };
}

function decodeBase64Url(value) {
  return new TextDecoder().decode(base64UrlToBytes(value));
}

function base64UrlToBytes(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}
