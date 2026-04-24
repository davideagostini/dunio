# Android Compose Modernization Checklist

## High-priority improvements

Prioritize these first:

- `collectAsState()` where `collectAsStateWithLifecycle()` should be used
- Business logic embedded in composables
- Incorrect or leaking `LaunchedEffect` or listener usage
- Missing stable keys in lazy lists
- Repeated recomposition work in hot paths
- Deprecated APIs with straightforward supported replacements
- Mutable state exposed directly instead of via immutable flow types

---

## Medium-priority improvements

- Weak composable naming or unclear stateful/stateless split
- Overuse of `CompositionLocal` where explicit parameters would be clearer
- Modifier ordering bugs or custom modifier complexity
- Duplicate state that can be derived on read
- Local cleanup that reduces indirection without changing behavior

---

## Usually leave alone unless requested

- Full feature or navigation rewrites
- Package structure redesign across the app
- Swapping architecture patterns wholesale
- Broad dependency changes not required by the task
- Subjective “style-only” cleanup with little practical value

---

## Safe fix boundary

Apply fixes automatically only when they are:

- High confidence
- Behavior-preserving
- Small enough to validate quickly
- Consistent with local patterns
