# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project uses date-based versioning: `YYYY.MM.DD-N` where N is the commit count for that day.

This library implements [TOON v1.3 specification](https://github.com/toon-format/spec) (2025-10-31).

## [1.0.0] - 2025-11-05

First stable release of TOON (Token-Oriented Object Notation) for Clojure/ClojureScript.

### Core Features

**Encoding & Decoding**
- Full TOON v1.3 specification compliance
- Bidirectional conversion between Clojure data structures and TOON format
- Support for objects, arrays, and primitive values
- Automatic type normalization to JSON-compatible types

**Array Formats**
- **Inline arrays**: Primitive values in comma-separated format (`tags[3]: a,b,c`)
- **Tabular arrays**: Uniform objects with field headers (`[2]{id,name}: 1,Alice 2,Bob`)
- **List arrays**: Non-uniform or mixed-type items with `- ` prefix

**Format Options**
- Configurable delimiters: comma (default), tab (`\t`), or pipe (`|`)
- Optional length markers with `#` prefix (e.g., `items[#3]`)
- Adjustable indentation size (default: 2 spaces)
- Strict and relaxed parsing modes

**Type Support**
- Primitives: strings, numbers, booleans, null
- Nested objects with indentation-based structure
- Arrays of primitives, objects, or mixed types
- Arrays of arrays (nested arrays)
- Empty containers: `[]`, `{}`, `[0]:`, `key:`

**String Handling**
- Intelligent quoting: only when necessary for delimiters, colons, reserved words
- Escape sequences: `\"`, `\\`, `\n`, `\t`, control characters
- Unicode and emoji support (unquoted)
- Leading/trailing whitespace preservation

**Platform Support**
- Clojure (JVM)
- ClojureScript (JavaScript)
- Cross-platform `.cljc` implementation

### Performance & Quality

**Token Efficiency**
- 49.1% reduction vs formatted JSON (2-space indentation)
- 28.0% reduction vs compact JSON (minified)
- 39.4% reduction vs YAML
- 56.0% reduction vs XML

**Testing**
- 340+ unit tests with 792+ assertions
- Property-based roundtrip tests using test.check
- Comprehensive edge case coverage
- Tests for primitives, arrays, objects, nesting, special characters
- Cross-platform compatibility testing

**Error Handling**
- Descriptive error messages with context
- Actionable suggestions for fixes
- Examples of correct usage
- Validation in strict mode:
  - Array length mismatches
  - Invalid escape sequences
  - Malformed headers and syntax errors

### Documentation

**User Documentation**
- Comprehensive README with:
  - Quick start guide and API reference
  - Format examples with JSON comparisons
  - Token efficiency benchmarks
  - Use case guidance (when to use TOON vs JSON vs CSV)
  - Links to official specification and benchmarks
- CONTRIBUTING.md for contributors
- SPEC.md reference to official specification

**Code Quality**
- Consolidated namespace organization
- Deduplicated escape logic across platforms
- Named constants for magic strings
- Comprehensive docstrings on public API
- Clear separation of concerns (encode/decode/parse/scan)

### API

**Main Functions**

`encode(value, options)`
- Encodes Clojure data to TOON format
- Options: `:indent`, `:delimiter`, `:length-marker`
- Returns TOON string with no trailing newline

`decode(input, options)`
- Decodes TOON format to Clojure data
- Options: `:indent`, `:strict`
- Returns Clojure data structures (maps, vectors, primitives)

### Build & Release

- Date-based semantic versioning (YYYY.MM.DD-N)
- Automatic version calculation from git commit count
- JAR packaging for Maven/Clojars distribution
- Comprehensive POM metadata

### License

- MIT License for broad compatibility and commercial use

### Links

- [TOON v1.3 Specification](https://github.com/toon-format/spec)
- [Reference Implementation (TypeScript)](https://github.com/toon-format/toon)
- [Conformance Tests](https://github.com/toon-format/spec/tree/main/tests)

[1.0.0]: https://github.com/vadelabs/toon/releases/tag/v1.0.0
