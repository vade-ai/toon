(ns com.vadelabs.toon.shared.quote
  "String quoting logic for TOON encoding.

  Handles delimiter-aware quoting to ensure values can be safely
  encoded without ambiguity. Uses JSON-style escaping with backslashes."
  (:require
    [com.vadelabs.str.interface :as str]
    [com.vadelabs.toon.constants :as const])
  #?(:cljs (:require [goog.string])))


;; ============================================================================
;; Pattern Detection Helpers
;; ============================================================================

(defn boolean-literal? [value]
  (or (= value "true") (= value "false")))

(defn null-literal? [value]
  (= value "null"))

(defn numeric-like? [value]
  (boolean (re-matches #"^-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?$" value)))

(defn has-structural-chars? [value]
  (boolean (re-find #"[\[\]{}\-]" value)))

(defn starts-with-hyphen? [value]
  (str/starts-with? value "-"))

(defn has-control-chars? [value]
  (or (str/includes? value "\n")
      (str/includes? value "\r")
      (str/includes? value "\t")))

(defn has-backslash? [value]
  (str/includes? value "\\"))

(defn has-whitespace-padding? [value]
  (not= value (str/trim value)))


;; ============================================================================
;; Escaping Logic
;; ============================================================================

(defn escaped
  "Returns an escaped version of the string using JSON-style backslash escaping.

  Single-pass implementation for performance.

  Escape rules:
  - Backslash → \\\\
  - Double quote → \\\"
  - Newline → \\n
  - Carriage return → \\r
  - Tab → \\t

  Parameters:
    - value: String to escape

  Returns:
    Escaped string.

  Examples:
    (escaped \"say \\\"hi\\\"\") ;=> \"say \\\\\\\"hi\\\\\\\"\"
    (escaped \"line1\\nline2\") ;=> \"line1\\\\nline2\"
    (escaped \"C:\\\\path\")    ;=> \"C:\\\\\\\\path\""
  [value]
  (let [sb #?(:clj (StringBuilder. (count value))
              :cljs (goog.string/StringBuffer.))]
    (doseq [c value]
      (case c
        \\ (.append sb "\\\\")
        \" (.append sb "\\\"")
        \newline (.append sb "\\n")
        \return (.append sb "\\r")
        \tab (.append sb "\\t")
        (.append sb c)))
    (.toString sb)))


;; ============================================================================
;; Quoting Logic
;; ============================================================================

(defn needs-quoting?
  "Returns true if a string value needs quoting in TOON format.

  A string needs quoting if it:
  - Is empty or blank
  - Has leading/trailing whitespace
  - Exactly matches reserved literals: 'true', 'false', 'null'
  - Looks like a number (e.g., '42', '-3.14', '1e-6')
  - Contains the active delimiter
  - Contains structural characters: [ ] { } -
  - Starts with hyphen (conflicts with list markers)
  - Contains colon (key-value separator)
  - Contains double quotes
  - Contains backslashes
  - Contains control characters (newline, tab, carriage return)

  Parameters:
    - value: String value to check
    - delimiter: Delimiter character being used (default: comma)

  Returns:
    Boolean indicating if quoting is needed."
  ([value]
   (needs-quoting? value const/default-delimiter))
  ([value delimiter]
   (or
     ;; Empty or blank strings need quoting
     (str/blank? value)

     ;; Leading/trailing whitespace needs quoting
     (has-whitespace-padding? value)

     ;; Reserved literals need quoting to avoid ambiguity
     (boolean-literal? value)
     (null-literal? value)

     ;; Numeric-like strings need quoting
     (numeric-like? value)

     ;; Structural characters need quoting
     (has-structural-chars? value)

     ;; Contains active delimiter
     (str/includes? value delimiter)

     ;; Contains colon (key-value separator)
     (str/includes? value const/colon)

     ;; Contains characters that need escaping
     (str/includes? value const/double-quote)
     (has-backslash? value)
     (has-control-chars? value))))


(defn wrap
  "Wraps a string value in double quotes for safe encoding in TOON format.

  Quoting rules:
  1. Escape special characters using JSON-style backslash escaping
  2. Wrap result in double quotes

  Parameters:
    - value: String value to quote

  Returns:
    Quoted string safe for TOON encoding.

  Examples:
    (quoted \"hello\")         ;=> \"\\\"hello\\\"\"
    (quoted \"say \\\"hi\\\"\") ;=> \"\\\"say \\\\\\\"hi\\\\\\\"\\\"\"
    (quoted \"a,b\")           ;=> \"\\\"a,b\\\"\"
    (quoted \"line1\\nline2\") ;=> \"\\\"line1\\\\nline2\\\"\""
  [value]
  (let [esc (escaped value)]
    (str const/double-quote esc const/double-quote)))


(defn maybe-quote
  "Quotes a string if it needs quoting, otherwise returns it unchanged.

  Parameters:
    - value: String value to potentially quote
    - delimiter: Delimiter character being used (default: comma)

  Returns:
    Original or quoted string.

  Examples:
    (maybe-quote \"simple\")      ;=> \"simple\"
    (maybe-quote \"has, comma\")  ;=> \"\\\"has, comma\\\"\"
    (maybe-quote \"true\")        ;=> \"\\\"true\\\"\" (reserved literal)
    (maybe-quote \"42\")          ;=> \"\\\"42\\\"\" (numeric-like)"
  ([value]
   (maybe-quote value const/default-delimiter))
  ([value delimiter]
   (if (needs-quoting? value delimiter)
     (wrap value)
     value)))
