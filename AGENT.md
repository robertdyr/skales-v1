# AGENT.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

## Project-Specific Context: Skales

### Documentation First

**All architectural and product documentation is in `/docs`.** Before making changes:

1. Check `docs/overview.md` for product vision and current build focus
2. Check `docs/architecture/` for component contracts and boundaries
3. Check `docs/roadmap.md` for development priorities
4. Check `docs/ui-ux/` for screen design patterns

**The docs are the source of truth.** If code and docs conflict, ask which should change.

### Architecture Rules

**Component boundaries matter.** Components are testable libraries with narrow APIs:

- `analyzer` - audio → note evidence (future phase)
- `infer` - seed/evidence → draft generation
- `editor` - scale authoring and correction operations
- `player` - scale playback engine
- `storage` - Room persistence
- `app-shell` - screens, ViewModels, navigation, orchestration

**Dependency direction:** `app-shell` depends on components, not the other way around.

**State ownership:**
- Screen/UI state lives in `ViewModel`s
- Components should be stateless or have minimal engine state
- Session state (playback cursor, editing state) lives in `ViewModel`s, not components

See `docs/architecture/overview.md` for full dependency rules.

### Domain Model Constraints

See `docs/architecture/models.md` for full model definitions.

**Critical rules:**
- Domain models (`Scale`, `ScaleSet`, `ScaleSound`) do not contain persistence metadata
- Persistence metadata (`createdAt`, `updatedAt`) lives only in `ScaleEntity`
- Do not reintroduce the old flat `notes + setStarts + cue` shape
- All timing is beat-based with explicit BPM
- Cue sounds are regular sounds with `kind = Cue` (no special playback rules)

### Terminology Matters

**Use these terms consistently:**

- `infer` - draft generation from evidence or partial sets (not "complete", "generate", or "fill")
- `analyzer` - audio to evidence extraction (not "detector" or "recognizer")
- `evidence` - structured note/phrase data from audio (not "results" or "detection")
- `draft` - inferred scale before user correction (not "suggestion" or "proposal")
- `sets` - groupings of sounds (not "phrases" or "measures")

**These are separate concerns:**
- `analyzer` produces evidence from audio
- `infer` produces drafts from evidence or partial sets
- They are not the same component

### Current Build Focus

**Editor-first creation is the MVP priority.** Recording analysis comes later.

Current priority order:
1. Editor-first scale authoring
2. Partial-set inference and reinference
3. Piano-roll editing with configurable snap
4. Smooth save and playback handoff
5. Later: recording analysis and microphone capture

**When proposing features, ask:**
1. Does it make editor-first creation easier?
2. Does it improve infer quality or reinference usability?
3. Is it recording-analysis work that can wait?

See `docs/roadmap.md` for phased priorities.

### Code Style

**Match existing patterns:**
- MVVM for screens (View = Compose, ViewModel = state + intents, Model = domain + components)
- Kotlin coroutines + StateFlow for async
- Jetpack Compose + Material3 for UI
- Navigation Compose for routing
- Room for persistence with type converters for nested models

**Prefer:**
- Readable state over clever implicit scheduling
- Explicit operations over hidden side effects
- Pure functions in components where possible
- Small, focused changes

### Common Mistakes to Avoid

❌ Don't put persistence logic in components (only `storage` persists)  
❌ Don't make `player` depend on `analyzer` models  
❌ Don't store screen state inside components  
❌ Don't add recording/microphone features yet (future phase)  
❌ Don't use old model terminology or flat scale shapes  
❌ Don't skip the docs when uncertain

✅ Do consult docs before making changes  
✅ Do keep components as testable libraries  
✅ Do put screen state in ViewModels  
✅ Do follow the current roadmap priorities  
✅ Do use correct terminology consistently  
✅ Do ask when architecture boundaries are unclear


