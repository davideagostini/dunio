# Italian SEO Rollout Plan

## Goal

Launch an Italian SEO section for `dunio.app` that is coherent with the existing Italian Play Store listing without translating the entire English site at once.

The main objective is to create a small but complete Italian content cluster:

- one Italian homepage
- one Italian resources hub
- three strong Italian SEO landing pages

This keeps the experience consistent for Italian users and avoids a partial localization that sends users from Italian pages into English-only journeys too early.

## Why Italian Now

Italian is the most practical first localization because:

- the Play Store listing is already localized in Italian
- the core product messaging is already defined in Italian
- content quality and maintenance are more realistic than with other languages
- the topic has a trust component, so native-language pages can improve conversion

## Current English Content

Current non-legal English pages in `mobile-app/site`:

- `/`
- `/resources/`
- `/shared-finance-app-for-couples/`
- `/couples-budget-app/`
- `/household-budget-app/`
- `/household-expense-tracker/`
- `/net-worth-tracker-for-couples/`
- `/recurring-payments-tracker/`
- `/how-to-manage-money-as-a-couple/`

These pages already form a small SEO cluster. The Italian version should mirror the structure only where it adds real value.

## Italian Keyword Model

### Core keywords

- `finanze di coppia`
- `spese condivise`
- `gestione spese di coppia`

### Secondary keywords

- `app finanze coppia`
- `budget di coppia`
- `conto condiviso coppia`
- `spese domestiche`

## Recommended Rollout

### Phase 1: launch first

Create these Italian pages first:

- `/it/`
- `/it/resources/`
- `/it/finanze-di-coppia/`
- `/it/spese-condivise/`
- `/it/gestione-spese-di-coppia/`

This is the minimum viable Italian cluster.

### Phase 2: add only if phase 1 gains traction

Potential expansion pages:

- `/it/budget-di-coppia/`
- `/it/patrimonio-di-coppia/` or `/it/net-worth-di-coppia/`
- `/it/pagamenti-ricorrenti/`
- `/it/come-gestire-i-soldi-in-coppia/`

Phase 2 should come only after there is evidence of impressions or clicks for Italian non-branded queries.

## EN to IT Mapping

This is the recommended mapping by search intent, not a literal page-by-page translation requirement.

| Current English page | Role today | Recommended Italian equivalent | Priority | Notes |
| --- | --- | --- | --- | --- |
| `/` | Main brand and conversion page | `/it/` | High | Must exist at launch |
| `/resources/` | Internal hub | `/it/resources/` | High | Must exist if homepage is localized |
| `/shared-finance-app-for-couples/` | Broad shared-finance positioning | `/it/finanze-di-coppia/` | High | Best semantic match |
| `/household-expense-tracker/` | Practical shared spending use case | `/it/spese-condivise/` | High | Stronger and more natural in Italian |
| `/couples-budget-app/` | Budgeting angle | `/it/gestione-spese-di-coppia/` | High | Better fit than a literal translation |
| `/household-budget-app/` | Household budgeting angle | Optional later: `/it/budget-di-coppia/` or support section within other pages | Low | Not needed in phase 1 |
| `/net-worth-tracker-for-couples/` | Net worth angle | Optional later | Low | Translate only if product/store messaging expands here |
| `/recurring-payments-tracker/` | Recurring payments angle | Optional later | Low | Good supporting topic, not first-wave |
| `/how-to-manage-money-as-a-couple/` | Educational guide | Optional later | Medium | Good future editorial page |

## Pages That Should Not Be Primary Targets Yet

These terms are useful inside body copy, but should not be standalone pages in phase 1:

- `conto condiviso coppia`
- `spese domestiche`

Why:

- `conto condiviso coppia` may attract banking or joint-account intent rather than a finance tracker intent
- `spese domestiche` is broader and less clearly aligned with Dunio's strongest positioning

Use them as supporting terms inside the Italian homepage and landing pages instead.

## Suggested Page Focus

### `/it/`

Primary focus:

- `finanze di coppia`
- `spese condivise`

Secondary support:

- `app finanze coppia`
- `budget di coppia`

This page should mirror the current homepage role:

- explain the shared household concept
- clarify that Dunio is simple and mobile-first
- drive traffic to Play Store and deeper Italian pages

### `/it/finanze-di-coppia/`

Primary focus:

- `finanze di coppia`

Secondary support:

- `app finanze coppia`
- `conto condiviso coppia`

This is the broadest Italian landing page and should be the Italian equivalent of the current high-level shared-finance positioning.

### `/it/spese-condivise/`

Primary focus:

- `spese condivise`

Secondary support:

- `spese domestiche`
- `gestione spese coppia`

This page should be practical and use-case driven, centered on day-to-day shared household tracking.

### `/it/gestione-spese-di-coppia/`

Primary focus:

- `gestione spese di coppia`

Secondary support:

- `budget di coppia`
- `app finanze coppia`

This page should be more search-intent driven and closer to the current store copy around shared daily money management.

### `/it/resources/`

This page should serve as the Italian hub and link only to the Italian pages that actually exist.

Do not include links from the Italian hub to English SEO pages unless there is no Italian equivalent and the UX copy clearly signals that the destination is in English.

## URL Structure

Recommended structure:

- English remains on root URLs
- Italian uses `/it/` subdirectories

Examples:

- `/`
- `/resources/`
- `/shared-finance-app-for-couples/`
- `/it/`
- `/it/resources/`
- `/it/finanze-di-coppia/`
- `/it/spese-condivise/`
- `/it/gestione-spese-di-coppia/`

This is the simplest structure to maintain on the current static site.

## hreflang Recommendation

Use separate URLs for English and Italian pages and connect them with `hreflang`.

For each translated page pair:

- English page references itself with `hreflang="en"`
- Italian page references itself with `hreflang="it"`
- both pages reference each other
- homepage can include `x-default`

Important:

- do not rely only on `<html lang="...">`
- do not auto-redirect users based on browser language
- keep manual language switching visible

## Sitemap Recommendation

If the Italian pages launch, the sitemap should be updated to include:

- the Italian URLs
- localized alternates if you decide to manage `hreflang` through sitemap entries

If HTML `hreflang` is easier, use that and still add all Italian URLs to the sitemap normally.

## Internal Linking Rules

When the Italian section launches:

- Italian homepage should link to Italian resources and Italian landing pages
- Italian resources hub should link only to live Italian SEO pages
- Italian landing pages should cross-link to each other
- the language switcher should point to the matching page when a translated equivalent exists

Avoid this pattern:

- Italian homepage -> English resources hub -> English landing pages

That would weaken both UX and language consistency.

## Recommended Launch Scope

### Must launch together

- `/it/`
- `/it/resources/`
- `/it/finanze-di-coppia/`
- `/it/spese-condivise/`
- `/it/gestione-spese-di-coppia/`

### Can remain English-only for now

- legal pages
- `net-worth-tracker-for-couples`
- `recurring-payments-tracker`
- `how-to-manage-money-as-a-couple`
- `household-budget-app`

Legal pages can stay in English temporarily if needed, but if Italian becomes a meaningful market, localizing privacy and support pages should be considered.

## Next Implementation Step

If proceeding with the Italian rollout, the next concrete task should be:

1. create the Italian URL tree
2. define page titles and meta descriptions
3. add `hreflang` pairs for translated pages
4. update sitemap and internal navigation
5. publish only the five Italian pages in phase 1

## Decision Summary

Recommended decision:

- yes to an Italian site section
- no to translating every English page immediately
- yes to translating the core cluster around homepage and resources
- yes to intent-based Italian pages rather than literal English keyword copies
