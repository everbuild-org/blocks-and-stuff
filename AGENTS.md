# Project Architecture and Agent Guidelines

## Project Architecture

`blocksandstuff` is a Kotlin/JVM Gradle project that provides reusable block and fluid implementations for Minestom.
The root project only coordinates shared Gradle setup, repositories, versioning, and module inclusion. Production code
lives in the library subprojects.

### Modules

- `blocksandstuff-common`: shared support code used by the public block and fluid modules. This includes instance
  options, dropped item helpers, registry/tag loading, and utility functions.
- `blocksandstuff-blocks`: vanilla-like block placement, block behavior, random ticking, block events, and block group
  registration helpers.
- `blocksandstuff-fluids`: fluid placement, replacement events, pickup handling, fluid implementations, and Minestom
  fluid integration.
- `testserver`: local executable Minestom test harness for manual verification. Do not add production APIs here.

### Source Layout

- Kotlin source lives under `src/main/kotlin`.
- Tests live under `src/test/kotlin` and use JUnit 5.
- Runtime resources live under `src/main/resources`.
- Generated build output under `build/`, `.gradle/`, and `.kotlin/` is not source and should not be edited.

### Build And Verification

- Use the Gradle wrapper from the repository root.
- Prefer `./gradlew test` for broad verification.
- For scoped changes, run the narrowest relevant task first, for example:
  - `./gradlew :blocksandstuff-blocks:test`
  - `./gradlew :blocksandstuff-common:test`
  - `./gradlew :blocksandstuff-fluids:test`
- Run `./gradlew build` before larger pull requests when practical.
- The project uses Kotlin JVM toolchains. Do not lower language, JVM, or dependency versions without a specific reason.

### Dependency Boundaries

- `blocksandstuff-common` must stay independent of the block and fluid modules.
- `blocksandstuff-blocks` and `blocksandstuff-fluids` may depend on `blocksandstuff-common`.
- Production library modules should not depend on `testserver`.
- Keep Minestom-specific behavior explicit at module boundaries. Avoid hiding world, instance, or block state mutations
  behind broad helper layers.

### Implementation Expectations

- Preserve existing package ownership. Place block placement rules in `blocks/placement`, block interactions in
  `blocks/behavior`, block events in `blocks/event`, fluid behavior in `fluids`, and shared helpers in `common`.
- Model vanilla behavior directly and locally unless repeated behavior already has a stable abstraction in the module.
- Prefer small, focused rule classes over broad condition tables when behavior is block-family specific.
- Validate external or registry-derived input before using it as trusted state.
- Keep event firing and state mutation order easy to audit. If behavior can be cancelled, make the cancellation path
  obvious.
- Add or update focused tests for placement/state logic whenever a rule changes behavior.

### Documentation And Issue Hygiene

- Public APIs should have useful documentation when their behavior is not obvious from the signature.
- Do not add comments that restate the code.
- Keep README badges and generated support lists consistent with the related scripts and workflow files.
- Issue templates should ask for Minestom version, library version, reproduction steps, expected behavior, and AI usage
  disclosure when applicable.

---

## Agent Guidelines

**ALL AGENTS MUST FOLLOW THESE RULES FOR CODE TO BE APPROVED**

### 🔴 0. Marker for Unreviewed Code (ALWAYS DO THIS FIRST)

After generating code, agents **MUST** mark all unreviewed sections with the following marker comments:
`<@AI_UNREVIEWED>` and `</<@AI_UNREVIEWED>`
Use these as XML-style tags enclosing the unreviewed code.
Always put these markers in a line comment around the corresponding code section.

Once a human has reviewed the code, they should remove this marker.

**If you skip this step, your code will not be approved.**

### 1. Code Contribution Review Rule

All AI-generated code contributions **must** be explicitly and thoroughly reviewed by a human user before being merged
or considered final.

### 3. Context and Planning Requirement

AIs are strictly prohibited from working on the codebase if:

- They do not have sufficient context regarding the module or function they are modifying.
- They don't have a clear plan how changes should be implemented, be it using some planning mode with confirmation or
  ahead-of-time planning.

### 4. Style and Consistency

Follow the styleguide outlined in [CONTRIBUTING.md](./CONTRIBUTING.md)

General style guidelines:

#### 1. Complexity is the enemy

Optimize for low cognitive load. A reader should understand what changes, where to change it, and what could break.
Avoid "just in case" design.

#### 2. Avoid premature abstraction

Prefer direct, flat code inside a module first.
Extract abstractions only when repeated shape, real complexity, or a stable domain concept appears.

#### 3. Make invalid states hard to express

Parse and validate at boundaries. Convert raw input into trusted internal types.
Use domain types, value objects, enums/unions/results, or value types where useful.
Make intent and invalid states explicit.

#### 4. Prefer predictable functions

A function should never surprise its caller.
Prefer explicit everything and minimal hidden state, pure functions. Return values for expected failure.
Throw exceptions for programmer errors, or unrecoverable conditions.

#### 5. Keep the happy path obvious

Use guard clauses for invalid, irrelevant, or exceptional cases.
Avoid deep nesting. Make the main behavior read top-to-bottom.

### Red flags

- pass-through methods or layers
- deep nesting
- primitive-heavy domain logic
- unclear ownership of state
- broad catch/throw chains for expected cases
- comments explaining obvious code
