# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project uses date-based versioning: `YYYY.MM.DD.N` where N is the commit count for that day.

This library implements [TOON v1.3 specification](https://github.com/toon-format/spec) (2025-10-31).

## [Unreleased]

### Added
- Date-commit-count versioning scheme (YYYY.MM.DD.N)
- Reference to official TOON v1.3 specification
- Comprehensive README with:
  - Token efficiency benchmarks (49% reduction vs formatted JSON)
  - TOON's sweet spot explanation (uniform arrays of objects)
  - Comparison with JSON, CSV, YAML for different use cases
  - Links to specification and other implementations
- CONTRIBUTING.md with development guidelines
- Property-based roundtrip tests using test.check
- Enhanced error messages with actionable suggestions and examples
- Comprehensive test coverage (340+ tests, 792+ assertions)

### Changed
- Consolidated shared namespaces into single `utils.cljc`
- Deduplicated escape logic across CLJ and CLJS implementations
- Extracted magic string literals to constants
- Updated build.clj with proper metadata and description
- Improved code organization and readability

### Fixed
- Root-level array encoding and decoding
- String trim function compatibility
- Key quoting for numeric patterns
- Array roundtrip test expectations

## [0.1.0] - 2025-11-04

### Added
- Core TOON encoder and decoder
- Support for objects, arrays, and primitives
- Inline, tabular, and list array formats
- Configurable delimiters (comma, tab, pipe)
- Optional length markers
- Strict and relaxed parsing modes
- Comprehensive test suite
- CLJ and CLJS support

### Features
- **Token Efficiency**: 30-60% reduction vs JSON
- **Readability**: Human-readable indented format
- **Type Normalization**: Automatic conversion to JSON-compatible types
- **Error Handling**: Detailed error messages with context
- **Format Options**: Customizable delimiters and length markers
- **Validation**: Optional strict mode for format compliance

[Unreleased]: https://github.com/vadelabs/toon/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/vadelabs/toon/releases/tag/v0.1.0
