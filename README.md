# TOON: Token-Oriented Object Notation

[![Clojars Project](https://img.shields.io/clojars/v/com.vadelabs/toon.svg)](https://clojars.org/com.vadelabs/toon)
[![License: EPL-1.0](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://www.eclipse.org/legal/epl-v10.html)

A compact, human-readable data format optimized for LLMs, reducing token usage by **30-60% compared to JSON** while maintaining readability.

## Why TOON?

When working with Large Language Models, token efficiency directly impacts:
- **Cost**: Fewer tokens = lower API costs
- **Context**: More data in the same context window
- **Speed**: Faster processing with less data to parse

TOON achieves significant token reduction through:
- **No redundant delimiters**: Uses indentation instead of `{}` and `[]`
- **Minimal punctuation**: Eliminates quotes where possible
- **Compact arrays**: Inline values for primitives, tabular format for objects
- **Type inference**: Numbers, booleans, and null don't need quotes

## Installation

### Clojure CLI/deps.edn

```clojure
com.vadelabs/toon {:mvn/version "2025.11.05.8"}
```

### Leiningen/Boot

```clojure
[com.vadelabs/toon "2025.11.05.8"]
```

## Quick Start

```clojure
(require '[com.vadelabs.toon.interface :as toon])

;; Encode Clojure data to TOON
(toon/encode {:name "Alice" :age 30 :tags ["dev" "rust"]})
;=> "name: Alice\nage: 30\ntags[2]: dev,rust"

;; Decode TOON to Clojure data
(toon/decode "name: Alice\nage: 30\ntags[2]: dev,rust")
;=> {"name" "Alice", "age" 30.0, "tags" ["dev" "rust"]}
```

## Format Examples

### Objects

**JSON** (50 tokens):
```json
{
  "name": "Alice",
  "age": 30,
  "active": true
}
```

**TOON** (18 tokens):
```
name: Alice
age: 30
active: true
```

### Nested Objects

**JSON** (80 tokens):
```json
{
  "user": {
    "name": "Alice",
    "email": "alice@example.com"
  }
}
```

**TOON** (30 tokens):
```
user:
  name: Alice
  email: alice@example.com
```

### Arrays of Primitives

**JSON** (40 tokens):
```json
{
  "tags": ["reading", "gaming", "coding"]
}
```

**TOON** (15 tokens):
```
tags[3]: reading,gaming,coding
```

### Arrays of Objects (Tabular Format)

**JSON** (120 tokens):
```json
[
  {"id": 1, "name": "Alice", "score": 95},
  {"id": 2, "name": "Bob", "score": 87},
  {"id": 3, "name": "Carol", "score": 92}
]
```

**TOON** (45 tokens):
```
[3]{id,name,score}:
  1,Alice,95
  2,Bob,87
  3,Carol,92
```

### Arrays of Mixed Items (List Format)

**TOON**:
```
items[3]:
  - name: Laptop
    price: 999
  - name: Mouse
    price: 29
  - name: Keyboard
    price: 79
```

## API Reference

### `encode`

Encodes Clojure data structures to TOON format.

```clojure
(encode input)
(encode input options)
```

**Parameters:**
- `input` - Any Clojure value (normalized to JSON-compatible types)
- `options` - Optional map:
  - `:indent` - Spaces per indentation level (default: 2)
  - `:delimiter` - Array value delimiter: `","` (default), `"\t"`, or `"|"`
  - `:length-marker` - Array length marker: `"#"` or `false` (default: false)

**Returns:** String in TOON format

**Examples:**

```clojure
;; Basic encoding
(encode {:name "Ada" :tags ["reading" "gaming"]})
;=> "name: Ada\ntags[2]: reading,gaming"

;; Custom delimiter
(encode {:tags ["a" "b" "c"]} {:delimiter "\t"})
;=> "tags[3\t]: a\tb\tc"

;; Length marker prefix
(encode {:items [1 2 3]} {:length-marker "#"})
;=> "items[#3]: 1,2,3"

;; Tabular array format
(encode [{:id 1 :name "Alice"}
         {:id 2 :name "Bob"}])
;=> "[2]{id,name}:\n  1,Alice\n  2,Bob"
```

### `decode`

Decodes TOON format to Clojure data structures.

```clojure
(decode input)
(decode input options)
```

**Parameters:**
- `input` - String in TOON format
- `options` - Optional map:
  - `:indent` - Spaces per indentation level (default: 2)
  - `:strict` - Enable strict validation (default: true)

**Returns:** Clojure data structure (maps, vectors, primitives)

**Examples:**

```clojure
;; Basic decoding
(decode "name: Ada\ntags[2]: reading,gaming")
;=> {"name" "Ada", "tags" ["reading" "gaming"]}

;; Tabular array
(decode "[2]{id,name}:\n  1,Alice\n  2,Bob")
;=> [{"id" 1.0, "name" "Alice"} {"id" 2.0, "name" "Bob"}]

;; Inline array
(decode "[3]: 1,2,3")
;=> [1.0 2.0 3.0]

;; Relaxed mode (allows tabs, inconsistent indentation)
(decode "name: Ada" {:strict false})
;=> {"name" "Ada"}
```

## Format Specification

### Primitives

```
string: Hello World
number: 42
float: 3.14
boolean: true
nil: null
```

### Quoted Strings

Strings are quoted when they contain special characters:

```
comma: "a,b"
colon: "key:value"
reserved: "true"
newline: "line1\nline2"
```

### Objects

Key-value pairs separated by colons:

```
name: Alice
age: 30
```

Nested objects use indentation:

```
user:
  name: Alice
  email: alice@example.com
```

### Arrays

**Inline format** (primitives):
```
tags[3]: reading,gaming,coding
```

**Tabular format** (objects with same keys):
```
[3]{id,name}:
  1,Alice
  2,Bob
  3,Carol
```

**List format** (mixed items):
```
items[2]:
  - name: Laptop
    price: 999
  - name: Mouse
    price: 29
```

### Options

**Custom delimiter:**
```
tags[3|]: a|b|c
tags[3\t]: a\tb\tc
```

**Length marker:**
```
items[#3]: 1,2,3
```

## Type Normalization

TOON normalizes Clojure types to JSON-compatible values:

- Keywords → Strings: `:name` → `"name"`
- Sets → Sorted vectors: `#{3 1 2}` → `[1 2 3]`
- All numbers → Doubles: `42` → `42.0`
- Maps → String-keyed maps: `{:a 1}` → `{"a" 1.0}`

## Testing

```bash
# Run all Clojure tests
bb test

# Run all tests (Clojure + Babashka)
bb test:all

# Run CI pipeline with tests
bb ci
```

The library includes:
- 340+ unit tests
- Property-based tests using test.check
- Comprehensive roundtrip testing
- Edge case coverage

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development setup
- Coding guidelines
- Testing requirements
- Pull request process

### Quick Contribution Guide

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes with tests
4. Run tests: `bb test`
5. Commit with clear messages: `git commit -m "add feature X"`
6. Push and create a pull request

## Roadmap

- [ ] ClojureScript browser support
- [ ] Streaming encoder/decoder
- [ ] Custom type handlers
- [ ] Performance benchmarks vs JSON
- [ ] Language interop (Java, JavaScript)

## License

Copyright © 2025 Pragyan

Distributed under the Eclipse Public License version 1.0.

## Links

- [GitHub Repository](https://github.com/vadelabs/toon)
- [Clojars Package](https://clojars.org/com.vadelabs/toon)
- [Issue Tracker](https://github.com/vadelabs/toon/issues)
- [Changelog](CHANGELOG.md)
