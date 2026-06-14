const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

admin.initializeApp();

const openRouterApiKey = defineSecret("OPENROUTER_API_KEY");
const DEFAULT_MODEL = process.env.OPENROUTER_MODEL || "openrouter/free";

function requireAuth(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in before using ShelfLife AI.");
  }
}

async function callOpenRouter(messages, responseFormat) {
  const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${openRouterApiKey.value()}`,
      "Content-Type": "application/json",
      "HTTP-Referer": "https://github.com/potatsukki/ShelfLife",
      "X-Title": "ShelfLife"
    },
    body: JSON.stringify({
      model: DEFAULT_MODEL,
      messages,
      temperature: 0.4,
      ...(responseFormat ? { response_format: responseFormat } : {})
    })
  });

  if (response.status === 429) {
    throw new HttpsError("resource-exhausted", "AI rate limit reached. Try again later.");
  }
  if (!response.ok) {
    throw new HttpsError("unavailable", `AI provider failed with status ${response.status}.`);
  }

  const json = await response.json();
  return json?.choices?.[0]?.message?.content || "";
}

exports.generateRecipes = onCall({ secrets: [openRouterApiKey] }, async (request) => {
  requireAuth(request);
  const ingredients = Array.isArray(request.data?.ingredients) ? request.data.ingredients : [];
  if (ingredients.length === 0) {
    throw new HttpsError("invalid-argument", "Add pantry ingredients before generating recipes.");
  }

  const pantry = ingredients.map((item) =>
    `- ${item.name} (${item.quantity} ${item.unit}), ${item.category}, ${item.location}, expires ${item.expirationDate}`
  ).join("\n");

  const content = await callOpenRouter([
    {
      role: "system",
      content: "Return only valid JSON. Do not include markdown. Create practical recipes from the user's actual pantry."
    },
    {
      role: "user",
      content: `Pantry:\n${pantry}\n\nReturn a JSON array of 2 recipes. Each item must include id, name, prepTime, difficulty, imageResUrl, whySuggested, ingredientsCsv, stepsCsv. Use imageResUrl as an empty string unless you are certain the URL is a stable food image.`
    }
  ], { type: "json_object" });

  const parsed = JSON.parse(content);
  return Array.isArray(parsed) ? parsed : parsed.recipes || [];
});

exports.chatAssistant = onCall({ secrets: [openRouterApiKey] }, async (request) => {
  requireAuth(request);
  const message = String(request.data?.message || "").trim();
  if (!message) {
    throw new HttpsError("invalid-argument", "Message is required.");
  }
  const history = Array.isArray(request.data?.history) ? request.data.history.slice(-12) : [];
  const reply = await callOpenRouter([
    {
      role: "system",
      content: "You are ShelfLife Kitchen AI. Help with pantry planning, food safety, substitutions, and reducing waste. Keep answers concise and practical."
    },
    ...history,
    { role: "user", content: message }
  ]);
  return { reply };
});

exports.identifyBarcodeFallback = onCall({ secrets: [openRouterApiKey] }, async (request) => {
  requireAuth(request);
  const barcode = String(request.data?.barcode || "").trim();
  if (!barcode) {
    throw new HttpsError("invalid-argument", "Barcode is required.");
  }
  const content = await callOpenRouter([
    {
      role: "system",
      content: "Return only a JSON object. If uncertain, return an empty name."
    },
    {
      role: "user",
      content: `Identify this grocery barcode if possible: ${barcode}. Return name, category, quantity, unit, notes.`
    }
  ], { type: "json_object" });
  return JSON.parse(content);
});
