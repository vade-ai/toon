(ns com.vadelabs.toon.decode.arrays
  "Array decoding for TOON format.

  Handles inline, tabular, and list array formats."
  (:require
   [clojure.string :as str]
   [com.vadelabs.toon.constants :as const]
   [com.vadelabs.toon.decode.parser :as parser]
   [com.vadelabs.toon.decode.scanner :as scanner]
   [com.vadelabs.toon.utils :as str-utils]))

;; Forward declaration for mutual recursion
(declare list-item)

;; ============================================================================
;; Validation Helpers
;; ============================================================================

(defn- validate-array-length!
  "Validates array length matches header specification in strict mode.

  Parameters:
    - expected: Expected length from header
    - actual: Actual count of items/rows
    - strict: Enable validation (throws if mismatch)
    - array-type: Type identifier for error message (:inline, :tabular, :list)

  Throws:
    ex-info with :array-length-mismatch type if validation fails"
  [expected actual strict array-type]
  (when (and strict (not= actual expected))
    (let [type-suffix (case array-type
                        :tabular " rows"
                        :list " items"
                        " items")
          header-fix (if (= array-type :tabular)
                       (str "[" actual "]{...}")
                       (str "[" actual "]"))
          type-key (case array-type
                     :tabular :tabular-array-length-mismatch
                     :list :list-array-length-mismatch
                     :array-length-mismatch)]
      (throw (ex-info (str "Array length mismatch: header specifies " expected type-suffix " but found " actual)
                      {:type type-key
                       :expected expected
                       :actual actual
                       :suggestion (str "Update header to " header-fix " or add " (- expected actual) " more" type-suffix)})))))

;; ============================================================================
;; Inline Primitive Array Decoding
;; ============================================================================

