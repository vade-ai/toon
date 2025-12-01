# Changelog

All notable changes to this project will be documented in this file.

This project uses date-based versioning: `YYYY.MM.DD-N` where N is the number of commits since the last release.

This library implements [TOON v2.0 specification](https://github.com/toon-format/spec) (2025-11-10).

## [Unreleased]

## [2025.12.01-36] - 2025-12-01

### Added

- **Streaming decode API** - New event-based streaming decoder for memory-efficient processing
  - `events` - Returns lazy sequence of parse events from TOON input
  - `events-ch` - Returns core.async channel of parse events for async processing
  - `events->value` - Reconstructs values from event streams
  - Event types: `:start-object`, `:end-object`, `:start-array`, `:end-array`, `:key`, `:primitive`
  - Array start events include `:length` property
  - Key events include `:was-quoted` property when key was quoted

- **New API functions**
  - `lines->value` - Decode from pre-split lines (useful for streaming line-by-line input)
  - `encode-lines` - Returns lazy sequence of encoded lines (streaming line output)

- **ClojureScript testing** - Added cljs-test-runner for cross-platform test verification

### Changed

- **BREAKING: Namespace rename** - Renamed `com.vadelabs.toon.interface` to `com.vadelabs.toon.core`
  - Avoids JavaScript reserved keyword `interface` causing namespace munging in ClojureScript
  - Update your requires: `[com.vadelabs.toon.core :as toon]`

- **Naming conventions** - Applied Stuart Sierra naming conventions throughout
  - `cursor->events` (was `decode-events-from-cursor`)
  - `list-item-type` (was `detect-list-item-type`)
  - `event-key` (was `key`) to avoid shadowing `clojure.core/key`

- **Code quality** - Refactored event-builder and streaming code
  - Extracted helpers for better readability
  - Reduced CLJ/CLJS duplication with shared helpers
  - Simplified event API with optional was-quoted

### Fixed

- Support for list arrays with nested objects in streaming decode
- Strict mode validation for array counts in streaming decode
- Babashka-compatible LineWriter

### Technical Details

- 520 tests with 1074 assertions (up from 456 tests)
- Added core.async dependency for async streaming support

## [2025.11.20-10] - 2025-11-20

### Changed

- **TOON v2.0 compliance** - Updated from v1.4 to v2.0 specification

- **Code quality** - Extensive refactoring for better maintainability
  - Extracted helper functions to simplify complex decoders and encoders
  - Reduced function complexity through focused, single-purpose helpers
  - Improved code organization in parser and scanner modules
  - Pre-compiled regex patterns in parser
  - Replaced lazy sequences with eager evaluation in encoders

### Added

- **Safety improvements**
  - Depth limiting to prevent stack overflow in normalize function
  - Validation for empty brackets and negative array lengths in parser
  - Enhanced error messages with specific suggestions

- **Test coverage** - Improved from 91.5% to 92.6% overall coverage
  - Added tests for max-depth validation and exception metadata
  - Added tests for parser edge cases (empty brackets, negative array lengths)
  - 456 tests with 958 assertions

## [2025.11.12-5] - 2025-11-12

### Added

- **Key collapsing** - New `:key-collapsing` option for encoding nested single-key objects into dotted paths
  - Reduces token usage by 40-60% for deeply nested structures
  - Collapses chains like `{data: {config: {server: "localhost"}}}` → `data.config.server: localhost`
  - Safe mode validation ensures only valid identifiers are collapsed
  - Collision detection prevents conflicts with existing keys
  - Configurable depth limit via `:flatten-depth` option

- **Path expansion** - New `:expand-paths` option for decoding dotted keys back to nested objects
  - Reverses key collapsing during decode for lossless round-trips
  - Deep merge support for overlapping paths
  - Strict/non-strict conflict resolution modes
  - Full round-trip guarantee (encode → decode preserves data)

### Changed

- Improved code quality with reduced complexity and better maintainability
  - Reduced nesting depth in key collapsing logic (5 levels → 3 levels)
  - Extracted reusable conflict handler to eliminate duplication
  - Simplified control flow in traversal functions
  - Pre-compiled regex patterns for better performance
  - Added precondition guards for safer error handling

### Removed

- **Length marker option** - Removed `:length-marker` option from encode API
  - Always use `[N]` format for array lengths (no more `[#N]` syntax)
  - Simplified API and implementation
  - **BREAKING CHANGE**: The `:length-marker` option is no longer supported

### Technical Details

- 126 new tests for key collapsing and path expansion (435 tests total)
- All tests passing with 929 assertions
- Code quality improvements: -19 lines, 83% less duplication, 33% lower cyclomatic complexity

## [2025.11.11-3] - 2025-11-11

### Changed

- **TOON v1.4 compliance** - Updated to TOON specification v1.4 (2025-11-05)
- **Negative zero normalization** - Parser now normalizes `-0` to `0` per v1.4 spec requirement
- **Updated documentation** - All spec version references updated from v1.3 to v1.4

### Technical Details

- Updated `com.vadelabs.toon.decode.parser/number` to normalize negative zero
- Updated README.md spec badges and references to v1.4
- Updated SPEC.md version from 1.3 to 1.4
- Updated build.clj pom description to reference v1.4
- All 340 tests passing with 792 assertions

## [2025.11.05-43] - 2025-11-05

First public release! 🎉

A Clojure/ClojureScript implementation of TOON (Token-Oriented Object Notation) - a compact format for passing data to LLMs with significantly fewer tokens than JSON.

### What's included

- **Full TOON v1.3 support** - encode and decode between Clojure data and TOON format
- **Three array styles** - inline for primitives, tabular for uniform objects, list for mixed data
- **Flexible options** - choose your delimiter (comma, tab, pipe), adjust indentation
- **Smart string handling** - only quotes when necessary, supports Unicode and emoji
- **Both platforms** - works in Clojure (JVM) and ClojureScript
- **Well tested** - 340+ tests with 90%+ code coverage including property-based roundtrip testing
- **Great errors** - helpful messages with suggestions when things go wrong
- **Comprehensive docs** - README with examples, API reference, and contribution guidelines
- **CI/CD** - Automated testing and deployment to Clojars via GitHub Actions
- **Smart versioning** - Version number reflects commits since last release

### Why use TOON?

Saves tokens when sending structured data to LLMs:
- 49% fewer tokens than formatted JSON
- 28% fewer than minified JSON
- Works best for uniform arrays of objects (like database query results)

### Getting started

```clojure
;; Add to deps.edn
com.vadelabs/toon {:mvn/version "2025.11.05-43"}

;; Use it
(require '[com.vadelabs.toon.core :as toon])

(toon/encode {:users [{:id 1 :name "Alice"} {:id 2 :name "Bob"}]})
;=> "users[2]{id,name}:\n  1,Alice\n  2,Bob"
```

### Links

- [TOON Specification](https://github.com/toon-format/spec)
- [Reference Implementation (TypeScript)](https://github.com/toon-format/toon)
- [Other Implementations](https://github.com/toon-format/toon#other-implementations)

[2025.12.01-36]: https://github.com/vadelabs/toon/releases/tag/v2025.12.01-36
[2025.11.20-10]: https://github.com/vadelabs/toon/releases/tag/v2025.11.20-10
[2025.11.12-5]: https://github.com/vadelabs/toon/releases/tag/v2025.11.12-5
[2025.11.11-3]: https://github.com/vadelabs/toon/releases/tag/v2025.11.11-3
[2025.11.05-43]: https://github.com/vadelabs/toon/releases/tag/v2025.11.05-43
