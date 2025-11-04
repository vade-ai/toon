(ns com.vadelabs.toon.decode.objects-test
  "Tests for object decoding functions."
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing]])
   [com.vadelabs.toon.decode.scanner :as scanner]
   [com.vadelabs.toon.decode.objects :as objects]
   [com.vadelabs.toon.decode.items :as items]))


;; ============================================================================
;; Simple Object Decoding Tests
;; ============================================================================

(deftest simple-object-test
  (testing "Decode simple flat object"
    (let [input "name: Alice\nage: 30"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"name" "Alice" "age" 30.0} result)))))

(deftest nested-object-test
  (testing "Decode nested object"
    (let [input "user:\n  name: Alice\n  age: 30"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"user" {"name" "Alice" "age" 30.0}} result)))))

(deftest empty-object-test
  (testing "Decode empty object value"
    (let [input "obj:"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"obj" nil} result)))))


;; ============================================================================
;; Object with Array Value Tests
;; ============================================================================

(deftest object-with-inline-array-test
  (testing "Decode object with inline array value"
    (let [input "tags[3]: a,b,c"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"tags" ["a" "b" "c"]} result)))))

(deftest object-with-nested-array-test
  (testing "Decode object with nested array"
    (let [input "items[2]:\n  - a\n  - b"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"items" ["a" "b"]} result)))))


;; ============================================================================
;; Complex Nested Structure Tests
;; ============================================================================

(deftest complex-nested-structure-test
  (testing "Decode complex nested structure"
    (let [input "user:\n  name: Alice\n  tags[2]: dev,clojure\n  profile:\n    age: 30"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"user" {"name" "Alice"
                      "tags" ["dev" "clojure"]
                      "profile" {"age" 30.0}}}
             result)))))

(deftest tabular-array-within-object-test
  (testing "Decode tabular array within object"
    (let [input "data:\n  items[2]{id,val}:\n    1,a\n    2,b"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"data" {"items" [{"id" 1.0 "val" "a"}
                                {"id" 2.0 "val" "b"}]}}
             result)))))

(deftest multi-level-nesting-test
  (testing "Decode multiple levels of nesting"
    (let [input "root:\n  level1:\n    level2:\n      value: 42"
          scan-result (scanner/to-parsed-lines input)
          cursor (scanner/cursor-from-scan-result scan-result)
          [result _] (objects/object cursor 0 "," true items/list-item)]
      (is (= {"root" {"level1" {"level2" {"value" 42.0}}}}
             result)))))
