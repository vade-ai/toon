# Contributing to TOON

Thank you for your interest in contributing to TOON! This document provides guidelines and information to help you contribute effectively.

## Code of Conduct

Be respectful, constructive, and professional in all interactions. We're building something useful together.

## Getting Started

### Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/install_clojure)
- [Babashka](https://babashka.org/) (optional, for faster task execution)
- Git
- A text editor with Clojure support (VS Code + Calva, Emacs + CIDER, IntelliJ + Cursive, etc.)

### Development Setup

1. **Fork and clone the repository:**

```bash
git clone https://github.com/YOUR_USERNAME/toon.git
cd toon
```

2. **Run the tests to verify your setup:**

```bash
bb test          # Run Clojure tests
bb test:bb       # Run Babashka tests (if applicable)
bb test:all      # Run all tests
```

3. **Try building the library:**

```bash
bb ci            # Run CI pipeline (tests + build)
```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

Use descriptive branch names:
- `feature/add-streaming-support`
- `fix/encoding-edge-case`
- `docs/improve-api-examples`
- `refactor/simplify-parser`

### 2. Make Your Changes

- Write clear, idiomatic Clojure code
- Follow existing code style and conventions
- Add docstrings to public functions
- Keep functions focused and small
- Use meaningful variable names

### 3. Write Tests

**Every change must include tests.** We use:

- **Unit tests**: Test individual functions and edge cases
- **Property-based tests**: Use `test.check` for roundtrip and invariant testing
- **Integration tests**: Test end-to-end encoding/decoding workflows

Example test structure:

```clojure
(ns com.vadelabs.toon.feature-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [clojure.test.check.clojure-test :refer [defspec]]
    [com.vadelabs.toon.interface :as toon]))

(deftest my-feature-test
  (testing "basic functionality"
    (is (= expected (my-function input))))

  (testing "edge cases"
    (is (= edge-expected (my-function edge-input)))))

(defspec my-feature-property-test 100
  (prop/for-all [input gen/some-generator]
    (= input (roundtrip input))))
```

### 4. Run Tests Locally

```bash
# Run all tests
bb test

# Run specific test namespace
clojure -X:test :only [com.vadelabs.toon.feature-test]

# Run with coverage (if configured)
bb test:coverage
```

**All tests must pass before submitting a PR.**

### 5. Commit Your Changes

Use clear, concise commit messages:

```bash
git commit -m "add streaming encoder support"
git commit -m "fix quoted string edge case in parser"
git commit -m "improve API documentation with examples"
```

**Commit message guidelines:**
- Use lowercase, imperative mood
- Keep first line under 50 characters
- Be specific about what changed
- No need for "Co-Authored-By" or emoji

### 6. Push and Create a Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub with:
- **Clear title**: Summarize the change
- **Description**: Explain what and why
- **Tests**: Confirm all tests pass
- **Documentation**: Update README if needed

## Code Style Guidelines

### General Principles

- **Readability over cleverness**: Write code others can understand
- **Composition over complexity**: Use simple functions that compose well
- **Explicit over implicit**: Make data flow and dependencies clear
- **Test everything**: If it can break, test it

### Naming Conventions

- **Functions**: `kebab-case` (e.g., `parse-array-header`)
- **Private functions**: Prefix with `-` (e.g., `-internal-helper`)
- **Predicates**: End with `?` (e.g., `empty-array?`)
- **Mutable operations**: End with `!` (e.g., `append!`)
- **Constants**: `kebab-case` with `def` (e.g., `default-delimiter`)

### Code Organization

```clojure
(ns com.vadelabs.toon.feature
  "Brief description of namespace purpose."
  (:require
    [clojure.string :as str]
    [com.vadelabs.toon.utils :as utils]))

;; ============================================================================
;; Constants
;; ============================================================================

(def default-value 42)

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn- private-helper
  "Helper function description."
  [arg]
  ...)

;; ============================================================================
;; Public API
;; ============================================================================

(defn public-function
  "Public function with comprehensive docstring.

  Parameters:
    - arg1: Description of first argument
    - arg2: Description of second argument

  Returns:
    Description of return value

  Examples:
    (public-function 1 2) ;=> 3"
  [arg1 arg2]
  ...)
```

### Docstring Format

```clojure
(defn example-function
  "One-line summary of what the function does.

  More detailed explanation if needed.

  Parameters:
    - param1: Description with type info
    - param2: Description with type info
    - options: Optional map (default: {...})

  Returns:
    Description of return value

  Examples:
    (example-function x y)
    ;=> result"
  [param1 param2 & [options]]
  ...)
```

## Testing Guidelines

### Unit Tests

- Test normal cases and edge cases
- Use descriptive test names
- One assertion per test when possible
- Use `testing` blocks for grouping

```clojure
(deftest parse-array-test
  (testing "inline arrays"
    (is (= [1 2 3] (parse-array "[3]: 1,2,3"))))

  (testing "empty arrays"
    (is (= [] (parse-array "[]"))))

  (testing "invalid input"
    (is (thrown? ExceptionInfo (parse-array "invalid")))))
```

### Property-Based Tests

- Use for roundtrip testing
- Test invariants (e.g., encoding + decoding = identity)
- Generate diverse test data

```clojure
(defspec roundtrip-test 100
  (prop/for-all [data gen/json-like]
    (= (normalize data)
       (-> data encode decode))))
```

### Test Coverage

Aim for high coverage of:
- All public API functions
- Edge cases (empty input, special characters, large data)
- Error conditions (invalid input, malformed data)
- Platform-specific behavior (CLJ vs CLJS)

## Documentation

### When to Update Documentation

Update documentation when you:
- Add new features
- Change public API
- Fix important bugs
- Add new configuration options

### What to Update

- **README.md**: User-facing features and examples
- **Docstrings**: Function-level documentation
- **CHANGELOG.md**: Record all notable changes
- **Code comments**: Explain complex logic

### Documentation Style

- Be concise and clear
- Provide examples
- Explain why, not just what
- Keep examples up-to-date

## Pull Request Process

1. **Ensure all tests pass**: Run `bb test` locally
2. **Update documentation**: Add examples if needed
3. **Write clear PR description**: Explain what and why
4. **Request review**: Tag maintainers if needed
5. **Address feedback**: Respond to review comments
6. **Keep commits clean**: Squash if needed

### PR Checklist

- [ ] Tests pass locally
- [ ] New tests added for new functionality
- [ ] Documentation updated
- [ ] Code follows style guidelines
- [ ] Commit messages are clear
- [ ] No unrelated changes included

## Areas for Contribution

### High Priority

- **Performance optimization**: Faster encoding/decoding
- **Memory efficiency**: Reduce allocations
- **Error messages**: More helpful error messages
- **Examples**: Real-world use cases

### Medium Priority

- **ClojureScript support**: Browser compatibility
- **Streaming**: Encode/decode large data incrementally
- **Benchmarks**: Compare performance vs JSON
- **Tooling**: Editor plugins, formatters

### Low Priority

- **Features**: Custom type handlers, schema validation
- **Interop**: Java, JavaScript bindings
- **Documentation**: Tutorials, guides, videos

## Questions?

- **Issues**: [GitHub Issues](https://github.com/vadelabs/toon/issues)
- **Discussions**: [GitHub Discussions](https://github.com/vadelabs/toon/discussions)
- **Email**: pragyan@vadelabs.com

## License

By contributing to TOON, you agree that your contributions will be licensed under the MIT License.
