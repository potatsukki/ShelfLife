const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

admin.initializeApp();

const openRouterApiKey = defineSecret("OPENROUTER_API_KEY");
const pexelsApiKey = defineSecret("PEXELS_API_KEY");
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
      temperature: 0.35,
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

function parseJsonObject(content) {
  try {
    return JSON.parse(content);
  } catch (error) {
    const start = content.indexOf("{");
    const end = content.lastIndexOf("}");
    if (start >= 0 && end > start) {
      return JSON.parse(content.slice(start, end + 1));
    }
    throw new HttpsError("internal", "AI returned unreadable JSON.");
  }
}

async function findRecipeImage(recipeName) {
  const key = pexelsApiKey.value();
  if (!key) return {};

  const query = encodeURIComponent(`${recipeName} food`);
  const response = await fetch(
    `https://api.pexels.com/v1/search?query=${query}&per_page=1&orientation=landscape`,
    { headers: { Authorization: key } }
  );

  if (!response.ok) return {};
  const json = await response.json();
  const photo = json?.photos?.[0];
  if (!photo?.src) return {};

  return {
    imageUrl: photo.src.large || photo.src.landscape || photo.src.medium || "",
    imageProvider: "Pexels",
    photographerName: photo.photographer || "",
    photographerUrl: photo.photographer_url || "",
    photoPageUrl: photo.url || ""
  };
}

function normalizeRecipe(recipe, index) {
  const safeName = String(recipe.name || `Recipe ${index + 1}`).trim();
  const ingredients = Array.isArray(recipe.ingredients) ? recipe.ingredients : [];
  const steps = Array.isArray(recipe.steps) ? recipe.steps : [];
  return {
    id: String(recipe.id || `ai_${Date.now()}_${index}`),
    name: safeName,
    prepTime: String(recipe.prepTime || "25 min"),
    difficulty: String(recipe.difficulty || "Medium"),
    whySuggested: String(recipe.whySuggested || "Based on your pantry."),
    ingredients: ingredients.map((item) => ({
      name: String(item.name || "").trim(),
      quantity: Number(item.quantity) || null,
      unit: String(item.unit || "").trim(),
      required: item.required !== false
    })).filter((item) => item.name),
    steps: steps.map((step) => ({
      text: typeof step === "string" ? step.trim() : String(step.text || "").trim()
    })).filter((step) => step.text),
    imageUrl: "",
    imageProvider: "",
    photographerName: "",
    photographerUrl: "",
    photoPageUrl: ""
  };
}

exports.generateRecipes = onCall({ secrets: [openRouterApiKey, pexelsApiKey] }, async (request) => {
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
      content: "Return only valid JSON. Create practical recipes from the user's actual pantry. Do not invent pantry ownership; list all recipe ingredients plainly."
    },
    {
      role: "user",
      content: `Pantry:\n${pantry}\n\nReturn this JSON shape: {"recipes":[{"id":"short-slug","name":"Recipe name","prepTime":"25 min","difficulty":"Easy|Medium|Hard","whySuggested":"why this matches the pantry","ingredients":[{"name":"ingredient","quantity":1,"unit":"cup","required":true}],"steps":[{"text":"step text"}]}]}. Return 2 recipes.`
    }
  ], { type: "json_object" });

  const parsed = parseJsonObject(content);
  const recipes = (Array.isArray(parsed) ? parsed : parsed.recipes || [])
    .map(normalizeRecipe);

  return {
    recipes: await Promise.all(recipes.map(async (recipe) => ({
      ...recipe,
      ...(await findRecipeImage(recipe.name))
    })))
  };
});

exports.chatAssistant = onCall({ secrets: [openRouterApiKey] }, async (request) => {
  requireAuth(request);
  const message = String(request.data?.message || "").trim();
  if (!message) {
    throw new HttpsError("invalid-argument", "Message is required.");
  }
  const history = Array.isArray(request.data?.history) ? request.data.history.slice(-12) : [];
  const context = request.data?.recipeContext;
  const pantry = Array.isArray(request.data?.pantry) ? request.data.pantry : [];

  const contextText = context ? `
Current recipe: ${context.recipeName}
Available ingredients: ${(context.availableIngredients || []).join(", ") || "None listed"}
Missing ingredients: ${(context.missingIngredients || []).join(", ") || "None listed"}
Steps:
${(context.steps || []).map((step, index) => `${index + 1}. ${step}`).join("\n")}
` : "";

  const pantryText = pantry.map((item) => `- ${item.name} (${item.quantity} ${item.unit})`).join("\n");
  const reply = await callOpenRouter([
    {
      role: "system",
      content: `You are ShelfLife Kitchen AI. Be concise and practical. If recipe context is provided, answer specifically about that recipe. Help with substitutions, budget swaps, vegetarian changes, ingredient removal, food safety, and pantry planning.\n${contextText}\nPantry:\n${pantryText}`
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
      content: `Identify this grocery barcode if possible: ${barcode}. Return name, category, quantity, unit, location, notes.`
    }
  ], { type: "json_object" });
  return parseJsonObject(content);
});
