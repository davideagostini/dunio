# Mobile Chat Input Roadmap

This document describes a practical V1 roadmap for a chat-style input screen in the Android phone
app.

The goal is not to build a general AI assistant on day one. The goal is to create a fast,
mobile-first command surface that can:

- answer simple finance queries
- log entries from typed text
- log entries from voice input
- create draft entries from photos such as receipts

The design intentionally starts with local Android capabilities and deterministic parsing before
introducing online AI services.

## Product goal

The screen should feel like a compact finance chat input, but it should behave more like a guided
command surface than a free-form assistant at first.

Target examples:

- `expenses this month`
- `10 euro coffee`
- `1000 income salary`
- `spent 25 groceries today`
- take a photo of a receipt and receive a draft entry
- speak an expense and receive a draft entry

## Why this roadmap starts locally

For a first version, local device capabilities are enough to validate the user value:

- text input already exists and needs only parsing
- Android provides speech-to-text through `SpeechRecognizer`
- ML Kit provides OCR through text recognition
- the app already has repositories and household-scoped data needed to execute simple commands

This keeps the first version:

- faster to build
- cheaper to operate
- more private
- easier to control and debug

## What ML Kit can and cannot do

ML Kit is a good fit for:

- OCR from images
- receipt text extraction
- turning a captured image into raw text for further parsing

ML Kit is not the main solution for speech input.

Voice input should use:

- Android `SpeechRecognizer`
- `createOnDeviceSpeechRecognizer(...)` when available
- fallback to the default system recognizer if needed

## Scope of V1

V1 should support only a limited set of predictable intents.

Supported V1 use cases:

1. add expense from text
2. add income from text
3. query monthly expense total
4. query monthly income total
5. query simple category totals for the current month
6. create a draft entry from receipt OCR text
7. create a draft entry from voice transcript

Out of scope for V1:

1. fully conversational multi-turn memory
2. free-form natural language across all finance tasks
3. automatic transaction splitting from receipts
4. advanced recurring transaction inference
5. full LLM dependency for all parsing
6. Wear integration changes as part of the first mobile rollout

## High-level architecture

All input types should converge into one parsing pipeline.

```text
typed text
voice transcript
ocr text
   -> normalize input
   -> detect intent
   -> parse payload
   -> produce action draft or query request
   -> confirm when needed
   -> execute through existing repositories
```

The most important architectural rule is:

- do not execute money-changing actions directly from raw input
- always create a draft first when logging an entry

## Future-proofing for a later AI migration

The first version may use local Android capabilities such as ML Kit and deterministic parsing, but
the architecture should not assume those choices are permanent.

The feature should be designed so that:

- the UI does not depend directly on ML Kit-specific models
- the ViewModel does not embed provider-specific OCR logic
- the parser and OCR pipeline can be swapped without rewriting the screen flow
- draft confirmation and repository execution remain stable even if the interpretation engine changes

The desired long-term architecture is:

```text
UI
  -> input adapters
  -> interpretation interfaces
  -> draft / result models
  -> execution layer
```

That way, local-first implementations can be replaced later with online AI-backed implementations
without reworking the full feature.

### Recommended abstraction points

At minimum, isolate these responsibilities behind interfaces:

1. OCR / image extraction
2. command interpretation
3. optional receipt-specific parsing

Example interface ideas:

```kotlin
interface ReceiptInputExtractor {
    suspend fun extract(input: ReceiptInput): ReceiptExtractionResult
}

interface FinanceCommandInterpreter {
    suspend fun interpret(input: UserCommandInput): FinanceCommandResult
}
```

Possible implementations:

- `MlKitReceiptInputExtractor`
- `AiReceiptInputExtractor`
- `LocalFinanceCommandInterpreter`
- `AiFinanceCommandInterpreter`

### What should remain stable if the engine changes later

If the feature is structured correctly, these pieces should remain mostly unchanged during a future
move from ML Kit or local parsing to online AI:

- chat UI
- message list model
- draft confirmation UX
- repository execution layer
- most of the ViewModel orchestration

The parts that should be replaceable are mainly:

- OCR extraction
- natural-language interpretation
- receipt understanding logic

### Why this matters

If ML Kit or the local parser are allowed to leak directly into the screen logic, replacing them
later becomes a reimplementation project.

