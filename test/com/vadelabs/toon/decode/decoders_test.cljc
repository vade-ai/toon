(ns com.vadelabs.toon.decode.decoders-test
  "Tests for root dispatcher functions."
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing]])
   [com.vadelabs.toon.decode.scanner :as scanner]
   [com.vadelabs.toon.decode.decoders :as decoders]))


;; ============================================================================
;; array-from-header Dispatcher Tests
;; ============================================================================

(deftest array-from-header-inline-test
  (testing "Dispatch to inline-primitive-array for inline values"
    (let [header-info {:length 3 :delimiter "," :inline-values "a,b,c"}
          cursor nil  ; Not needed for inline arrays
          [result _] (decoders/array-from-header header-info cursor 1)]
      (is (= ["a" "b" "c"] result)))))

(deftest array-from-header-tabular-test
  (testing "Dispatch to tabular-array for fields"
    (let [input "  1,Alice\n  2,Bob"  ; Indented at depth 1
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          header-info {:length 2 :delimiter "," :fields ["id" "name"]}
          [result _] (decoders/array-from-header header-info cursor 1)]
      (is (= 2 (count result)))
      (is (= {"id" 1.0 "name" "Alice"} (first result))))))

(deftest array-from-header-list-test
  (testing "Dispatch to list-array for neither inline nor tabular"
    (let [input "  - hello\n  - world"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          header-info {:length 2 :delimiter ","}
          [result _] (decoders/array-from-header header-info cursor 1)]
      (is (= ["hello" "world"] result)))))


;; ============================================================================
;; value-from-lines Dispatcher Tests
;; ============================================================================

(deftest value-from-lines-empty-input-test
  (testing "Empty input returns empty map"
    (let [scan-result (scanner/to-parsed-lines "")
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= {} result)))))

(deftest value-from-lines-primitive-test
  (testing "Single-line primitive without colon"
    (let [scan-result (scanner/to-parsed-lines "hello")
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= "hello" result)))))

(deftest value-from-lines-number-test
  (testing "Single-line number"
    (let [scan-result (scanner/to-parsed-lines "42")
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= 42.0 result)))))

(deftest value-from-lines-array-header-test
  (testing "Dispatch to array for root array header"
    (let [input "[3]: 1,2,3"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= [1.0 2.0 3.0] result)))))

(deftest value-from-lines-tabular-array-test
  (testing "Dispatch to tabular array at root"
    (let [input "[2]{id,name}:\n  1,Alice\n  2,Bob"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= [{"id" 1.0 "name" "Alice"}
              {"id" 2.0 "name" "Bob"}]
             result)))))

(deftest value-from-lines-object-test
  (testing "Dispatch to object for key-value lines"
    (let [input "name: Alice\nage: 30"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= {"name" "Alice" "age" 30.0} result)))))

(deftest value-from-lines-nested-object-test
  (testing "Dispatch to object for nested structure"
    (let [input "user:\n  name: Bob\n  age: 25"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= {"user" {"name" "Bob" "age" 25.0}} result)))))


;; ============================================================================
;; Integration: Dispatcher Behavior Tests
;; ============================================================================

(deftest dispatcher-complex-structure-test
  (testing "Root dispatcher handles complex nested structures"
    (let [input "config:\n  name: MyApp\n  features[2]:\n    - auth\n    - logging"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= {"config" {"name" "MyApp"
                        "features" ["auth" "logging"]}}
             result)))))

(deftest dispatcher-root-list-array-test
  (testing "Root dispatcher handles list array at top level"
    (let [input "[2]:\n  - item1\n  - item2"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          result (decoders/value-from-lines cursor 2 true)]
      (is (= ["item1" "item2"] result)))))
