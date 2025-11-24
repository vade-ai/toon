(ns com.vadelabs.toon.decode.stream
  "Streaming decoder for TOON format.

  Provides memory-efficient event-based decoding for large TOON documents.
  Instead of building complete value trees, emits parse events that can be
  consumed incrementally."
  (:require
    #?(:clj [clojure.core.async :as async]
       :cljs [cljs.core.async :as async])
    #?(:clj [clojure.core.async.impl.protocols :as async-proto]
       :cljs [cljs.core.async.impl.protocols :as async-proto])
    [clojure.string :as str]
    [com.vadelabs.toon.decode.event-builder :as event-builder]
    [com.vadelabs.toon.decode.scanner :as scanner])
  #?(:cljs
     (:require-macros
       [cljs.core.async.macros :refer [go]])))


;; ============================================================================
;; Synchronous Streaming (Lazy Sequence)
;; ============================================================================

(defn decode-stream-sync
  "Decode TOON lines into lazy sequence of parse events.

  Processes TOON content line-by-line, emitting parse events instead of
  building complete value trees. Memory-efficient for large documents.

  Parameters:
    - lines: Sequence of TOON lines (strings)
    - options: Optional map with keys:
      - :indent - Number of spaces per indentation level (default: 2)
      - :strict - Enable strict validation (default: true)

  Returns:
    Lazy sequence of events. Event types:
      - {:type :start-object}
      - {:type :end-object}
      - {:type :start-array}
      - {:type :end-array}
      - {:type :key :key \"field-name\"}
      - {:type :primitive :value <value>}

  Example:
    (decode-stream-sync [\"name: Alice\" \"age: 30\"])
    => ({:type :start-object}
        {:type :key :key \"name\"}
        {:type :primitive :value \"Alice\"}
        {:type :key :key \"age\"}
        {:type :primitive :value 30}
        {:type :end-object})

  Example with array:
    (decode-stream-sync [\"[3]: a,b,c\"])
    => ({:type :start-array}
        {:type :primitive :value \"a\"}
        {:type :primitive :value \"b\"}
        {:type :primitive :value \"c\"}
        {:type :end-array})"
  ([lines]
   (decode-stream-sync lines {}))
  ([lines options]
   (let [opts (merge {:indent 2 :strict true} options)
         input (if (string? lines) lines (str/join "\n" lines))
         cursor (-> input
                    (scanner/to-parsed-lines (:indent opts) (:strict opts))
                    scanner/cursor-from-scan-result)]
     (event-builder/events-from-cursor cursor (:indent opts) (:strict opts)))))


;; ============================================================================
;; Asynchronous Streaming (core.async)
;; ============================================================================

#?(:clj
   (defn decode-stream
     "Decode TOON lines asynchronously into core.async channel of events.

     Processes TOON content from either a string, lazy sequence, or async channel,
     emitting parse events to an output channel.

     Parameters:
       - source: Either:
         - String in TOON format
         - Sequence of strings (TOON lines)
         - core.async channel of strings
       - options: Optional map with keys:
         - :indent - Number of spaces per indentation level (default: 2)
         - :strict - Enable strict validation (default: true)
         - :buf-size - Channel buffer size (default: 32)

     Returns:
       core.async channel of events (same format as decode-stream-sync)

     Example:
       (let [lines [\"name: Alice\" \"age: 30\"]
             events-ch (decode-stream lines)]
         (async/<!! (async/into [] events-ch)))
       => [{:type :start-object}
           {:type :key :key \"name\"}
           {:type :primitive :value \"Alice\"}
           {:type :key :key \"age\"}
           {:type :primitive :value 30}
           {:type :end-object}]

     Example with async source:
       (let [source-ch (async/to-chan! [\"name: Alice\" \"age: 30\"])
             events-ch (decode-stream source-ch)]
         (async/<!! (async/into [] events-ch)))"
     ([source]
      (decode-stream source {}))
     ([source options]
      (let [opts (merge {:indent 2
                         :strict true
                         :buf-size 32}
                        options)
            out-ch (async/chan (:buf-size opts))]
        (async/go
          (try
            ;; Normalize input to sequence of lines
            (let [is-channel? (and (not (string? source))
                                   (not (sequential? source)))
                  lines (cond
                          ;; String input - split into lines
                          (string? source)
                          (str/split-lines source)

                          ;; Channel input - read all lines
                          is-channel?
                          (async/<! (async/into [] source))

                          ;; Already a sequence
                          :else
                          source)]
              ;; Generate events synchronously
              (doseq [event (decode-stream-sync lines opts)]
                (async/>! out-ch event)))
            (catch #?(:clj Exception :cljs js/Error) e
              (println "Error in decode-stream:" e)
              (throw e))
            (finally
              (async/close! out-ch))))
        out-ch))))


#?(:cljs
   (defn decode-stream
     "Decode TOON lines asynchronously into core.async channel of events.

     Processes TOON content from either a string, lazy sequence, or async channel,
     emitting parse events to an output channel.

     Parameters:
       - source: Either:
         - String in TOON format
         - Sequence of strings (TOON lines)
         - core.async channel of strings
       - options: Optional map with keys:
         - :indent - Number of spaces per indentation level (default: 2)
         - :strict - Enable strict validation (default: true)
         - :buf-size - Channel buffer size (default: 32)

     Returns:
       core.async channel of events (same format as decode-stream-sync)

     Example:
       (let [lines [\"name: Alice\" \"age: 30\"]
             events-ch (decode-stream lines)]
         (async/<! (async/into [] events-ch)))
       => [{:type :start-object}
           {:type :key :key \"name\"}
           {:type :primitive :value \"Alice\"}
           {:type :key :key \"age\"}
           {:type :primitive :value 30}
           {:type :end-object}]"
     ([source]
      (decode-stream source {}))
     ([source options]
      (let [opts (merge {:indent 2
                         :strict true
                         :buf-size 32}
                        options)
            out-ch (async/chan (:buf-size opts))]
        (go
          (try
            (let [is-channel? (and (not (string? source))
                                   (not (sequential? source)))
                  lines (cond
                          ;; String input - split into lines
                          (string? source)
                          (str/split-lines source)

                          ;; Channel input - read all lines
                          is-channel?
                          (async/<! (async/into [] source))

                          ;; Already a sequence
                          :else
                          source)]
              ;; Generate events synchronously
              (doseq [event (decode-stream-sync lines opts)]
                (async/>! out-ch event)))
            (catch js/Error e
              (js/console.error "Error in decode-stream:" e)
              (throw e))
            (finally
              (async/close! out-ch))))
        out-ch))))