If they are isolated behind stable interfaces, a future online AI migration becomes a provider swap
instead of a full feature rewrite.

## UI structure

Recommended screen concept:

- a new mobile-only screen such as `FinanceAssistantScreen`

Core UI sections:

1. message list
   - user messages
   - system responses
   - draft cards
   - query result cards

2. composer
   - text field
   - microphone button
   - camera button
   - send button

3. draft confirmation surface
   - type
   - amount
   - category
   - description
   - date
   - confirm / cancel actions

4. result cards for supported queries
   - current month expenses
   - current month income
   - simple category totals

## Dunio + Material 3 UI concept

The visual reference for a chat-like finance surface is valid, but the Dunio version should feel
calmer, more trustworthy, and more product-driven than a generic "AI chatbot" mockup.

The product framing should be:

- fast finance command surface
- guided logging and simple finance queries
- lightweight assistant feel without overpromising intelligence

### Design intent

The Dunio interpretation of this UI should feel:

- clean
- soft
- minimal
- reliable
- mobile-first

It should not feel:

- gimmicky
- overly theatrical
- neon or hype-driven
- like a fake general-purpose AI assistant

### Overall screen structure

Recommended layout:

1. top sheet-style header
2. scrolling message list
3. optional suggestion chips
4. bottom composer with text, camera, and microphone actions

The screen should feel like a focused task surface layered above the app, not like a separate
social messaging product.

### Header

The header should be simple and product-oriented.

Recommended content:

- title:
  - `Quick log`
  - or `Ask Dunio`
- subtitle:
  - `Add entries, ask totals, or scan receipts`
- dismiss action on the right

Avoid:

- loud AI branding
- exaggerated assistant persona
- theatrical request counters unless the business model later requires them

Recommended Material 3 building blocks:

- `TopAppBar` or custom header row
- `Text`
- `IconButton`
- `HorizontalDivider` only if visually needed

### Background and container treatment

The overall surface should use Material 3 surfaces instead of heavy gradients.

Recommended approach:

- page background:
  - `surface`
  - or `surfaceContainerLowest`
- main chat container:
  - `surfaceContainerLow`
  - rounded large corners if presented as a modal or full-screen sheet

Visual goal:

- a finance tool that feels premium and calm
- not a noisy chat demo

### Message list

Messages should be rendered as a vertical conversation stream with strong readability.

Two main bubble types:

1. user message bubble
2. system response bubble

Optional specialized message types:

3. entry draft card
4. query result card
5. OCR import draft card
6. error or unsupported-command card

### User bubble

Recommended characteristics:

- aligned to the right
- filled style using primary or tertiary container tone
- rounded large shape
- concise typography

This gives a clear "I said this" feeling without becoming overly decorative.

Recommended Material 3 building blocks:

- `Surface`
- `Text`

### System response bubble

Recommended characteristics:

- aligned to the left
- neutral surface card
- slightly elevated or tonal
- short copy that confirms what the system understood

Example tone:

- `Got it. €15 coffee, ready to review.`
- `This month: €420 total expenses.`

Avoid overly playful responses like:

- `Cha-ching!`
- `Boom!`
- `Magic done!`

For a finance product, trust beats entertainment.

Recommended Material 3 building blocks:

- `Card`
- `Surface`
- `Text`

### Entry draft card

This is the most important specialized UI element.

Instead of turning every assistant response into a plain bubble, entry parsing should usually end
in a structured draft card.

Recommended content:

- type: expense or income
- amount
- description
- category if recognized
- date if recognized
- payment method if supported later
- actions:
  - `Confirm`
  - `Edit`
  - `Cancel`

Why this matters:

- money actions should not execute from raw chat text alone
- the user must be able to review the interpretation before saving

Recommended Material 3 building blocks:

- `ElevatedCard` or `Card`
- `ListItem`
- `FilledTonalButton`
- `OutlinedButton`
- `TextButton`

### Query result card

For simple questions like:

- `expenses this month`
- `income this month`
- `groceries this month`

the response should be more structured than a generic chat reply.

Recommended content:

- title
- main value
- optional context label
- optional follow-up chip suggestions

Example:

- `Expenses this month`
- `€1,245`
- `Top category: Groceries`

