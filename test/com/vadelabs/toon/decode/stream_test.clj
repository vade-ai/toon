(ns com.vadelabs.toon.decode.stream-test
  "Streaming decode tests matching TypeScript reference implementation.

  Test categories:
  - Basic event streaming (decodeStreamSync)
  - Event reconstruction (buildValueFromEvents / events->value)
  - Streaming equivalence with regular decode
  - Strict mode validation
  - wasQuoted and length property parity"
  (:require
    [clojure.core.async :as async]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing are]]
    [com.vadelabs.toon.core :as toon]))


;; ============================================================================
;; Test Utilities
;; ============================================================================

(defn events->vec
  "Convert lazy sequence of events to vector for testing."
  [events]
  (vec events))


(defn async-events->vec
  "Convert async channel of events to vector for testing."
  [events-ch]
  (async/<!! (async/into [] events-ch)))


;; ============================================================================
;; decodeStreamSync Tests (matches TypeScript decodeStream.test.ts)
;; ============================================================================

(deftest test-decode-simple-object
  (testing "decode simple object"
    (let [input "name: Alice\nage: 30"
          events (events->vec (toon/events input))]
      (is (= [{:type :start-object}
              {:type :key :key "name"}
              {:type :primitive :value "Alice"}
              {:type :key :key "age"}
              {:type :primitive :value 30.0}
              {:type :end-object}]
             events)))))


(deftest test-decode-nested-object
  (testing "decode nested object"
    (let [input "user:\n  name: Alice\n  age: 30"
          events (events->vec (toon/events input))]
      (is (= [{:type :start-object}
              {:type :key :key "user"}
              {:type :start-object}
              {:type :key :key "name"}
              {:type :primitive :value "Alice"}
              {:type :key :key "age"}
              {:type :primitive :value 30.0}
              {:type :end-object}
              {:type :end-object}]
             events)))))


(deftest test-decode-inline-primitive-array
  (testing "decode inline primitive array"
    (let [input "scores[3]: 95, 87, 92"
          events (events->vec (toon/events input))]
      (is (= [{:type :start-object}
              {:type :key :key "scores"}
              {:type :start-array :length 3}
              {:type :primitive :value 95.0}
              {:type :primitive :value 87.0}
              {:type :primitive :value 92.0}
              {:type :end-array}
              {:type :end-object}]
             events)))))


(deftest test-decode-list-array
  (testing "decode list array"
    (let [input "items[2]:\n  - Apple\n  - Banana"
          events (events->vec (toon/events input))]
      (is (= [{:type :start-object}
              {:type :key :key "items"}
              {:type :start-array :length 2}
              {:type :primitive :value "Apple"}
              {:type :primitive :value "Banana"}
              {:type :end-array}
              {:type :end-object}]
             events)))))


(deftest test-decode-tabular-array
  (testing "decode tabular array"
    (let [input "users[2]{name,age}:\n  Alice, 30\n  Bob, 25"
          events (events->vec (toon/events input))]
      (is (= [{:type :start-object}
              {:type :key :key "users"}
              {:type :start-array :length 2}
              {:type :start-object}
              {:type :key :key "name"}
              {:type :primitive :value "Alice"}
              {:type :key :key "age"}
              {:type :primitive :value 30.0}
              {:type :end-object}
              {:type :start-object}
              {:type :key :key "name"}
              {:type :primitive :value "Bob"}
              {:type :key :key "age"}
              {:type :primitive :value 25.0}
              {:type :end-object}
              {:type :end-array}
              {:type :end-object}]
             events)))))


(deftest test-decode-root-primitive
  (testing "decode root primitive"
    (let [input "Hello World"
          events (events->vec (toon/events input))]
      (is (= [{:type :primitive :value "Hello World"}]
             events)))))


(deftest test-decode-root-array
  (testing "decode root array"
    (let [input "[2]:\n  - Apple\n  - Banana"
          events (events->vec (toon/events input))]
      (is (= [{:type :start-array :length 2}
              {:type :primitive :value "Apple"}
              {:type :primitive :value "Banana"}
              {:type :end-array}]
             events)))))


(deftest test-decode-empty-input-as-empty-object
  (testing "decode empty input as empty object"
    (let [events (events->vec (toon/events ""))]
      (is (= [{:type :start-object}
              {:type :end-object}]
             events)))))


(deftest test-enforce-strict-mode-validation
  (testing "enforce strict mode validation"
    (is (thrown? clojure.lang.ExceptionInfo
                 (doall (toon/events "items[2]:\n  - Apple" {:strict true}))))))


