(ns com.vadelabs.toon.encode.keys
  "Key manipulation utilities for TOON encoding.

  Provides functions to fold nested single-key objects into dotted paths."
  (:require
    [clojure.string :as str]
    [com.vadelabs.toon.constants :as const]
    [com.vadelabs.toon.utils :as utils]))


;; ============================================================================
;; Key Folding Helpers
;; ============================================================================

(defn- json-object?
  "Returns true if value is a map (JSON object)."
  [value]
  (map? value))


(defn- empty-object?
  "Returns true if value is an empty map."
  [value]
  (and (map? value) (empty? value)))


(defn- chain
  "Returns chain information for a single-key object traversal.

  Traverses nested objects, collecting keys until:
  - A non-single-key object is found
  - An array is encountered
  - A primitive is reached
  - An empty object is reached
  - The depth limit is reached

  Parameters:
    - start-key: The initial key to start the chain
    - start-value: The value to traverse
    - max-depth: Maximum number of segments to collect

  Returns:
    Map with :segments (vector of keys), :tail (remainder value), :leaf-value (final value)"
  [start-key start-value max-depth]
  (loop [segments [start-key]
         current-value start-value]
    (if (>= (count segments) max-depth)
      ;; Depth limit reached
      {:segments segments
       :tail current-value
       :leaf-value current-value}
      ;; Try to continue the chain
      (if-not (json-object? current-value)
        ;; Not an object - this is a leaf
        {:segments segments
         :tail nil
         :leaf-value current-value}
        ;; It's an object - check if it has exactly one key
        (let [ks (keys current-value)]
          (if (not= (count ks) 1)
            ;; Not a single-key object - stop here
            (if (empty? ks)
              ;; Empty object is a leaf
              {:segments segments
               :tail nil
               :leaf-value current-value}
              ;; Multi-key object - return as tail
              {:segments segments
               :tail current-value
               :leaf-value current-value})
            ;; Single-key object - continue the chain
            (let [next-key (first ks)
                  next-value (get current-value next-key)]
              (recur (conj segments next-key) next-value))))))))


(defn- dotted-key
  "Returns a dotted key string from segments.

  Parameters:
    - segments: Vector of key segments

  Returns:
    Dotted key string (e.g., \"data.config.server\")"
  [segments]
  (str/join const/dot segments))


(defn fold
  "Folds a key-value pair into a dotted path, or returns nil if folding not possible.

  Folding traverses nested objects with single keys, collapsing them into a dotted path.
  It stops when:
  - A non-single-key object is encountered
  - An array is encountered
  - A primitive value is reached
  - The flatten depth limit is reached
  - Any segment fails safe mode validation

  Safe mode requirements:
  - key-folding must be :safe
  - Every segment must be a valid identifier (no dots, no special chars)
  - The folded key must not collide with existing sibling keys
  - The folded key must not collide with root-level literal dotted keys

  Parameters:
    - key: The starting key to fold
    - value: The value associated with the key
    - siblings: Vector of all sibling keys at this level (for collision detection)
    - options: Map with :key-folding and :flatten-depth
    - root-literal-keys: Optional set of dotted keys that exist at root level
    - path-prefix: Optional string prefix for building absolute path

  Returns:
    Map with :folded-key, :remainder, :leaf-value, :segment-count if folding is possible,
    nil otherwise"
  ([key value siblings options]
   (fold key value siblings options nil nil))
  ([key value siblings options root-literal-keys path-prefix]
   ;; Only fold when safe mode is enabled
   (when (= (:key-folding options) :safe)
     ;; Can only fold objects
     (when (json-object? value)
       (let [;; Use provided flatten-depth or fall back to options default
             effective-flatten-depth (:flatten-depth options ##Inf)

             ;; Collect the chain of single-key objects
             {:keys [segments tail leaf-value]} (chain key value effective-flatten-depth)]

         ;; Need at least 2 segments for folding to be worthwhile
         (when (>= (count segments) 2)
           ;; Validate all segments are safe identifiers
           (when (every? utils/identifier-segment? segments)
             (let [;; Build the folded key (relative to current nesting level)
                   folded-key (dotted-key segments)

                   ;; Build the absolute path from root
                   absolute-path (if path-prefix
                                   (str path-prefix const/dot folded-key)
                                   folded-key)]

               ;; Check for collision with existing literal sibling keys (at current level)
               (when-not (some #{folded-key} siblings)
                 ;; Check for collision with root-level literal dotted keys
                 (when-not (and root-literal-keys (contains? root-literal-keys absolute-path))
                   {:folded-key folded-key
                    :remainder tail
                    :leaf-value leaf-value
                    :segment-count (count segments)}))))))))))