Recommended Material 3 building blocks:

- `Card`
- `Text`
- optional `AssistChip`

### Suggestion chips

The reference UI uses suggestion pills near the bottom. This is a very good fit for Dunio.

These chips help users understand the supported command language without reading documentation.

Recommended examples:

- `10€ coffee`
- `1000 income salary`
- `expenses this month`
- `scan receipt`
- `groceries this month`

Recommended Material 3 building blocks:

- `SuggestionChip`
- `AssistChip`

### Composer

The composer should be the primary action zone and stay extremely simple.

Recommended structure:

1. text field
2. camera action
3. microphone action
4. send action

Recommended behavior:

- text field expands only modestly
- camera button starts OCR import flow
- microphone button starts speech input flow
- send button submits text command

Recommended Material 3 building blocks:

- `TextField` or `OutlinedTextField`
- `IconButton`
- optional `FloatingActionButton` only if it truly helps the hierarchy

### Motion and feedback

Motion should stay minimal.

Recommended:

- smooth insertion of new messages
- subtle loading indicators
- no dramatic assistant animations

Good feedback states:

- listening
- processing
- draft ready
- saved
- queued
- unsupported

### Empty state

The initial state should teach the feature quickly.

Recommended first-screen content:

- title
- one-line explanation
- 3 to 5 suggestion chips

Example:

- `Add entries, ask monthly totals, or scan a receipt.`

This helps keep the feature approachable and not "blank chat intimidating."

### Material 3 component mapping

Suggested Compose building blocks:

- `Scaffold`
- `TopAppBar`
- `LazyColumn`
- `Card`
- `ElevatedCard`
- `Surface`
- `TextField`
- `IconButton`
- `SuggestionChip`
- `AssistChip`
- `FilledTonalButton`
- `OutlinedButton`
- `TextButton`
- `CircularProgressIndicator`

### Product recommendation

The UI should look like:

- a focused Dunio command surface
- not a clone of a consumer AI chat app

That means:

- keep the chat metaphor
- keep the speed and suggestions
- keep camera and mic in the composer
- but render drafts and finance results in a more structured Material 3 way

### Summary

The reference is directionally strong for:

- mobile usability
- fast input
- low-friction onboarding

The Dunio version should translate it into:

- calmer visuals
- stronger trust cues
- structured draft cards
- Material 3 surfaces and chips
- confirmation-first finance actions

## Intent model

The first version should keep the intent model explicit and small.

Example shape:

```kotlin
sealed interface ChatIntent {
    data class AddEntryDraft(...)
    data class QueryMonthlyExpenses(...)
    data class QueryMonthlyIncome(...)
    data class QueryCategoryExpenses(...)
    data class ImportReceiptDraft(...)
    data class Unknown(...)
}
```

Expected V1 user input examples:

### Add entry

- `10 coffee`
- `10€ coffee`
- `expense 24 taxi`
- `income 1000 salary`

### Query

- `expenses this month`
- `income this month`
- `groceries this month`

### OCR import

- user takes or selects a receipt photo
- OCR extracts raw text
- the parser turns it into a draft

### Voice

- user speaks an entry
- speech recognizer produces text
- the text flows through the same parser as typed text

## Parsing strategy

V1 should use a deterministic local parser.

Why:

- predictable behavior
- easier testing
- lower complexity
- no network dependency for the basic feature

### Parsing stages

1. normalize text
   - lowercase
   - normalize currency symbols
   - trim extra whitespace
   - normalize decimal separators

2. detect intent
   - amount + short description -> likely entry draft
   - keywords like `income`, `expense`, `spent`
   - query phrases like `this month`, `expenses`, `income`

3. extract entities
   - amount
   - type
   - date phrases such as `today` or `yesterday`
   - description tail
   - category candidates

4. resolve category
   - exact match
   - normalized name match
   - fallback to uncategorized draft or explicit user choice

## Voice input plan

Voice should be treated as another text source, not a separate finance engine.

Recommended flow:

1. user taps microphone
2. app records with `SpeechRecognizer`
3. transcript is returned
4. transcript is displayed as user input
5. normal command parsing runs
6. app produces either:
   - a draft entry
   - a query result
   - an unsupported-command response

Important constraints:

- use on-device recognition when available
- fall back gracefully when on-device speech is unavailable
- always allow the user to review before committing an entry

## OCR import plan

OCR should be positioned as draft generation, not perfect automation.

Recommended flow:

1. user taps camera
2. app captures or selects an image
3. ML Kit extracts text
4. `ReceiptTextParser` tries to identify:
   - amount
   - merchant
   - date
5. app shows a draft entry for review
6. user confirms or edits manually

V1 should not promise:

- accurate line-item splitting
- perfect receipt classification
- full accounting import

The product value is:

- reduce typing
- create a useful draft quickly

## Execution layer

Execution should stay close to the existing repositories.

For add-entry intents:

1. parser produces `EntryDraft`
2. UI shows draft card
3. user confirms
4. app calls `EntryRepository.insert(...)`

For query intents:

1. parser identifies supported query
2. view model calls the existing repositories or cached screen data sources
3. app returns a compact result card

## Suggested package structure

Recommended files:

- `app/src/main/java/com/davideagostini/summ/ui/chat/FinanceAssistantScreen.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/FinanceAssistantViewModel.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/model/ChatMessage.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/model/ChatIntent.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/model/EntryDraft.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/parser/FinanceCommandParser.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/parser/ReceiptTextParser.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/voice/VoiceInputController.kt`
- `app/src/main/java/com/davideagostini/summ/ui/chat/ocr/ReceiptOcrProcessor.kt`

## ViewModel responsibilities

`FinanceAssistantViewModel` should:

1. own chat session state
2. accept typed text submissions
3. accept voice transcripts
4. accept OCR text results
5. call the parser
6. publish draft/result messages
7. handle draft confirmation and cancellation
8. execute supported repository actions

Suggested API surface:

```kotlin
fun onTextSubmitted(text: String)
fun onVoiceTranscriptReceived(text: String)
fun onReceiptTextExtracted(text: String)
fun onConfirmDraft()
fun onCancelDraft()
```

## Message model

Suggested message model:

```kotlin
sealed interface ChatMessage {
    data class UserText(val text: String) : ChatMessage
    data class SystemText(val text: String) : ChatMessage
    data class EntryDraftMessage(val draft: EntryDraft) : ChatMessage
    data class QueryResultMessage(val title: String, val value: String) : ChatMessage
    data class ErrorMessage(val text: String) : ChatMessage
}
```

## Delivery phases

### Phase 1: text-only command surface

Goal:

- prove that the chat-style interaction adds value before introducing media input

Deliverables:

1. chat screen UI
2. local parser for typed text
3. support for:
   - add expense
   - add income
   - monthly totals
4. draft confirmation before saving

### Phase 2: voice input

Goal:

- let the user dictate quick entries and simple commands

Deliverables:

1. `SpeechRecognizer` integration
2. transcript-to-parser flow
3. microphone states and permission handling

### Phase 3: OCR import

Goal:

- reduce manual typing for receipts and photographed text

Deliverables:

1. image picker or camera flow
2. ML Kit OCR integration
3. receipt text parser
4. draft confirmation from OCR result

### Phase 4: optional online AI support

Goal:

- improve handling of ambiguous or more conversational requests

Only add this phase if V1 proves value and the local parser becomes the main limitation.

Potential use cases for online AI later:

1. more natural phrasing
2. ambiguous command recovery
3. better receipt interpretation
4. richer finance question handling

## Decision criteria for adding online AI later

Do not introduce an online AI service just because it is possible.

Add it only if one or more of these become true:

1. the local parser produces too many `Unknown` results
2. users naturally ask more conversational questions than the deterministic parser can support
3. OCR drafts need stronger semantic cleanup
4. product goals shift toward a true assistant instead of a fast command surface

## Risks

Main risks in this feature area:

1. user trust if money-changing commands execute without confirmation
2. ambiguous parsing of amounts, dates, and categories
3. speech recognition variability across devices and languages
4. OCR inaccuracies on poor-quality receipts
5. scope creep toward a general AI assistant too early

## Recommendation

The recommended starting point is:

1. text command V1
2. voice second
3. OCR third
4. online AI only if validated by real usage

That keeps the feature aligned with Dunio's product values:

- fast
- simple
- mobile-first
- easy to understand
- optimized for daily use
