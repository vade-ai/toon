(ns com.vadelabs.toon.core-test
  (:require
    #?(:clj [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [deftest is testing]])
    [com.vadelabs.toon.constants :as const]
    [com.vadelabs.toon.core :as toon]))


;; ============================================================================
;; Constants Tests
;; ============================================================================

(deftest constants-test
  (testing "List markers"
    (is (= "-" const/list-item-marker))
    (is (= "- " const/list-item-prefix)))

  (testing "Delimiters"
    (is (= "," (:comma const/delimiters)))
    (is (= "\t" (:tab const/delimiters)))
    (is (= "|" (:pipe const/delimiters)))
    (is (= "," const/default-delimiter)))

  (testing "Literals"
    (is (= "null" const/null-literal))
    (is (= "true" const/true-literal))
    (is (= "false" const/false-literal)))

  (testing "Default options"
    (is (= 2 const/default-indent))))


;; ============================================================================
;; Encode Function Tests (Basic)
;; ============================================================================

(deftest encode-stub-test
  (testing "encode function exists and returns string"
    (is (string? (toon/encode {})))
    (is (string? (toon/encode [] {:indent 2})))
    (is (string? (toon/encode nil {:delimiter ","})))))


(deftest decode-nested-objects-test
  (testing "Decode nested objects"
    (is (= {"user" {"name" "Ada"}}
           (toon/decode "user:\n  name: Ada")))
    (is (= {"user" {"name" "Ada" "age" 30.0}}
           (toon/decode "user:\n  name: Ada\n  age: 30")))
    (is (= {"outer" {"inner" {"value" 42.0}}}
           (toon/decode "outer:\n  inner:\n    value: 42")))))


(deftest decode-inline-arrays-test
  (testing "Decode inline arrays"
    (is (= [1.0 2.0 3.0] (toon/decode "[3]: 1,2,3")))
    (is (= ["a" "b" "c"] (toon/decode "[3]: a,b,c")))
    (is (= [true false] (toon/decode "[2]: true,false")))
    (is (= [] (toon/decode "[0]:")))))


(deftest decode-tabular-arrays-test
  (testing "Decode tabular arrays"
    (is (= [{"id" 1.0 "name" "Alice"}
            {"id" 2.0 "name" "Bob"}]
           (toon/decode "[2]{id,name}:\n  1,Alice\n  2,Bob")))
    (is (= [{"a" 1.0 "b" 2.0 "c" 3.0}]
           (toon/decode "[1]{a,b,c}:\n  1,2,3")))))


(deftest decode-list-arrays-test
  (testing "Decode list arrays with primitives"
    (is (= ["hello" 42.0 true]
           (toon/decode "[3]:\n  - hello\n  - 42\n  - true"))))

  (testing "Decode list arrays with objects"
    (is (= [{"id" 1.0 "name" "Alice"}
            {"id" 2.0 "name" "Bob"}]
           (toon/decode "[2]:\n  - id: 1\n    name: Alice\n  - id: 2\n    name: Bob")))))


(deftest decode-objects-with-arrays-test
  (testing "Decode objects with inline arrays"
    (is (= {"tags" ["dev" "clojure"]}
           (toon/decode "tags[2]: dev,clojure"))))

  (testing "Decode objects with nested arrays"
    (is (= {"items" ["a" "b"]}
           (toon/decode "items[2]:\n  - a\n  - b")))))


(deftest decode-complex-structures-test
  (testing "Decode complex nested structure"
    (is (= {"user" {"name" "Alice"
                    "tags" ["dev" "clojure"]
                    "profile" {"age" 30.0}}}
           (toon/decode "user:\n  name: Alice\n  tags[2]: dev,clojure\n  profile:\n    age: 30")))))


(deftest decode-empty-input-test
  (testing "Decode empty string returns empty map"
    (is (= {} (toon/decode "")))))


;; ============================================================================
;; Round-trip Tests (Encode + Decode)
;; ============================================================================