(deftest test-allow-count-mismatch-non-strict
  (testing "allow count mismatch in non-strict mode"
    (is (= [{:type :start-object}
            {:type :key :key "items"}
            {:type :start-array :length 2}
            {:type :primitive :value "Apple"}
            {:type :end-array}
            {:type :end-object}]
           (events->vec (toon/events "items[2]:\n  - Apple" {:strict false}))))))


;; ============================================================================
;; events->value Tests (matches TypeScript buildValueFromEvents)
;; ============================================================================

(deftest test-build-object-from-events
  (testing "build object from events"
    (let [events [{:type :start-object}
                  {:type :key :key "name"}
                  {:type :primitive :value "Alice"}
                  {:type :key :key "age"}
                  {:type :primitive :value 30}
                  {:type :end-object}]
          result (toon/events->value events)]
      (is (= {"name" "Alice" "age" 30} result)))))


(deftest test-build-nested-object-from-events
  (testing "build nested object from events"
    (let [events [{:type :start-object}
                  {:type :key :key "user"}
                  {:type :start-object}
                  {:type :key :key "name"}
                  {:type :primitive :value "Alice"}
                  {:type :end-object}
                  {:type :end-object}]
          result (toon/events->value events)]
      (is (= {"user" {"name" "Alice"}} result)))))


(deftest test-build-array-from-events
  (testing "build array from events"
    (let [events [{:type :start-array :length 3}
                  {:type :primitive :value 1}
                  {:type :primitive :value 2}
                  {:type :primitive :value 3}
                  {:type :end-array}]
          result (toon/events->value events)]
      (is (= [1 2 3] result)))))


(deftest test-build-primitive-from-events
  (testing "build primitive from events"
    (let [events [{:type :primitive :value "Hello"}]
          result (toon/events->value events)]
      (is (= "Hello" result)))))


(deftest test-throw-on-incomplete-event-stream
  (testing "throw on incomplete event stream"
    (let [events [{:type :start-object}
                  {:type :key :key "name"}
                  ;; Missing primitive and endObject
                  ]]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"[Ii]ncomplete"
                            (toon/events->value events))))))


;; ============================================================================
;; lines->value / decodeFromLines Tests
;; ============================================================================

(deftest test-lines->value-produces-same-result-as-decode
  (testing "produce same result as decode"
    (let [input "name: Alice\nage: 30\nscores[3]: 95, 87, 92"
          lines (str/split-lines input)
          from-lines (toon/lines->value lines)
          from-string (toon/decode input)]
      (is (= from-string from-lines)))))


(deftest test-lines->value-supports-expand-paths
  (testing "support expandPaths option"
    (let [lines ["user.name: Alice" "user.age: 30"]
          result (toon/lines->value lines {:expand-paths :safe})]
      (is (= {"user" {"name" "Alice" "age" 30.0}} result)))))


(deftest test-lines->value-complex-nested
  (testing "handle complex nested structures"
    (let [input "users[2]:\n  - name: Alice\n    scores[3]: 95, 87, 92\n  - name: Bob\n    scores[3]: 88, 91, 85"
          lines (str/split-lines input)
          from-lines (toon/lines->value lines)
          from-string (toon/decode input)]
      (is (= from-string from-lines))
      (is (= {"users" [{"name" "Alice" "scores" [95.0 87.0 92.0]}
                       {"name" "Bob" "scores" [88.0 91.0 85.0]}]}
             from-lines)))))


(deftest test-lines->value-tabular-arrays
  (testing "handle tabular arrays"
    (let [input "users[3]{name,age,city}:\n  Alice, 30, NYC\n  Bob, 25, LA\n  Charlie, 35, SF"
          lines (str/split-lines input)
          from-lines (toon/lines->value lines)
          from-string (toon/decode input)]
      (is (= from-string from-lines))
      (is (= {"users" [{"name" "Alice" "age" 30.0 "city" "NYC"}
                       {"name" "Bob" "age" 25.0 "city" "LA"}
                       {"name" "Charlie" "age" 35.0 "city" "SF"}]}
             from-lines)))))


;; ============================================================================
;; Streaming Equivalence Tests (data-driven)
;; ============================================================================

(deftest test-streaming-equivalence
  (testing "streaming decode matches regular decode"
    (are [desc input]
        (= (toon/decode input) (toon/events->value (toon/events input)))

      "simple object"
      "name: Alice\nage: 30"

      "nested objects"
      "user:\n  profile:\n    name: Alice\n    age: 30"

      "mixed structures"
      "name: Alice\nscores[3]: 95, 87, 92\naddress:\n  city: NYC\n  zip: 10001"

      "list array with objects"
      "users[2]:\n  - name: Alice\n    age: 30\n  - name: Bob\n    age: 25"

      "root primitive number"
      "42"

      "root primitive string"
      "Hello World"

      "root primitive boolean true"
      "true"

      "root primitive boolean false"
      "false"

      "root primitive null"
      "null"

      "empty object"
      ""

      "root array"
      "[2]:\n  - Apple\n  - Banana"

      "tabular array"
      "[2]{id,name}:\n  1,Alice\n  2,Bob")))


