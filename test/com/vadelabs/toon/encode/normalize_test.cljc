(ns com.vadelabs.toon.encode.normalize-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing]])
            [com.vadelabs.toon.encode.normalize :as norm]))

;; ============================================================================
;; Primitive Normalization Tests
;; ============================================================================

(deftest nil-normalization-test
  (testing "nil remains nil"
    (is (nil? (norm/normalize-value nil)))))

(deftest boolean-normalization-test
  (testing "Booleans remain unchanged"
    (is (= true (norm/normalize-value true)))
    (is (= false (norm/normalize-value false)))))

(deftest string-normalization-test
  (testing "Strings remain unchanged"
    (is (= "hello" (norm/normalize-value "hello")))
    (is (= "" (norm/normalize-value "")))
    (is (= "hello world" (norm/normalize-value "hello world")))
    (is (= "🚀" (norm/normalize-value "🚀")))
    (is (= "café" (norm/normalize-value "café")))))

(deftest number-normalization-test
  (testing "Finite numbers remain unchanged"
    (is (= 42 (norm/normalize-value 42)))
    (is (= 3.14 (norm/normalize-value 3.14)))
    (is (= -7 (norm/normalize-value -7)))
    (is (= 0 (norm/normalize-value 0))))

  (testing "-0 normalizes to 0"
    (is (= 0 (norm/normalize-value -0.0))))

  (testing "NaN normalizes to nil"
    (is (nil? (norm/normalize-value ##NaN))))

  (testing "Infinity normalizes to nil"
    (is (nil? (norm/normalize-value ##Inf)))
    (is (nil? (norm/normalize-value ##-Inf)))))

;; ============================================================================
;; Clojure-Specific Type Normalization Tests
;; ============================================================================

(deftest keyword-normalization-test
  (testing "Keywords normalize to strings"
    (is (= "foo" (norm/normalize-value :foo)))
    (is (= "bar-baz" (norm/normalize-value :bar-baz))))

  (testing "Namespaced keywords preserve namespace"
    (is (= "user/id" (norm/normalize-value :user/id)))
    (is (= "app/config" (norm/normalize-value :app/config)))))

(deftest symbol-normalization-test
  (testing "Symbols normalize to strings"
    (is (= "foo" (norm/normalize-value 'foo)))
    (is (= "bar-baz" (norm/normalize-value 'bar-baz))))

  (testing "Namespaced symbols preserve namespace"
    (is (= "clojure.core/map" (norm/normalize-value 'clojure.core/map)))))

#?(:clj
   (deftest uuid-normalization-test
     (testing "UUIDs normalize to strings"
       (let [uuid (java.util.UUID/randomUUID)
             normalized (norm/normalize-value uuid)]
         (is (string? normalized))
         (is (= (str uuid) normalized))))))

#?(:clj
   (deftest date-normalization-test
     (testing "java.util.Date normalizes to Instant"
       (let [date (java.util.Date.)
             normalized (norm/normalize-value date)]
         (is (instance? java.time.Instant normalized))))

     (testing "java.time.Instant normalizes to string"
       (let [instant (java.time.Instant/now)
             normalized (norm/normalize-value instant)]
         (is (string? normalized))
         (is (.contains normalized "T"))
         (is (.contains normalized "Z"))))))

;; ============================================================================
;; Collection Normalization Tests
;; ============================================================================

(deftest set-normalization-test
  (testing "Sets normalize to sorted vectors"
    (is (= [1 2 3] (norm/normalize-value #{3 1 2})))
    (is (= ["a" "b" "c"] (norm/normalize-value #{"c" "a" "b"}))))

  (testing "Empty sets normalize to empty vectors"
    (is (= [] (norm/normalize-value #{})))))

(deftest vector-normalization-test
  (testing "Vectors with primitives"
    (is (= [1 2 3] (norm/normalize-value [1 2 3])))
    (is (= ["a" "b"] (norm/normalize-value ["a" "b"]))))

  (testing "Vectors with mixed types normalize recursively"
    (is (= [1 "foo" true] (norm/normalize-value [1 :foo true]))))

  (testing "Nested vectors"
    (is (= [[1 2] [3 4]] (norm/normalize-value [[1 2] [3 4]])))))

(deftest list-normalization-test
  (testing "Lists normalize to vectors"
    (is (= [1 2 3] (norm/normalize-value '(1 2 3))))
    (is (= ["a" "b"] (norm/normalize-value '("a" "b"))))))

(deftest map-normalization-test
  (testing "Maps with string keys remain unchanged"
    (is (= {"name" "Ada"} (norm/normalize-value {"name" "Ada"}))))

  (testing "Maps with keyword keys normalize to string keys"
    (is (= {"name" "Ada" "age" 30}
           (norm/normalize-value {:name "Ada" :age 30}))))

  (testing "Maps with nested structures normalize recursively"
    (is (= {"user" {"id" 1 "tags" ["admin" "user"]}}
           (norm/normalize-value {:user {:id 1 :tags [:admin :user]}}))))

  (testing "Empty maps remain empty"
    (is (= {} (norm/normalize-value {})))))

;; ============================================================================
;; Unsupported Type Normalization Tests
;; ============================================================================

(deftest function-normalization-test
  (testing "Functions normalize to nil"
    (is (nil? (norm/normalize-value (fn [] 42))))
    (is (nil? (norm/normalize-value +)))))

;; ============================================================================
;; Type Guard Tests
;; ============================================================================

(deftest primitive-guard-test
  (testing "Primitives"
    (is (norm/primitive? nil))
    (is (norm/primitive? true))
    (is (norm/primitive? false))
    (is (norm/primitive? 42))
    (is (norm/primitive? 3.14))
    (is (norm/primitive? "hello")))

  (testing "Non-primitives"
    (is (not (norm/primitive? [])))
    (is (not (norm/primitive? {})))
    (is (not (norm/primitive? :keyword)))))

(deftest json-array-guard-test
  (testing "Vectors are JSON arrays"
    (is (norm/json-array? []))
    (is (norm/json-array? [1 2 3]))
    (is (norm/json-array? ["a" "b"])))

  (testing "Non-vectors are not JSON arrays"
    (is (not (norm/json-array? {})))
    (is (not (norm/json-array? '(1 2 3))))
    (is (not (norm/json-array? #{1 2 3})))
    (is (not (norm/json-array? "hello")))))

(deftest json-object-guard-test
  (testing "Maps are JSON objects"
    (is (norm/json-object? {}))
    (is (norm/json-object? {"a" 1})))

  (testing "Non-maps are not JSON objects"
    (is (not (norm/json-object? [])))
    (is (not (norm/json-object? "hello")))
    (is (not (norm/json-object? 42)))))

(deftest array-of-primitives-guard-test
  (testing "Arrays of only primitives"
    (is (norm/array-of-primitives? []))
    (is (norm/array-of-primitives? [1 2 3]))
    (is (norm/array-of-primitives? ["a" "b" "c"]))
    (is (norm/array-of-primitives? [true false nil]))
    (is (norm/array-of-primitives? [1 "a" true nil])))

  (testing "Arrays with non-primitives"
    (is (not (norm/array-of-primitives? [[1 2]])))
    (is (not (norm/array-of-primitives? [{}])))
    (is (not (norm/array-of-primitives? [1 [2 3]])))
    (is (not (norm/array-of-primitives? {:a 1})))))

(deftest array-of-objects-guard-test
  (testing "Arrays of only objects"
    (is (norm/array-of-objects? [{}]))
    (is (norm/array-of-objects? [{"a" 1} {"b" 2}])))

  (testing "Empty arrays are not arrays of objects"
    (is (not (norm/array-of-objects? []))))

  (testing "Arrays with non-objects"
    (is (not (norm/array-of-objects? [1 2 3])))
    (is (not (norm/array-of-objects? [[1 2]])))
    (is (not (norm/array-of-objects? [{"a" 1} 2])))))

(deftest array-of-arrays-guard-test
  (testing "Arrays of only arrays"
    (is (norm/array-of-arrays? [[]]))
    (is (norm/array-of-arrays? [[1 2] [3 4]])))

  (testing "Empty arrays are not arrays of arrays"
    (is (not (norm/array-of-arrays? []))))

  (testing "Arrays with non-arrays"
    (is (not (norm/array-of-arrays? [1 2 3])))
    (is (not (norm/array-of-arrays? [{}])))
    (is (not (norm/array-of-arrays? [[1 2] 3])))))

;; ============================================================================
;; Complex Normalization Tests
;; ============================================================================

(deftest complex-nested-normalization-test
  (testing "Deeply nested structure with various Clojure types"
    (let [input {:user/profile {:name "Ada"
                                :tags #{:admin :user}
                                :scores [100 95 88]}
                 :settings {:notifications true
                            :theme :dark}}
          expected {"user/profile" {"name" "Ada"
                                    "tags" ["admin" "user"]
                                    "scores" [100 95 88]}
                    "settings" {"notifications" true
                                "theme" "dark"}}
          result (norm/normalize-value input)]
      (is (= expected result)))))

(deftest edge-case-normalization-test
  (testing "Mix of supported and unsupported types"
    (let [input {:valid 42
                 :fn (fn [] nil)
                 :keyword :foo}
          result (norm/normalize-value input)]
      (is (= {"valid" 42
              "fn" nil
              "keyword" "foo"}
             result)))))
