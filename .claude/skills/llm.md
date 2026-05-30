---
name: llm
description: YoPt LLM integration — LLMService, ApiStyle request/response shapes, adding providers, model fetching, error handling
---

# YoPt LLM Integration

For layering rules (port vs service, where things live) see `.claude/skills/architecture.md`.
For exact signatures and the current provider list, read the source — this skill documents
the **per-ApiStyle wire formats and the rules for extending them**, which is the part that
isn't obvious from reading any single file.

## Where the code lives

| Concern | Type | Location |
|---|---|---|
| Request dispatch by `ApiStyle`, response parsing, model fetching | domain service `LLMService` | `domain/services/LLMService.kt` |
| HTTP transport (POST/GET) | port `HttpGateway` + adapter `KtorHttpAdapter` | `domain/ports/`, `adapters/` |
| Provider definitions | `ProviderDef.predefined` | `domain/models/` |
| Model persistence | domain service `ModelService` | `domain/services/` |

`LLMService` has **no Ktor imports** — it builds JSON and calls `HttpGateway`. There are no
per-provider clients; a single service dispatches all styles via `when (provider.apiStyle)`.

## Flow

```
SendPromptUseCase
  → LLMService.send(prompt, model, systemInstructions, apiKey)
      → resolve provider from ModelService.getProviders()
      → when (provider.apiStyle) → openAI / anthropic / gemini
        → HttpGateway.post(...)

RefreshModelsUseCase
  → LLMService.fetchModels(provider, apiKey)
      → when (provider.apiStyle) → fetch via HttpGateway.get(...), or static list
  → ModelService.upsertModels(providerId, models)
```

## ApiStyle wire formats

`enum class ApiStyle { OPENAI, ANTHROPIC, GEMINI }` — each has its own request body, auth
header, and response shape inside `LLMService`.

### OPENAI
- **Send:** `POST {baseUrl}/v1/chat/completions`, header `Authorization: Bearer {key}`, body `{model, messages: [{role, content}]}` (system instructions become a `system`-role message).
- **Response:** `choices[0].message.content`. Note `content` may be a string **or** an array of `{text}` blocks — handle both.
- **Fetch models:** `GET {baseUrl}/v1/models` → `data[].id`.

### ANTHROPIC
- **Send:** `POST {baseUrl}/v1/messages`, headers `x-api-key: {key}` + `anthropic-version: 2023-06-01`, body `{model, max_tokens: 4096, system?, messages: [{role, content}]}`.
- **Response:** join the `text` of every block in `content[]`.
- **Fetch models:** static hardcoded list (no Anthropic listing endpoint).

### GEMINI
- **Send:** `POST {baseUrl}/v1beta/models/{modelId}:generateContent?key={key}` (key in URL, no auth header), body `{system_instruction?, contents: [{role, parts: [{text}]}]}`.
- **Response:** join the `text` of every part in `candidates[0].content.parts[]`.
- **Fetch models:** `GET {baseUrl}/v1beta/models?key={key}` → `models[]`; id = `name` with `models/` prefix stripped, display name = `displayName`.

## Adding a provider

1. Add a `ProviderDef` to `ProviderDef.predefined` (id, display name, `ApiStyle`, auth, baseUrl).
2. Add its "Get API key" URL in the `ApiKeyAuth` composable (`SettingsDialog.kt`).
3. If it needs a brand-new `ApiStyle`, see below.
4. No other wiring — `AppModule`, model selector, and settings all work off `List<ProviderDef>` dynamically.

Providers that lack a model-listing endpoint return a **static list** keyed off their id
inside `LLMService.fetchModels` (Anthropic does this; Hugging Face is wired the same way but
not currently in `predefined`).

## Adding an ApiStyle

1. Add the enum value to `ApiStyle`.
2. In `LLMService`: add a branch in `send()` (a private method building that API's request/response) and, if the provider lists models, a branch in `fetchModels()`.

## Auth

API-key only. `AuthCredentials` carries `apiKey`; there are no OAuth/access tokens. The key is
passed straight through to `LLMService.send`. `send` throws if the key is null/blank.

## Error handling

- **Transport:** `KtorHttpAdapter` catches Ktor `ResponseException` and appends the response body (first 300 chars) to the thrown message.
- **API-level:** each style also inspects the parsed JSON for an `error` object and throws its `message`.
- **Provider context:** `LLMService.send` wraps any failure as `"{providerName}: {message}"`.

## Model enabled state

Fetched models are upserted into `ModelService` (persisted as JSON in `KeyValueStore`).
Enabled/disabled lives on `ModelDef.enabled`, toggled via `ModelService.setModelEnabled()` —
there is no separate disabled-models store.