(defn inline-primitive-array
  "Decodes an inline primitive array.

  Format: [3]: a,b,c

  Parameters:
    - header-info: Map from parse-array-header-line
    - strict: Enable strict mode validation (default true)

  Returns:
    Vector of parsed primitive values

  Strict mode validates:
    - Array length matches header"
  ([header-info]
   (inline-primitive-array header-info true))
  ([{:keys [inline-values delimiter length]} strict]
   (if-not inline-values
     []
     (let [tokens (parser/delimited-values inline-values delimiter)
           values (mapv #(parser/primitive-token % strict) tokens)]
       (validate-array-length! length (count values) strict :inline)
       values))))

;; ============================================================================
;; Tabular Array Decoding
;; ============================================================================

(defn- parse-tabular-row
  "Parses a single tabular row into values.

  Parameters:
    - row-content: String content of row
    - delimiter: Delimiter character
    - strict: Enable strict mode

  Returns:
    Vector of parsed primitive values"
  [row-content delimiter strict]
  (let [tokens (parser/delimited-values row-content delimiter)]
    (mapv #(parser/primitive-token % strict) tokens)))

(defn- analyze-line-positions
  "Analyzes positions of colon and delimiter in a line.

  Parameters:
    - content: Line content string
    - delimiter: Delimiter character

  Returns:
    Map with :colon-pos and :delim-pos (nil if not found)"
  [content delimiter]
  (let [delim-char (first delimiter)]
    {:colon-pos (str-utils/unquoted-char content \:)
     :delim-pos (str-utils/unquoted-char content delim-char)}))

(defn- delimiter-before-colon?
  "Checks if delimiter appears before colon in position analysis.

  Parameters:
    - positions: Map with :colon-pos and :delim-pos

  Returns:
    Boolean (true if both present and delimiter comes first)"
  [{:keys [colon-pos delim-pos]}]
  (and colon-pos delim-pos (< delim-pos colon-pos)))

(defn- peek-next-line-has-delimiter-first?
  "Checks if next line at same depth has delimiter before colon.

  Parameters:
    - cursor: LineCursor for look-ahead
    - depth: Depth to peek at
    - delimiter: Delimiter character

  Returns:
    Boolean (false if no next line)"
  [cursor depth delimiter]
  (if-let [next-line (scanner/peek-at-depth cursor depth)]
    (delimiter-before-colon? (analyze-line-positions (:content next-line) delimiter))
    false))

(defn- row-or-key-value?
  "Determines if a line is a data row or key-value line.

  Uses character position analysis with optional look-ahead for disambiguation:
  - No colon → data row
  - No delimiter → key-value line
  - Delimiter before colon → data row
  - Colon before delimiter → check look-ahead if provided

  Look-ahead strategy (when cursor and depth provided):
  If next line at same depth also has delimiter-before-colon pattern → data row
  Otherwise → key-value line

  Parameters:
    - content: Line content string
    - delimiter: Delimiter character
    - cursor: Optional LineCursor for look-ahead
    - depth: Optional depth for look-ahead

  Returns:
    :row or :key-value"
  ([content delimiter]
   (row-or-key-value? content delimiter nil nil))
  ([content delimiter cursor depth]
   (let [{:keys [colon-pos delim-pos] :as positions} (analyze-line-positions content delimiter)]
     (cond
       ;; No colon: must be row
       (nil? colon-pos)
       :row

       ;; No delimiter: must be key-value
       (nil? delim-pos)
       :key-value

       ;; Delimiter before colon: data row
       (delimiter-before-colon? positions)
       :row

       ;; Colon before delimiter: ambiguous case, use look-ahead if available
       (and cursor depth (peek-next-line-has-delimiter-first? cursor depth delimiter))
       :row

       ;; Default to key-value
       :else
       :key-value))))

(defn tabular-array
  "Decodes a tabular array into objects.

  Format:
    [2]{id,name}:
      1,Alice
      2,Bob

  Parameters:
    - header-info: Map from parse-array-header-line
    - cursor: LineCursor positioned after header
    - depth: Expected depth for rows
    - strict: Enable strict mode validation

  Returns:
    [decoded-objects, new-cursor]

  Strict mode validates:
    - Row count matches header length
    - No extra rows after expected count"
  ([header-info cursor depth]
   (tabular-array header-info cursor depth true))
  ([{:keys [fields delimiter length]} cursor depth strict]
   (loop [remaining-cursor cursor
          objects []
          row-count 0]
     (let [line (scanner/peek-at-depth remaining-cursor depth)]
       (if-not line
         ;; No more lines at depth
         (do
           (validate-array-length! length row-count strict :tabular)
           [objects remaining-cursor])
         ;; Check if this is a data row or key-value line with look-ahead
         (let [next-cursor (scanner/advance-cursor remaining-cursor)
               line-type (row-or-key-value? (:content line) delimiter next-cursor depth)]
           (if (= line-type :key-value)
             ;; End of rows (key-value line follows)
             (do
               (validate-array-length! length row-count strict :tabular)
               [objects remaining-cursor])
             ;; Parse data row
             (let [values (parse-tabular-row (:content line) delimiter strict)
                   obj (zipmap fields values)
                   new-cursor (scanner/advance-cursor remaining-cursor)]
               (recur new-cursor
                      (conj objects obj)
                      (inc row-count))))))))))

;; ============================================================================
;; List Array Decoding
;; ============================================================================

(defn list-array
  "Decodes a list-format array.

  Format:
    [3]:
      - item1
      - item2
      - item3

  Parameters:
    - header-info: Map from parse-array-header-line
    - cursor: LineCursor positioned after header
    - depth: Expected depth for items
    - strict: Enable strict mode validation
    - list-item-fn: Function to decode list items (for dependency injection)

  Returns:
    [decoded-items, new-cursor]

  Strict mode validates:
    - Item count matches header length
    - No extra items after expected count"
  ([{:keys [length delimiter]} cursor depth strict list-item-fn]
   (loop [remaining-cursor cursor
          items []
          item-count 0]
     (let [line (scanner/peek-at-depth remaining-cursor depth)]
       (if-not line
         ;; No more lines at depth
         (do
           (validate-array-length! length item-count strict :list)
           [items remaining-cursor])
         ;; Check if line starts with list marker (prefix "- " or bare "-")
         (if-not (or (str/starts-with? (:content line) const/list-item-prefix)
                     (= (:content line) const/list-item-marker))
           ;; No list marker: end of list
           (do
             (validate-array-length! length item-count strict :list)
             [items remaining-cursor])
           ;; Decode list item using provided function
           (let [[item new-cursor] (list-item-fn line remaining-cursor depth delimiter strict)]
             (recur new-cursor
                    (conj items item)
                    (inc item-count)))))))))