;; ============================================================================
;; Key wasQuoted Property Tests (Feature Parity)
;; ============================================================================

(deftest test-unquoted-key-wasquoted
  (testing "Unquoted key has was-quoted nil (not present)"
    (is (= [{:type :start-object}
            {:type :key :key "name"}  ;; No :was-quoted key for unquoted
            {:type :primitive :value "Alice"}
            {:type :end-object}]
           (events->vec (toon/events "name: Alice"))))))


(deftest test-quoted-key-wasquoted
  (testing "Quoted key has was-quoted true"
    (is (= [{:type :start-object}
            {:type :key :key "user.name" :was-quoted true}
            {:type :primitive :value "Alice"}
            {:type :end-object}]
           (events->vec (toon/events "\"user.name\": Alice"))))))


(deftest test-mixed-quoted-unquoted-keys
  (testing "Object with mix of quoted and unquoted keys"
    (is (= [{:type :start-object}
            {:type :key :key "name"}
            {:type :primitive :value "Alice"}
            {:type :key :key "user.id" :was-quoted true}
            {:type :primitive :value 123.0}
            {:type :key :key "age"}
            {:type :primitive :value 30.0}
            {:type :end-object}]
           (events->vec (toon/events "name: Alice\n\"user.id\": 123\nage: 30"))))))


;; ============================================================================
;; Array Length Property Tests (Feature Parity)
;; ============================================================================

(deftest test-array-length-property
  (testing "start-array events include length property"
    (are [input expected]
        (= expected (events->vec (toon/events input)))

      "[3]: a,b,c"
      [{:type :start-array :length 3}
       {:type :primitive :value "a"}
       {:type :primitive :value "b"}
       {:type :primitive :value "c"}
       {:type :end-array}]

      "[0]"
      [{:type :start-array :length 0}
       {:type :end-array}]

      "tags[2]: a,b"
      [{:type :start-object}
       {:type :key :key "tags"}
       {:type :start-array :length 2}
       {:type :primitive :value "a"}
       {:type :primitive :value "b"}
       {:type :end-array}
       {:type :end-object}]

      "[2]{id,name}:\n  1,Alice\n  2,Bob"
      [{:type :start-array :length 2}
       {:type :start-object}
       {:type :key :key "id"}
       {:type :primitive :value 1.0}
       {:type :key :key "name"}
       {:type :primitive :value "Alice"}
       {:type :end-object}
       {:type :start-object}
       {:type :key :key "id"}
       {:type :primitive :value 2.0}
       {:type :key :key "name"}
       {:type :primitive :value "Bob"}
       {:type :end-object}
       {:type :end-array}])))


;; ============================================================================
;; Async Stream Tests
;; ============================================================================

(deftest test-async-simple-object
  (testing "Async decode of simple object"
    (is (= [{:type :start-object}
            {:type :key :key "name"}
            {:type :primitive :value "Alice"}
            {:type :key :key "age"}
            {:type :primitive :value 30.0}
            {:type :end-object}]
           (async-events->vec (toon/events-ch "name: Alice\nage: 30"))))))


(deftest test-async-from-channel
  (testing "Async decode from channel source"
    (is (= [{:type :start-object}
            {:type :key :key "name"}
            {:type :primitive :value "Alice"}
            {:type :key :key "age"}
            {:type :primitive :value 30.0}
            {:type :end-object}]
           (async-events->vec (toon/events-ch (async/to-chan! ["name: Alice" "age: 30"])))))))


(deftest test-async-array
  (testing "Async decode of array"
    (is (= [{:type :start-array :length 3}
            {:type :primitive :value "a"}
            {:type :primitive :value "b"}
            {:type :primitive :value "c"}
            {:type :end-array}]
           (async-events->vec (toon/events-ch "[3]: a,b,c"))))))


;; ============================================================================
;; Lazy Evaluation Test
;; ============================================================================

(deftest test-lazy-evaluation
  (testing "Events are lazily evaluated"
    ;; Take only first 3 events - rest should not be evaluated
    (is (= [{:type :start-object}
            {:type :key :key "name"}
            {:type :primitive :value "Alice"}]
           (vec (take 3 (toon/events "name: Alice\nage: 30\ncity: NYC")))))))