(deftest roundtrip-primitives-test
  (testing "Round-trip primitives"
    (is (nil? (toon/decode (toon/encode nil))))
    (is (true? (toon/decode (toon/encode true))))
    (is (false? (toon/decode (toon/encode false))))
    (is (= 42.0 (toon/decode (toon/encode 42))))
    (is (= "hello" (toon/decode (toon/encode "hello"))))))


(deftest roundtrip-flat-objects-test
  (testing "Round-trip flat objects"
    (is (= {"name" "Ada"}
           (toon/decode (toon/encode {"name" "Ada"}))))
    (is (= {"name" "Ada" "age" 30.0}
           (toon/decode (toon/encode {"name" "Ada" "age" 30}))))))


(deftest roundtrip-nested-objects-test
  (testing "Round-trip nested objects"
    (is (= {"user" {"name" "Ada" "age" 30.0}}
           (toon/decode (toon/encode {"user" {"name" "Ada" "age" 30}}))))))


(deftest roundtrip-arrays-test
  (testing "Round-trip arrays in objects (inline)"
    (is (= {"items" [1.0 2.0 3.0]}
           (toon/decode (toon/encode {"items" [1 2 3]})))))

  (testing "Round-trip tabular arrays"
    (is (= [{"id" 1.0 "name" "Alice"}
            {"id" 2.0 "name" "Bob"}]
           (toon/decode (toon/encode [{"id" 1 "name" "Alice"}
                                      {"id" 2 "name" "Bob"}]))))))


(deftest roundtrip-complex-structures-test
  (testing "Round-trip complex nested structure"
    (let [data {"user" {"name" "Alice"
                        "tags" ["dev" "clojure"]
                        "profile" {"age" 30
                                   "active" true}}}
          encoded (toon/encode data)
          decoded (toon/decode encoded)]
      (is (= {"user" {"name" "Alice"
                      "tags" ["dev" "clojure"]
                      "profile" {"age" 30.0
                                 "active" true}}}
             decoded)))))


;; ============================================================================
;; lines->value Function Tests
;; ============================================================================

(deftest lines->value-simple-object-test
  (testing "Decode simple object from lines"
    (is (= {"name" "Alice" "age" 30.0}
           (toon/lines->value ["name: Alice" "age: 30"])))))


(deftest lines->value-nested-object-test
  (testing "Decode nested object from lines"
    (is (= {"user" {"name" "Alice" "age" 30.0}}
           (toon/lines->value ["user:" "  name: Alice" "  age: 30"])))))


(deftest lines->value-inline-array-test
  (testing "Decode inline array from lines"
    (is (= [1.0 2.0 3.0]
           (toon/lines->value ["[3]: 1,2,3"])))))


(deftest lines->value-object-with-array-test
  (testing "Decode object with array field from lines"
    (is (= {"tags" ["dev" "clojure"]}
           (toon/lines->value ["tags[2]: dev,clojure"])))))


(deftest lines->value-path-expansion-test
  (testing "Decode with path expansion disabled (default)"
    (is (= {"user.name" "Alice" "user.age" 30.0}
           (toon/lines->value ["user.name: Alice" "user.age: 30"]))))

  (testing "Decode with path expansion enabled"
    (is (= {"user" {"name" "Alice" "age" 30.0}}
           (toon/lines->value ["user.name: Alice" "user.age: 30"]
                             {:expand-paths :safe})))))


(deftest lines->value-equivalence-with-decode-test
  (testing "lines->value produces same result as decode"
    (let [toon-str "name: Alice\nage: 30\ntags[2]: dev,clojure"
          lines ["name: Alice" "age: 30" "tags[2]: dev,clojure"]
          from-decode (toon/decode toon-str)
          from-lines (toon/lines->value lines)]
      (is (= from-decode from-lines)))))


(deftest lines->value-empty-lines-test
  (testing "Empty lines sequence returns empty map"
    (is (= {} (toon/lines->value [])))))


(deftest lines->value-single-primitive-test
  (testing "Single primitive value from lines"
    (is (= 42.0 (toon/lines->value ["42"])))
    (is (= "hello" (toon/lines->value ["hello"])))
    (is (= true (toon/lines->value ["true"])))
    (is (nil? (toon/lines->value ["null"])))))
