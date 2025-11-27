(ns com.vadelabs.toon.decode.events
  "Event types and utilities for streaming TOON decode.

  Events are simple maps with a :type key. Prefer using maps directly:

    {:type :start-object}
    {:type :end-object}
    {:type :start-array :length 3}
    {:type :end-array}
    {:type :key :key \"field-name\"}
    {:type :key :key \"field-name\" :was-quoted true}
    {:type :primitive :value <value>}

  This enables memory-efficient processing of large TOON documents.")


;; ============================================================================
;; Event Predicates
;; ============================================================================

(defn start-object?
  "Returns true if event is start-object."
  [event]
  (= :start-object (:type event)))


(defn end-object?
  "Returns true if event is end-object."
  [event]
  (= :end-object (:type event)))


(defn start-array?
  "Returns true if event is start-array."
  [event]
  (= :start-array (:type event)))


(defn end-array?
  "Returns true if event is end-array."
  [event]
  (= :end-array (:type event)))


(defn key-event?
  "Returns true if event is a key event."
  [event]
  (= :key (:type event)))


(defn primitive?
  "Returns true if event is a primitive value."
  [event]
  (= :primitive (:type event)))


;; ============================================================================
;; Event Accessors
;; ============================================================================

(defn value
  "Extract value from primitive event."
  [event]
  (:value event))


(defn event-key
  "Extract key from key event."
  [event]
  (:key event))


(defn was-quoted
  "Returns true if key was quoted in original TOON source.
  Returns false/nil for non-key events or unquoted keys."
  [event]
  (:was-quoted event))


(defn length
  "Extract length from start-array event.
  Returns nil for non-array events."
  [event]
  (:length event))


;; ============================================================================
;; Event Constructors
;; These are kept for convenience but direct maps are preferred.
;; ============================================================================

(defn start-object
  "Create start-object event.
  Prefer: {:type :start-object}"
  []
  {:type :start-object})


(defn end-object
  "Create end-object event.
  Prefer: {:type :end-object}"
  []
  {:type :end-object})


(defn start-array
  "Create start-array event with length.
  Prefer: {:type :start-array :length n}"
  [length]
  {:type :start-array :length length})


(defn end-array
  "Create end-array event.
  Prefer: {:type :end-array}"
  []
  {:type :end-array})


(defn key-event
  "Create key event.
  Prefer: {:type :key :key k} or {:type :key :key k :was-quoted true}

  Note: :was-quoted is only included when true (matches TypeScript API)."
  ([k]
   {:type :key :key k})
  ([k was-quoted]
   (if was-quoted
     {:type :key :key k :was-quoted true}
     {:type :key :key k})))


(defn primitive
  "Create primitive event.
  Prefer: {:type :primitive :value v}"
  [value]
  {:type :primitive :value value})
