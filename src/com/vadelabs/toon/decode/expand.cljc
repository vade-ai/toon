(ns com.vadelabs.toon.decode.expand
  "Path expansion utilities for TOON decoding.

  Provides functions to expand dotted keys into nested objects."
  (:require
    [clojure.string :as str]
    [com.vadelabs.toon.constants :as const]
    [com.vadelabs.toon.utils :as utils]))


;; ============================================================================
;; Path Expansion Helpers
;; ============================================================================

(defn- json-object?
  "Returns true if value is a map (JSON object)."
  [value]
  (map? value))


(defn- can-merge?
  "Returns true if two values can be deep merged (both are objects)."
  [a b]
  (and (json-object? a) (json-object? b)))


(defn- merge-objects
  "Deep merges properties from source into target (mutates target).

  For each key in source:
  - If key doesn't exist in target: copy it
  - If both values are objects: recursively merge
  - Otherwise: conflict (strict throws, non-strict overwrites)

  Parameters:
    - target: Target map to merge into
    - source: Source map to merge from
    - strict: Whether to throw on conflicts

  Returns:
    Merged map (same as target)

  Throws:
    ex-info if a conflict occurs in strict mode"
  [target source strict]
  (reduce-kv
    (fn [acc key source-value]
      (let [target-value (get acc key)]
        (cond
          ;; Key doesn't exist in target - copy it
          (nil? target-value)
          (assoc acc key source-value)

          ;; Both are objects - recursively merge
          (can-merge? target-value source-value)
          (assoc acc key (merge-objects target-value source-value strict))

          ;; Conflict: incompatible types
          :else
          (if strict
            (throw (ex-info (str "Path expansion conflict at key \"" key "\": cannot merge "
                                 (type target-value) " with " (type source-value))
                            {:type :path-expansion-conflict
                             :key key
                             :target-type (type target-value)
                             :source-type (type source-value)}))
            ;; Non-strict: overwrite (LWW)
            (assoc acc key source-value)))))
    target
    source))


(defn- insert-path
  "Inserts a value at a nested path, creating intermediate objects as needed.

  Walks the segment path, creating nested objects as needed.
  When an existing value is encountered:
  - If both are objects: deep merge (continue insertion)
  - If values differ: conflict
    - strict=true: throw ex-info
    - strict=false: overwrite with new value (LWW)

  Parameters:
    - target: Map to insert into
    - segments: Vector of path segments (e.g., [\"data\" \"metadata\" \"items\"])
    - value: Value to insert at the end of the path
    - strict: Whether to throw on conflicts

  Returns:
    Updated target map

  Throws:
    ex-info if a conflict occurs in strict mode"
  [target segments value strict]
  (let [last-idx (dec (count segments))
        last-seg (get segments last-idx)]
    ;; Build the nested path
    (if (zero? last-idx)
      ;; Only one segment - insert directly
      (let [existing (get target last-seg)]
        (cond
          (nil? existing)
          (assoc target last-seg value)

          (can-merge? existing value)
          (assoc target last-seg (merge-objects existing value strict))

          :else
          (if strict
            (throw (ex-info (str "Path expansion conflict at key \"" last-seg "\": cannot merge "
                                 (type existing) " with " (type value))
                            {:type :path-expansion-conflict
                             :key last-seg
                             :existing-type (type existing)
                             :value-type (type value)}))
            ;; Non-strict: overwrite
            (assoc target last-seg value))))

      ;; Multiple segments - walk the path
      (let [path-to-parent (subvec segments 0 last-idx)]
        (update-in target
                   path-to-parent
                   (fn [parent]
                     (let [parent-obj (or parent {})]
                       (if-not (json-object? parent-obj)
                         (if strict
                           (throw (ex-info (str "Path expansion conflict: expected object at path but found "
                                                (type parent-obj))
                                           {:type :path-expansion-conflict
                                            :path (str/join "." path-to-parent)
                                            :found-type (type parent-obj)}))
                           ;; Non-strict: replace with new object
                           {last-seg value})
                         ;; Parent is object - insert at last segment
                         (let [existing (get parent-obj last-seg)]
                           (cond
                             (nil? existing)
                             (assoc parent-obj last-seg value)

                             (can-merge? existing value)
                             (assoc parent-obj last-seg (merge-objects existing value strict))

                             :else
                             (if strict
                               (throw (ex-info (str "Path expansion conflict at key \"" last-seg "\": cannot merge "
                                                    (type existing) " with " (type value))
                                               {:type :path-expansion-conflict
                                                :key last-seg
                                                :existing-type (type existing)
                                                :value-type (type value)}))
                               ;; Non-strict: overwrite
                               (assoc parent-obj last-seg value))))))))))))


(defn paths
  "Expands dotted keys into nested objects in safe mode.

  Recursively traverses a decoded TOON value and expands any keys
  containing dots (`.`) into nested object structures, provided all segments
  are valid identifiers.

  Expansion rules:
  - Keys containing dots are split into segments
  - All segments must pass identifier validation
  - Non-eligible keys (with special characters) are left as literal dotted keys
  - Deep merge: When multiple dotted keys expand to the same path, their values are merged if both are objects
  - Conflict handling:
    - strict=true: Throws ex-info on conflicts (non-object collision)
    - strict=false: LWW (silent overwrite)

  Parameters:
    - value: Decoded value to expand
    - strict: Whether to throw errors on conflicts
    - expand-paths: Whether to expand paths (:off or :safe)

  Returns:
    Expanded value with dotted keys reconstructed as nested objects

  Throws:
    ex-info if conflicts occur in strict mode"
  [value strict expand-paths]
  (cond
    ;; Path expansion disabled
    (not= expand-paths :safe)
    value

    ;; Array - recursively expand elements
    (vector? value)
    (mapv #(paths % strict expand-paths) value)

    ;; Object - expand dotted keys
    (json-object? value)
    (let [expanded-object
          (reduce-kv
            (fn [acc key key-value]
              ;; Check if key contains dots and should be expanded
              (if (and (str/includes? key const/dot)
                       (let [segments (str/split key (re-pattern (str "\\" const/dot)))]
                         (every? utils/identifier-segment? segments)))
                ;; Expandable - split and insert
                (let [segments (str/split key (re-pattern (str "\\" const/dot)))
                      expanded-value (paths key-value strict expand-paths)]
                  (insert-path acc segments expanded-value strict))

                ;; Not expandable - keep as literal key, but still recursively expand the value
                (let [expanded-value (paths key-value strict expand-paths)]
                  ;; Check for conflicts with already-expanded keys
                  (if (contains? acc key)
                    (let [conflicting-value (get acc key)]
                      (if (can-merge? conflicting-value expanded-value)
                        (assoc acc key (merge-objects conflicting-value expanded-value strict))
                        (if strict
                          (throw (ex-info (str "Path expansion conflict at key \"" key "\": cannot merge "
                                               (type conflicting-value) " with " (type expanded-value))
                                          {:type :path-expansion-conflict
                                           :key key
                                           :conflicting-type (type conflicting-value)
                                           :expanded-type (type expanded-value)}))
                          ;; Non-strict: overwrite (LWW)
                          (assoc acc key expanded-value))))
                    ;; No conflict - insert directly
                    (assoc acc key expanded-value)))))
            {}
            value)]
      expanded-object)

    ;; Primitive - return as-is
    :else
    value))
